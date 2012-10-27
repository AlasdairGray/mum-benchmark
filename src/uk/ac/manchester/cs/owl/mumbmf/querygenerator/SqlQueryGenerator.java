package uk.ac.manchester.cs.owl.mumbmf.querygenerator;

import org.w3c.dom.NodeList;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SqlConnection;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SqlConnectionParameters;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/07/2012
 * Time: 17:04
 * The University of Manchester
 */


public class SqlQueryGenerator extends QueryGenerator {

    private SqlConnectionParameters connectionParameters;


    /**
     * Constructor.
     * @param connectionParameters The SQL DB connection parameters
     * @param outputDir            directory to save the queries to
     */
    public SqlQueryGenerator(SqlConnectionParameters connectionParameters, String outputDir) {
        this.connectionParameters = connectionParameters;
        this.outputDir = outputDir + "/sql/";
        new File(outputDir).mkdirs();
    }

    /**
     * Populates the paramMap with random values from a SQL query result
     * @param paramMap
     * @param paramNames
     * @param paramQueryString
     */
    @Override
    public void populateParamMap(Map<String, String> paramMap, NodeList paramNames, String paramQueryString) {
        ResultSet queryResult = getQueryResult(paramQueryString);

        int rowcount = 0;
        try {
            if (queryResult.last()) {
                rowcount = queryResult.getRow();
                queryResult.beforeFirst();
                // not rs.first() because the rs.next() below will move on, missing the first element

                String paramName = "";
                String paramValue = "";
                boolean validRecordFound = false;

                Random randomGenerator = new Random();

                while (!validRecordFound) {
                    int randomInt = randomGenerator.nextInt(rowcount);
//                moving the cursor to a random row
                    queryResult.absolute(randomInt + 1);
                    validRecordFound = isValidRecord(queryResult, paramNames);
                }

                for (int i = 0; i < paramNames.getLength(); i++) {
                    paramName = paramNames.item(i).getTextContent();
                    paramValue = queryResult.getString(paramName);
                        System.out.println("\n  PARAM NAME: " + paramName);
                    System.out.println("  PARAM VALUE: " + paramValue);

//                    finally, add the parameter name + value to the map
                    paramMap.put(paramName, paramValue);
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to deal with ' in the results which can mess up the queries
     * TODO: find out how to manage queries with ' in them anyway
     * @param queryResult
     * @param paramNames
     * @return
     */
    private boolean isValidRecord(ResultSet queryResult, NodeList paramNames) {
        for (int i = 0; i < paramNames.getLength(); i++) {
            String paramName = paramNames.item(i).getTextContent();
            try {
                String paramValue = queryResult.getString(paramName);
                if (paramValue.contains("'")) {
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Executes a SQL query and returns the results
     * @param paramQueryString the query
     * @return
     */
    private ResultSet getQueryResult(String paramQueryString) {
        SqlConnection ex = new SqlConnection(connectionParameters, 0);
        return ex.executeSimpleQuery(paramQueryString);
    }


}
