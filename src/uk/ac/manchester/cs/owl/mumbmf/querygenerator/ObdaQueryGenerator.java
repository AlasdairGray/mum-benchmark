package uk.ac.manchester.cs.owl.mumbmf.querygenerator;

import it.unibz.krdb.obda.owlapi3.OWLResultSet;
import org.w3c.dom.NodeList;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.ObdaConnection;

import java.io.File;
import java.util.*;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/07/2012
 * Time: 17:45
 * The University of Manchester
 */


public class ObdaQueryGenerator extends QueryGenerator {

    private String owlFile;
    private String obdaFile;

    /**
     * Constructor
     * @param owlFile the ontology
     * @param obdaFile the mappings
     * @param outputDir directory to save the completed queries to
     */
    public ObdaQueryGenerator(String owlFile, String obdaFile, String outputDir) {
        this.owlFile = owlFile;
        this.obdaFile = obdaFile;
        this.outputDir = outputDir + "/obda/";
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

        OWLResultSet queryResult = getQueryResult(paramQueryString);
        Map<String, String> record = getRandomRecord(queryResult, paramNames);

        for (int i = 0; i < paramNames.getLength(); i++) {
            String paramName = paramNames.item(i).getTextContent();
            String paramValue = record.get(paramName);
            paramMap.put(paramName, paramValue);
        }
    }


    /**
     * executes a query
     * @param paramQueryString
     * @return
     */
    private OWLResultSet getQueryResult(String paramQueryString) {
        ObdaConnection ex = new ObdaConnection(owlFile, obdaFile, 0);
        return ex.executeSimpleQuery(paramQueryString);
    }


    /**
     * selects a random result from the result set
     * @param queryResult
     * @param paramNames
     * @return
     */
    public Map<String, String> getRandomRecord(OWLResultSet queryResult, NodeList paramNames) {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();

        try {
            while (queryResult.nextRow()) {
                Map<String, String> record = new HashMap<String, String>();

                for (int i = 0; i < paramNames.getLength(); i++) {
                    String paramName = paramNames.item(i).getTextContent();
                    String paramValue = queryResult.getOWLLiteral(paramName).toString();
                    record.put(paramName.toLowerCase(), paramValue);
                }
                results.add(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.shuffle(results);
        return results.get(0);
    }
}
