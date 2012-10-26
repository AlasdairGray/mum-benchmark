package uk.ac.manchester.cs.owl.mumbmf.querygenerator;

import org.jdom.output.DOMOutputter;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SparqlConnection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/07/2012
 * Time: 16:58
 * The University of Manchester
 */


public class SparqlQueryGenerator extends QueryGenerator {

    private String sparqlEndpoint;

    /**
     * Constructor
     * @param sparqlEndpoint the SPARQL endpoint to query for param values
     * @param outputDir      dir to save the queries to
     */
    public SparqlQueryGenerator(String sparqlEndpoint, String outputDir) {
        this.sparqlEndpoint = sparqlEndpoint;
        this.outputDir = outputDir;
        new File(outputDir).mkdirs();
    }

    /**
     * populates the param map with values
     * @param paramMap         map
     * @param paramNames       list of param names
     * @param paramQueryString query to get param values from
     */
    @Override
    public void populateParamMap(Map<String, String> paramMap, NodeList paramNames, String paramQueryString) {
        Document queryResult = null;
        try {
            queryResult = getSPARQLQueryResult(paramQueryString);
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println(queryResult.toString());
        Node record = getRandomRecord(queryResult);

        for (int i = 0; i < paramNames.getLength(); i++) {
            String paramName = paramNames.item(i).getTextContent();
            String paramValue = getParameterValue(paramName, record);

            System.out.println("\n  PARAM NAME: " + paramName);
            System.out.println("  PARAM VALUE: " + paramValue);

            paramMap.put(paramName.toLowerCase(), paramValue);
        }
    }


    /**
     * executes a SPARQL query
     * @param queryString the query
     * @return a SPARQL result DOM
     * @throws Exception
     */
    private Document getSPARQLQueryResult(String queryString) throws Exception {
        SparqlConnection conn = new SparqlConnection(sparqlEndpoint, null, 0);
        org.jdom.Document doc = conn.executeSimpleQuery(queryString);
        DOMOutputter domOut = new org.jdom.output.DOMOutputter();
        return domOut.output(doc);
    }


    /**
     * gets a value for a paramter from a SPARQL result file
     * @param paramName
     * @param result
     * @return
     */
    private String getParameterValue(String paramName, Node result) {
        try {
// TODO: fix xpath
//            XPath xpath = XPathFactory.newInstance().newXPath();
//            XPathExpression expr = xpath.compile("binding");
//            NodeList bindings = (NodeList) expr.evaluate(result, XPathConstants.NODESET);

            for (int i = 0; i < result.getChildNodes().getLength(); i++) {
                Node n = result.getChildNodes().item(i);
                if (n.getNodeName().equals("binding")) {
                    Attr att = (Attr) n.getAttributes().getNamedItem("name");

//                    if (att.getValue().equals(paramName)) {
//                        return n.getFirstChild().getTextContent();
//                    }
                    if (att.getValue().equals(paramName)) {
                        for (int j = 0; j < n.getChildNodes().getLength(); j++) {
                            Node node = n.getChildNodes().item(j);
//                              printNode(node, 0);
                            if (!node.getTextContent().trim().equals("")) {
//                                      System.out.println("Value: " + node.getTextContent());
                                return node.getTextContent();
                            }
                        }
                        return n.getFirstChild().getTextContent();
                    }


                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * Selects a random result from the SPARQL query result document
     * @param doc the SPARQL query result document
     * @return randomly selected node
     */
    public Node getRandomRecord(Document doc) {
        NodeList results = doc.getElementsByTagName("result");
        List<Node> allResults = new ArrayList<Node>();
        for (int i = 0; i < results.getLength(); i++) {
            Node n = results.item(i);
            allResults.add(n);
        }

        Collections.shuffle(allResults);

        try {
            return allResults.get(0);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;

    }


}
