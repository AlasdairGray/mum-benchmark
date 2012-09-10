package uk.ac.manchester.cs.owl.mumbmf.querygenerator;

import uk.ac.manchester.cs.owl.mumbmf.querygenerator.MultiQueryGenerator;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.QueryGenerator;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by
 * User: Samantha Bail
 * Date: 03/08/2012
 * Time: 00:29
 * The University of Manchester
 */


public class WeightedQueryMixGenerator extends MultiQueryGenerator {

//    TODO: nothing happening here yet - implement!

    public WeightedQueryMixGenerator(List<String> queryFiles, QueryGenerator queryGenerator, String outputDir) {
        super(queryFiles, queryGenerator, outputDir);
    }


    public void generateQueryMix() {
//       generate map: query name --> frequency
//        e.g. [Comname, 43]
//        read in query files for SQL, SPARQL, OBDA
//        for each query in the file, generate parameters for the query acc to frequency in the map

//        this gives us a list of 1 map per query
//        we need to instantiate each query the correct number of times
        List<Map<String, String>> paramValuesMaps = generateParamValueMapsForAllQueries();

         for (String s : queryFiles) {
            currentQueryType = getQueryType(s);

            List<String> queriesForQueryType = getQueries(new File(s));
            for (int i = 0; i < queriesForQueryType.size(); i++) {
                String query = queriesForQueryType.get(i);
                Map<String, String> paramValuesForQuery = paramValuesMaps.get(i);
                String completedQuery = generateCompleteQuery(query, paramValuesForQuery);
            }
        }

    }
}
