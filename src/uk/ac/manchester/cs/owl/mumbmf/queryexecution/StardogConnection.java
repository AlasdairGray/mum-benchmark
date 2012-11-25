package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

import com.clarkparsia.stardog.StardogException;
import com.clarkparsia.stardog.api.Connection;
import com.clarkparsia.stardog.api.ConnectionConfiguration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by
 * User: Samantha Bail
 * Date: 24/11/2012
 * Time: 22:35
 * The University of Manchester
 */


public class StardogConnection implements ServerConnection {

    private StardogConnectionParameters connection;
    private static Logger logger = Logger.getLogger(SparqlConnection.class);
    private int timeout;
    private Connection aConn = null;

    /**
     * @param timeout query timeout
     */
    public StardogConnection(StardogConnectionParameters params, int timeout) {
        this.timeout = timeout;
        this.connection = params;
        try {
            this.aConn = ConnectionConfiguration
                        .to(connection.dbName)            // the name of the db to connect to
                        .credentials(connection.login, connection.password)              // credentials to use while connecting
                        .url(connection.dbUrl)
                        .connect();
        } catch (StardogException e) {
            e.printStackTrace();
        }
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

        Connection aConn = null;
        com.clarkparsia.stardog.api.Query qe = null;
        TupleQueryResult aResult = null;
//        System.out.println("Executing query " + queryNr);

        long start = 0, stop = 0;
        try {
//            aConn = ConnectionConfiguration
//                    .to(connection.dbName)            // the name of the db to connect to
//                    .credentials(connection.login, connection.password)              // credentials to use while connecting
//                    .url(connection.dbUrl)
//                    .connect();
            qe = aConn.query(queryString);
            start = System.nanoTime();
            aResult = qe.executeSelect();
            stop = System.nanoTime();

        } catch (StardogException e) {
            e.printStackTrace();
        }

        int queryMixRun = queryMix.getRun() + 1;

        int resultCount = 0;
        try {
            resultCount = countResults(aResult);

        } catch (SocketTimeoutException e) {
            double t = this.timeout / 1000.0;
            System.out.println("Query " + queryNr + ": " + t + " seconds timeout!");
            queryMix.reportTimeOut();
            queryMix.setCurrent(0, t);
            try {
                aResult.close();
            } catch (QueryEvaluationException e1) {
                e1.printStackTrace();
            }
            return;
        }
        double diff = (double) stop - (double) start;
        timeInSeconds = diff / 1000000000;

        queryMix.setCurrent(resultCount, timeInSeconds);

    }


    private int countResults(TupleQueryResult aResult) throws SocketTimeoutException {
        int count = 0;
        try {
            while (aResult.hasNext()) {
                aResult.next();
                count++;
            }
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
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
//        TODO: implement
        return null;

    }
}
