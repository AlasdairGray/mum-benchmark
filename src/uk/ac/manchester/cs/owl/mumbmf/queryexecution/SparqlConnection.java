package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SparqlConnection implements ServerConnection {
    private String sparqlEndpoint;
    private String updateSparqlEndpoint;
    private String defaultGraph;
    private static Logger logger = Logger.getLogger(SparqlConnection.class);
    private int timeout;

    /**
     * @param sparqlEndpoint sparql endpoint
     * @param defaultGraph   the default graph
     * @param timeout        query timeout
     */
    public SparqlConnection(String sparqlEndpoint, String defaultGraph, int timeout) {
        this.sparqlEndpoint = sparqlEndpoint;
        this.defaultGraph = defaultGraph;
        this.timeout = timeout;
    }

    /**
     * @param sparqlEndpoint       sparql endpoint
     * @param updateSparqlEndpoint sparql UPDATE endpoint
     * @param defaultGraph         default graph
     * @param timeout              query timeout
     */
    public SparqlConnection(String sparqlEndpoint, String updateSparqlEndpoint, String defaultGraph, int timeout) {
        this.updateSparqlEndpoint = updateSparqlEndpoint;
        this.sparqlEndpoint = sparqlEndpoint;
        this.defaultGraph = defaultGraph;
        this.timeout = timeout;
    }

    /*
      * Execute Query with Query Object
      */
    public void executeQuery(Query query, byte queryType) {
        executeQuery(query.getQueryString(), queryType, query.getNr(), query.getQueryMix());
    }


    /*
      * execute Query with Query String
      */
    private void executeQuery(String queryString, byte queryType, int queryNr, QueryMix queryMix) {
        double timeInSeconds;

        SparqlQuery qe;
        if (queryType == Query.UPDATE_TYPE)
            qe = new SparqlQuery(updateSparqlEndpoint, queryString, queryType, defaultGraph, timeout);
        else
            qe = new SparqlQuery(sparqlEndpoint, queryString, queryType, defaultGraph, timeout);
        int queryMixRun = queryMix.getRun() + 1;

        long start = System.nanoTime();

        InputStream is = qe.exec();


        long stop = System.nanoTime();
        Long interval = stop - start;
        timeInSeconds = interval.doubleValue() / 1000000000;

        if (is == null) {
            double t = this.timeout / 1000.0;
            System.out.println("Query " + queryNr + ": " + t + " seconds timeout!");
            queryMix.reportTimeOut();//inc. timeout counter
            queryMix.setCurrent(0, t);
            qe.close();
            return;
        }
        int resultCount = 0;
        //Write XML result into result
        try {
            if (queryType == Query.SELECT_TYPE) {
                resultCount = countResults(is);
            } else {
                resultCount = countBytes(is);
            }
        } catch (SocketTimeoutException e) {
            double t = this.timeout / 1000.0;
            System.out.println("Query " + queryNr + ": " + t + " seconds timeout!");
            queryMix.reportTimeOut();//inc. timeout counter
            queryMix.setCurrent(0, t);
            qe.close();
            return;
        }
//        timeInSeconds = qe.getExecutionTimeInSeconds();

        if (logger.isEnabledFor(Level.ALL) && queryMixRun > 0)
            logResultInfo(queryNr, queryMixRun, timeInSeconds,
                    queryString, queryType,
                    resultCount);

        queryMix.setCurrent(resultCount, timeInSeconds);
        qe.close();
    }


    private int countBytes(InputStream is) {
        int nrBytes = 0;
        byte[] buf = new byte[10000];
        int len = 0;
//	StringBuffer sb = new StringBuffer(1000);
        try {
            while ((len = is.read(buf)) != -1) {
                nrBytes += len;//resultCount counts the returned bytes
//			String temp = new String(buf,0,len);
//			temp = "\n\n" + temp + "\n\n";
//			logger.log(Level.ALL, temp);
//			sb.append(temp);
            }
        } catch (IOException e) {
            System.err.println("Could not read result from input stream");
        }
//	System.out.println(sb.toString());
        return nrBytes;
    }

    private void logResultInfo(int queryNr, int queryMixRun, double timeInSeconds,
                               String queryString, byte queryType,
                               int resultCount) {
        StringBuffer sb = new StringBuffer(1000);
        sb.append("\n\n\tQuery " + queryNr + " of run " + queryMixRun + " has been executed ");
        sb.append("in " + String.format("%.6f", timeInSeconds) + " seconds.\n");
        sb.append("\n\tQuery string:\n\n");
        sb.append(queryString);
        sb.append("\n\n");

        //Log results
        if (queryType == Query.DESCRIBE_TYPE)
            sb.append("\tQuery(Describe) result (" + resultCount + " Bytes): \n\n");
        else if (queryType == Query.CONSTRUCT_TYPE)
            sb.append("\tQuery(Construct) result (" + resultCount + " Bytes): \n\n");
        else
            sb.append("\tQuery results (" + resultCount + " results): \n\n");


        sb.append("\n__________________________________________________________________________________\n");
        logger.log(Level.ALL, sb.toString());
    }

    private int countResults(InputStream s) throws SocketTimeoutException {
        ResultHandler handler = new ResultHandler();
        int count = 0;
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
//		  ByteArrayInputStream bis = new ByteArrayInputStream(s.getBytes("UTF-8"));
            saxParser.parse(s, handler);
            count = handler.getCount();
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException();
        } catch (Exception e) {
            System.err.println("SAX Error");
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
        return count;
    }

    private static class ResultHandler extends DefaultHandler {
        private int count;

        ResultHandler() {
            count = 0;
        }

        @Override
        public void startElement(String namespaceURI,
                                 String localName,   // local name
                                 String qName,       // qualified name
                                 Attributes attrs) {
            if (qName.equals("result"))
                count++;
        }

        public int getCount() {
            return count;
        }
    }

    public void close() {
        //nothing to close
    }

//    /*
//      * (non-Javadoc)
//      * @see benchmark.testdriver.ServerConnection#executeValidation(benchmark.testdriver.Query, byte, java.lang.String[])
//      * Gather information about the result a query returns.
//      */
//    public QueryResult executeValidation(Query query, byte queryType) {
//        String queryString = query.getQueryString();
//        int queryNr = query.getNr();
//        String[] rowNames = query.getRowNames();
//        boolean sorted = queryString.toLowerCase().contains("order by");
//        QueryResult queryResult = null;
//
//        FishNetQuery qe;
//        if (queryType == Query.UPDATE_TYPE)
//            qe = new FishNetQuery(updateServiceURL, queryString, queryType, defaultGraph, 0);
//        else
//            qe = new FishNetQuery(serviceURL, queryString, queryType, defaultGraph, 0);
//
//        InputStream is = qe.exec();
//
//        if (queryType != Query.UPDATE_TYPE) {
//            Document doc = getXMLDocument(is);
//            XMLOutputter outputter = new XMLOutputter();
//            logResultInfo(query, outputter.outputString(doc));
//
//            if (queryType == Query.SELECT_TYPE)
//                queryResult = gatherResultInfoForSelectQuery(queryString, queryNr, sorted, doc, rowNames);
//        } else
//            logResultInfo(query, "");
//
//        if (queryResult != null)
//            queryResult.setRun(query.getQueryMix().getRun());
//        return queryResult;
//    }

    private void logResultInfo(Query query, String queryResult) {
        StringBuffer sb = new StringBuffer();

        sb.append("\n\n\tQuery " + query.getNr() + " of run " + (query.getQueryMix().getQueryMixRuns() + 1) + ":\n");
        sb.append("\n\tQuery string:\n\n");
        sb.append(query.getQueryString());
        sb.append("\n\n\tResult:\n\n");
        sb.append(queryResult);
        sb.append("\n\n__________________________________________________________________________________\n");
        logger.log(Level.ALL, sb.toString());
    }

    /**
     * @param is
     * @return
     */
    public Document getXMLDocument(InputStream is) {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setIgnoringElementContentWhitespace(true);
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return doc;
    }

    private QueryResult gatherResultInfoForSelectQuery(String queryString, int queryNr, boolean sorted, Document doc, String[] rows) {
        Element root = doc.getRootElement();

        //Get head information
        Element child = root.getChild("head", Namespace.getNamespace("http://www.w3.org/2005/sparql-results#"));

        //Get result rows (<head>)
        @SuppressWarnings("unchecked")
        List<Element> headChildren = child.getChildren("variable", Namespace.getNamespace("http://www.w3.org/2005/sparql-results#"));

        Iterator<Element> it = headChildren.iterator();
        ArrayList<String> headList = new ArrayList<String>();
        while (it.hasNext()) {
            headList.add((it.next()).getAttributeValue("name"));
        }

        @SuppressWarnings("unchecked")
        List<Element> resultChildren = root.getChild("results", Namespace.getNamespace("http://www.w3.org/2005/sparql-results#"))
                .getChildren("result", Namespace.getNamespace("http://www.w3.org/2005/sparql-results#"));
        int nrResults = resultChildren.size();

        QueryResult queryResult = new QueryResult(queryNr, queryString, nrResults, sorted, headList);

        it = resultChildren.iterator();
        while (it.hasNext()) {
            Element resultElement = it.next();
            StringBuilder result = new StringBuilder();

            //get the row values and paste it together to one String
            for (int i = 0; i < rows.length; i++) {
                @SuppressWarnings("unchecked")
                List<Element> bindings = resultElement.getChildren("binding", Namespace.getNamespace("http://www.w3.org/2005/sparql-results#"));
                String rowName = rows[i];
                for (int j = 0; j < bindings.size(); j++) {
                    Element binding = bindings.get(j);
                    if (binding.getAttributeValue("name").equals(rowName))
                        if (result.length() == 0)
                            result.append(rowName + ": " + ((Element) binding.getChildren().get(0)).getTextNormalize());
                        else
                            result.append("\n" + rowName + ": " + ((Element) binding.getChildren().get(0)).getTextNormalize());
                }
            }

            queryResult.addResult(result.toString());
        }
        return queryResult;
    }

    /**
     * @param queryString
     * @return
     */
    public Document executeSimpleQuery(String queryString) {
        SparqlQuery qe = new SparqlQuery(sparqlEndpoint, queryString, Query.SELECT_TYPE, defaultGraph, timeout);
        InputStream is = qe.exec();
        Document doc = getXMLDocument(is);
        try {
            qe.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;

    }
}
