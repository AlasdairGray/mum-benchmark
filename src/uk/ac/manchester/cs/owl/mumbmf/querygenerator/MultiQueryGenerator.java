package uk.ac.manchester.cs.owl.mumbmf.querygenerator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.manchester.cs.owl.mumbmf.util.Util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by
 * User: Samantha Bail
 * Date: 31/07/2012
 * Time: 11:15
 * The University of Manchester
 */

public class MultiQueryGenerator {

    protected Map<String, String> queryTemplates = new HashMap<String, String>();
    protected String seedTemplateFile;
    protected QueryGenerator queryGenerator;
    protected String outputDir;
    protected String currentQueryType;
    protected List<String> queryNames = new ArrayList<String>();


    /**
     * constructor
     * @param seedType
     * @param queryTemplates
     * @param queryGenerator
     * @param outputDir
     */
    public MultiQueryGenerator(String seedType, Map<String, String> queryTemplates, QueryGenerator queryGenerator, String outputDir) {
        this.queryTemplates = queryTemplates;
        this.queryGenerator = queryGenerator;
        this.seedTemplateFile = queryTemplates.get(seedType);
        this.outputDir = outputDir;
    }

    /**
     * @param qmFile
     * @param ignoreFile
     */
    public void generateMultipleQueryTypes(String qmFile, String ignoreFile) {

//        for each query in each file, load the query as queryString into an array
//        for one of them (first one seems reasonable), get the paramqueries + parameters
//        execute paramqueries, get the map
//        for each queryString, replace the param with value and save the query file

        List<Map<String, String>> paramValuesMaps = generateParamValueMapsForAllQueries();

        for (Map.Entry<String, String> entry : queryTemplates.entrySet()) {
            currentQueryType = entry.getKey();
            if (!entry.getValue().equals("")) {
                makeOutputDirectory(qmFile, ignoreFile);
                List<String> queryTemplates = getQueryTemplates(new File(entry.getValue()));
                for (int i = 0; i < queryTemplates.size(); i++) {
                    String query = queryTemplates.get(i);
                    Map<String, String> paramValuesForQuery = paramValuesMaps.get(i);
                    String completedQuery = substituteQueryParameters(query, paramValuesForQuery);
                    saveQuery(completedQuery, i);
                }
            }

        }

    }

    private String substituteQueryParameters(String queryString, Map<String, String> paramMap) {
        StringBuilder sb = new StringBuilder(queryString);
        String completeQuery = sb.toString();

        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String parameter = "";
            String value = entry.getValue();

            if (value.contains("^^")) {
                value = value.substring(1, value.indexOf("\"^^"));
            }

            if (currentQueryType.equals("sql")) {
                parameter = "%" + entry.getKey() + "%";
            } else {
                parameter = "%" + entry.getKey().toLowerCase() + "%";
            }
            completeQuery = completeQuery.replace(parameter, value);
        }
        return completeQuery;

    }


    /**
     * generates a list of maps which holds parameters + values for each query
     * @return
     */
    public List<Map<String, String>> generateParamValueMapsForAllQueries() {

        List<Map<String, String>> allParamValueMaps = new ArrayList<Map<String, String>>();
        File queryFile = new File(seedTemplateFile);

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(queryFile);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("/queries/*");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            // for each query, generate a map of parameter names and values
//            then replace the parameters in the final query with the respective (randomly selected) values
            for (int i = 0; i < nodes.getLength(); i++) {

                Node namedQuery = nodes.item(i);
                String queryName = namedQuery.getAttributes().getNamedItem("id").getTextContent();
                queryNames.add(queryName);
                System.out.println("\n* Generating query: " + queryName);
//
//                // this returns all paramquery nodes
                XPathExpression xGetParamNode = xpath.compile("//query[@id='" + queryName + "']/parameter");
                NodeList paramQueries = (NodeList) xGetParamNode.evaluate(doc, XPathConstants.NODESET);
//
                Map<String, String> paramMap = new HashMap<String, String>();
//
////                for each parameter query, fill a new map
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
//                    System.out.println(paramQueryString);
                    addRandomParamValuesToParamMap(paramMap, paramNames, paramQueryString);
                }
////                then add the map for the query to the list of all maps
                allParamValueMaps.add(paramMap);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allParamValueMaps;
    }


    /**
     * gets the query templates from an XML template file
     * @param templateFile
     * @return
     */
    public List<String> getQueryTemplates(File templateFile) {
        List<String> allQueryTemplatesForQueryType = new ArrayList<String>();

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(templateFile);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("/queries/*");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            // for each query, generate a map of parameter names and values
//            then replace the parameters in the final query with the respective (randomly selected) values
            for (int i = 0; i < nodes.getLength(); i++) {
                Node namedQuery = nodes.item(i);

//                get the parameterised query
                Node queryStringNode = namedQuery.getChildNodes().item(1);
                String queryString = queryStringNode.getTextContent();
                allQueryTemplatesForQueryType.add(queryString);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allQueryTemplatesForQueryType;
    }


    /**
     * @param completeQuery
     * @param queryCount
     */
    protected void saveQuery(String completeQuery, int queryCount) {
        String queryFileName = "query" + (queryCount + 1) + "_" + queryNames.get(queryCount) + ".txt";
        String fileName = outputDir + "/" + currentQueryType + "/" + queryFileName;
//        System.out.println("\nSaving query to: " + fileName);
        Writer out;
        try {
            out = new OutputStreamWriter(new FileOutputStream(fileName));
            out.write(completeQuery);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * creates the output directory and copies over the query mix and ignore queries files
     * @param qmFile
     * @param ignoreFile
     */
    private void makeOutputDirectory(String qmFile, String ignoreFile) {

        File queryOut = new File(outputDir + "/" + currentQueryType);
        if (!queryOut.exists()) {
            queryOut.mkdirs();

//            copy the ignore file and querymix file
            File qFile = new File(qmFile);
            File qFileCopy = new File(queryOut + "/" + qFile.getName());
            Util.copyFile(qFile, qFileCopy);

            File iFile = new File(ignoreFile);
            if (iFile.exists()) {
                File iFileCopy = new File(queryOut + "/" + iFile.getName());
                Util.copyFile(iFile, iFileCopy);
            }
        }

    }

    /**
     * @param paramMap
     * @param paramNames
     * @param paramQueryString
     */
    protected void addRandomParamValuesToParamMap(Map<String, String> paramMap, NodeList paramNames, String paramQueryString) {
        queryGenerator.populateParamMap(paramMap, paramNames, paramQueryString);
    }
}
