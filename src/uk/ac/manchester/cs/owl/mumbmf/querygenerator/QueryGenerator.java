package uk.ac.manchester.cs.owl.mumbmf.querygenerator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.manchester.cs.owl.mumbmf.util.Util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/07/2012
 * Time: 16:57
 * The University of Manchester
 */


public abstract class QueryGenerator {

    protected String currentQueryName = "";
    protected int queryCount = 1;
    protected String outputDir = "";


    /**
     * Takes an XML file containing parameterised queries + parameter queries
     * and substitutes parameters for values, then saves the resulting queries
     * @param queryFileName the XML query file
     * @param queryMixFile
     * @param ignoreFile
     */
    public void generateQueries(String queryFileName, String queryMixFile, String ignoreFile) {
        File queryFile = new File(queryFileName);
        try {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(queryFile);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("/queries/query");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            // for each query, generate a map of parameter names and values
//            then replace the parameters in the final query with the respective (randomly selected) values
            for (int i = 0; i < nodes.getLength(); i++) {
                queryCount = i + 1;
                String completeQuery = generateCompleteQuery(nodes.item(i), doc);
                saveCompleteQuery(completeQuery);
            }
            saveHelperFiles(queryMixFile, ignoreFile);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void saveHelperFiles(String queryMixFile, String ignoreFile) {

        if (!queryMixFile.equals("")) {
            File f = new File(outputDir + "/querymix.txt");
            Util.copyFile(new File(queryMixFile), f);
        }
        if (!ignoreFile.equals("")) {
            File f = new File(outputDir + "/ignoreQueries.txt");
            Util.copyFile(new File(ignoreFile), f);
        }
    }


    /**
     * generates a complete query from a parameterised query + parameter queries
     * @param namedQuery the current query
     * @param doc        the XML file containing the queries
     * @return a complete query
     * @throws XPathExpressionException
     */
    protected String generateCompleteQuery(Node namedQuery, Document doc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();

        String queryName = namedQuery.getAttributes().getNamedItem("id").getTextContent();

        currentQueryName = queryName;
        System.out.println("\n* Generating query: " + currentQueryName);

//                get the parameterised query
        Node queryStringNode = namedQuery.getChildNodes().item(1);  // TODO: get it by name
        String queryString = queryStringNode.getTextContent();

        // this returns all paramquery nodes
        XPathExpression xGetParamNode = xpath.compile("//query[@id='" + queryName + "']/parameter");
        NodeList paramQueries = (NodeList) xGetParamNode.evaluate(doc, XPathConstants.NODESET);

        Map<String, String> paramMap = new HashMap<String, String>();

        for (int j = 0; j < paramQueries.getLength(); j++) {
//                    this is one parameter query item
            Node paramNode = paramQueries.item(j);

//                get the list of parameter names for which we query
            XPathExpression xGetParamNames = xpath.compile("paramname");
            NodeList paramNames = (NodeList) xGetParamNames.evaluate(paramNode, XPathConstants.NODESET);

            XPathExpression xGetParamQueryNodes = xpath.compile("paramvaluesquery");
            NodeList paramQueryNodes = (NodeList) xGetParamQueryNodes.evaluate(paramNode, XPathConstants.NODESET);

//                    the query to obtain the parameters
            String paramQueryString = paramQueryNodes.item(0).getTextContent();

            populateParamMap(paramMap, paramNames, paramQueryString);
        }

        return substituteQueryParameters(queryString, paramMap);
    }


    /**
     * takes a parameterised query string and a param name - param value map
     * and substitutes parameters for values
     * @param parameterisedQuery the query
     * @param paramMap           the map containing parameters + values
     * @return a query string
     */
    protected String substituteQueryParameters(String parameterisedQuery, Map<String, String> paramMap) {
        StringBuilder sb = new StringBuilder(parameterisedQuery);
        String completeQuery = sb.toString();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String parameter = "%" + entry.getKey() + "%";
            String value = entry.getValue();
            completeQuery = completeQuery.replace(parameter, value);
        }
        return completeQuery;
    }


    /**
     * Saves the query string to a text file. Unspectacular.
     * @param completeQuery the query string
     * @throws IOException
     */
    protected void saveCompleteQuery(String completeQuery) throws IOException {
        String queryFileName = outputDir + "query" + queryCount + "_" + currentQueryName + ".txt";
        System.out.println("\n  Saving query to: " + queryFileName);
        Writer out = new OutputStreamWriter(new FileOutputStream(queryFileName));
        try {
            out.write(completeQuery);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    protected abstract void populateParamMap(Map<String, String> paramQueryMap, NodeList paramNames, String paramQueryString);

}
