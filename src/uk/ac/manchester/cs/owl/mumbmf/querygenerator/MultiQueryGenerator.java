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

/*
 * TODO: I've just copied this class in from the old project, i.e. it needs fixing to work with the new project
 */

public class MultiQueryGenerator {

    protected List<String> queryFiles = new ArrayList<String>();
    protected String executedQueryFile;
    protected QueryGenerator queryGenerator;
    protected String outputDir;
    protected String currentQueryType;

    public MultiQueryGenerator(List<String> queryFiles, QueryGenerator queryGenerator, String outputDir) {
        this.queryFiles = queryFiles;
        this.executedQueryFile = queryFiles.get(0);
        this.queryGenerator = queryGenerator;
        this.outputDir = outputDir;

    }

    public void generateMultipleQueryTypes() {
//        for each query in each file, load the query as queryString into an array
//        for one of them (first one seems reasonable), get the paramqueries + parameters
//        execute paramqueries, get the map
//        for each queryString, replace the param with value and save the query file

        List<Map<String, String>> paramValuesMaps = generateParamValueMapsForAllQueries();

        for (String s : queryFiles) {
            currentQueryType = getQueryType(s);

            List<String> queriesForQueryType = getQueries(new File(s));
            for (int i = 0; i < queriesForQueryType.size(); i++) {
                String query = queriesForQueryType.get(i);
                Map<String, String> paramValuesForQuery = paramValuesMaps.get(i);
                String completedQuery = generateCompleteQuery(query, paramValuesForQuery);
                saveCompleteQuery(completedQuery, i + 1);
            }
            saveQueryMixFiles();

        }

    }

    private void saveQueryMixFiles() {

//        File ignoreFile = new File("/Users/fishdelish/fishbench/fishmark/fishmark_queries/ignoreQueries.txt");
//        File ignoreFileCp = new File(outputDir + "/" + currentQueryType + "/ignoreQueries.txt");
//        Util.copyFile(ignoreFile, ignoreFileCp);
//
//        System.out.println("SAVED IGNORE FILE TO " + outputDir + "/" + currentQueryType + "/ignoreQueries.txt");
//
//        File f2 = new File("/Users/fishdelish/fishbench/fishmark/fishmark_queries/descdummy.txt");
//        File f2copy = new File(outputDir + "/" + currentQueryType + "/descdummy.txt");
//        Util.copyFile(f2, f2copy);


        File f3 = new File("/Users/fishdelish/fishbench/fishmark/fishmark_queries/querymix.txt");
        File f3copy = new File(outputDir + "/" + currentQueryType + "/querymix.txt");
        Util.copyFile(f3, f3copy);
    }

    protected String getQueryType(String s) {
        if (s.toLowerCase().contains("sparql")) {
            new File(outputDir + "/sparql").mkdirs();
            return "sparql";
        } else if (s.toLowerCase().contains("sql")) {
            new File(outputDir + "/sql").mkdirs();
            return "sql";
        } else if (s.toLowerCase().contains("obda")) {
            new File(outputDir + "/obda").mkdirs();
            return "obda";
        }
        return "";
    }

    protected void saveCompleteQuery(String completeQuery, int queryCount) {
        String fileName = outputDir + "/" + currentQueryType + "/query" + queryCount + ".txt";
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

    public List<Map<String, String>> generateParamValueMapsForAllQueries() {

        List<Map<String, String>> allParamValueMaps = new ArrayList<Map<String, String>>();

        File queryFile = new File(executedQueryFile);

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(queryFile);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("/Queries/*");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            // for each query, generate a map of parameter names and values
//            then replace the parameters in the final query with the respective (randomly selected) values
            for (int i = 0; i < nodes.getLength(); i++) {

                Node namedQuery = nodes.item(i);
                String queryName = namedQuery.getNodeName();
                System.out.println("* Generating query parameters: " + queryName);

                // this returns all paramquery nodes
                XPathExpression xGetParamNode = xpath.compile("//" + queryName + "/parameter");
                NodeList paramQueries = (NodeList) xGetParamNode.evaluate(doc, XPathConstants.NODESET);

                Map<String, String> paramMap = new HashMap<String, String>();

                for (int j = 0; j < paramQueries.getLength(); j++) {
//                    this is one parameter query item
                    Node paramNode = paramQueries.item(j);

//                get the parameter names for which we query
                    XPathExpression xGetParamNames = xpath.compile("paramname");
                    NodeList paramNames = (NodeList) xGetParamNames.evaluate(paramNode, XPathConstants.NODESET);

                    XPathExpression xGetParamQueryNodes = xpath.compile("paramvalues/query");
                    NodeList paramQueryNodes = (NodeList) xGetParamQueryNodes.evaluate(paramNode, XPathConstants.NODESET);

//                    the query to obtain the parameters
                    String paramQueryString = paramQueryNodes.item(0).getTextContent();

                    addRandomParamValuesToParamMap(paramMap, paramNames, paramQueryString);

                    allParamValueMaps.add(paramMap);
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allParamValueMaps;
    }

    protected void addRandomParamValuesToParamMap(Map<String, String> paramMap, NodeList paramNames, String paramQueryString) {
        queryGenerator.populateParamMap(paramMap, paramNames, paramQueryString);
    }


    public List<String> getQueries(File queryFile) {
        List<String> allQueriesForQueryType = new ArrayList<String>();

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(queryFile);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("/Queries/*");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            // for each query, generate a map of parameter names and values
//            then replace the parameters in the final query with the respective (randomly selected) values
            for (int i = 0; i < nodes.getLength(); i++) {
                Node namedQuery = nodes.item(i);

//                get the parameterised query
                Node queryStringNode = namedQuery.getChildNodes().item(1);
                String queryString = queryStringNode.getTextContent();
                allQueriesForQueryType.add(queryString);
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
        return allQueriesForQueryType;
    }


    protected String generateCompleteQuery(String queryString, Map<String, String> paramMap) {
        StringBuilder sb = new StringBuilder(queryString);
        String completeQuery = sb.toString();

        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String parameter;
            String value = entry.getValue();

//            TODO: hacks to fit the current query XML files
//            we should fix the query XML files so we don't need the hacks

            if (currentQueryType.equals("sql")) {
                parameter = "%" + entry.getKey() + "%";
            } else {
                parameter = "%" + entry.getKey().toLowerCase() + "%";
            }

            if (currentQueryType.equals("obda")) {
                value = "\"" + value + "\"^^xsd:string";
            }

            completeQuery = completeQuery.replace(parameter, value);

        }
        return completeQuery;
    }
}
