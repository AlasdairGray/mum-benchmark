package uk.ac.manchester.cs.owl.mumbmf.querygenerator;

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

    public WeightedQueryMixGenerator(String seedType, Map<String, String> queryTemplates, QueryGenerator queryGenerator, String outputDir) {
        super(seedType, queryTemplates, queryGenerator, outputDir);
    }

//    TODO: nothing happening here yet - implement!


    public void generateQueryMix() {
//       generate map: query name --> frequency
//        e.g. [Comname, 43]
//        read in query files for SQL, SPARQL, OBDA
//        for each query in the file, generate parameters for the query acc to frequency in the map

//        this gives us a list of 1 map per query
//        we need to instantiate each query the correct number of times


    }
}
