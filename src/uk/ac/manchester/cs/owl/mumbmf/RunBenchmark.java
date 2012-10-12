package uk.ac.manchester.cs.owl.mumbmf;

import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SqlConnection;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SqlConnectionParameters;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.TestDriver;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.MultiQueryGenerator;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.QueryGenerator;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.SparqlQueryGenerator;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.SqlQueryGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by
 * User: Samantha Bail
 * Date: 19/06/2012
 * Time: 19:47
 * The University of Manchester
 */

//   PARAMETERS FOR THE TESTDIVER
//
// -runs <number of runs>
//The number of query mix runs. Default: 50
// -idir <directory>
//The input parameter directory which was created by the Data Generator. Default: "td_data"
//-ucf <path to use case file>
//Specifies the use case, which in turn defines the combination of queries from one or more query mixes. Different use cases are found under the "usecase" directory.
// -w <number of warm up runs>	 Number of runs executed before the actual test to warm up the store. Default: 10
//-o <result XML file> 	 The output file containing the aggregated result overview. Default: "benchmark_result.xml"
//-dg <default graph URI> 	 Specify a default graph for the queries. Default: null
//-mt <number of clients>	 Benchmark with multiple concurrent clients.
//-sql
//Use JDBC connection to a RDBMS. Instead of a SPARQL-Endpoint, the test driver needs a JDBC URL as argument.
//default: not set
//-dbdriver <DB-Driver Class Name>
//default: com.mysql.jdbc.Driver
//-seed <Long value>	 Set the seed for the random number generator used for the parameter generation.
//-t <Timeout in ms>	 If for a specific query the complete result is not read after the specified timeout, the client disconnects and reports a timeout to the Test Driver. This is also the maximum runtime a query can contribute to the metrics.
//-q	Turn on qualification mode. For more information, see the qualification chapter of the use case.
//-qf <qualification file name>	Change the qualification file name, also see the qualification chapter of the use case.
//-rampup
//Run test driver in ramp-up/warm-up mode. The test driver will execute randomized queries until it is stopped - ideally when the store reached steady state and is not improving any more.
//-u <Service endpoint URI for SPARQL Update>
//If you are running update queries in your tests this option defines where the SPARQL update service endpoint can be found.
//-udataset <file name>
//The file name of the update dataset.
//-uqp <update query parameter>
//The forms parameter name for the SPARQL Update query string.
//Default: update

public class RunBenchmark {

	private static void usage() {
		System.out.println("\nThe benchmark should be run with one of the following flags as the first command line argument:");
		System.out.println("\t-g to generate a set of queries");
		System.out.println("\t-b to run the benchmark");
		
		System.out.println("\nThe following are command line arguments to control the behaviour of the query generation process.");
		System.out.println("\t-f <query file> The XML file containing the parameterised queries for the benchmark.");
		System.out.println("\t-type [sparql|sql] Specify whether to generate sparql or sql queries.");
		System.out.println("\t-output <output directoyr> The location to store the generated query files.");
		System.out.println("\t-ignorefile <filename> Ignore the specified file.");
		System.out.println("\t-qm <filename> File containing details of the query mix.");
		System.out.println("\t-dbdriver <DB-Driver Class Name> (Required if generating sql queries)");
		System.out.println("\t-db <url> (Required for sql queries) Location of the database server.");
		System.out.println("\t-login <username> (Required for sql queries) Username for accessing the database.");
		System.out.println("\t-pw <password> (Required for sql queries) Password for accessing the database.");
		System.out.println("\t<SPARQL endpoint URL> (Required for SPARQL queries) Location of the SPARQL endpoint.");

		System.out.println("\nThe following are command line arguments to vary the behaviour of the benchmark framework when running the benchmark.");
		System.out.println("\t-runs <number of runs> (Default: 50) The number of query mix runs.");
		System.out.println("\t-idir <directory> (Default: td_data) The input parameter directory which was created by the Data Generator.");
		System.out.println("\t-ucf <path to use case file> Specifies the use case, which in turn defines the combination of queries from one or more query mixes. Different use cases are found under the \"usecase\" directory.");
		System.out.println("\t-w <number of warm-up runs> (Default: 10) Number of runs executed before the actual test to warm up the store.");
		System.out.println("\t-o <result file> (Default: benchmark_result.xml) The output file containing the aggregated result overview.");
		System.out.println("\t-df <default graph URI> (Default: null) Specify a default graph for the queries.");
		System.out.println("\t-mt <number of clients> Benchmark with multiple concurrent clients.");
		System.out.println("To use a database connection, the arguments are:");
		System.out.println("\t-sql <url> (Required) Use JDBC connection to a RDBMS. Instead of a SPARQL-Endpoint, the test driver needs a JDBC URL as argument.");
		System.out.println("\t-dbdriver <DB-Driver Class Name (Default: com.mysql.jdbc.Driver)");
		System.out.println("\t-seed <long value>");
		System.out.println("\t-t <Timeout in ms>     If for a specific query the complete result is not read after the specified timeout, the client disconnects and reports a timeout to the Test Driver. This is also the maximum runtime a query can contribute to the metrics.");
		System.out.println("\t-q    Turn on qualification mode. For more information, see the qualification chapter of the use case.");
		System.out.println("\t-qf <qualification file name> Change the qualification file name, also see the qualification chapter of the use case.");
		System.out.println("\t-rampup Run test driver in ramp-up/warm-up mode. The test driver will execute randomized queries until it is stopped - ideally when the store reached steady state and is not improving any more.");
		System.out.println("\t-u <Service endpoint URI for SPARQL Update> If you are running update queries in your tests this option defines where the SPARQL update service endpoint can be found.");
		System.out.println("\t-udataset <file name> The file name of the update dataset.");
		System.out.println("\t-uqp <update query parameter> (Default: update) The forms parameter name for the SPARQL Update query string.\n");		
	}

    public static void main(String[] args) throws Exception {
    	if (args.length < 1) {
    		usage();
    		System.exit(1);
    	}
        String flag = args[0];

        if (flag.equals("-b")) {  // run benchmark
            String[] params = new String[args.length - 1];
            System.arraycopy(args, 1, params, 0, args.length - 1);
            TestDriver driver = new TestDriver(params);
            driver.init();
            driver.run();

        } else if (flag.equals("-g")) {  // generate queries for a single query type
            SqlConnectionParameters sqlConnParams = new SqlConnectionParameters();
            String queryType = "";
            String queryFile = "";
            String sparqlEndpoint = "";
            String outputDir = "";
            String ignoreFile = "";
            String queryMix = "";

            int i = 1;

            while (i < args.length) {
                if (args[i].equals("-f")) {
                    queryFile = args[i++ + 1];
                } else if (args[i].equals("-type")) {
                    queryType = args[i++ + 1];
                } else if (!args[i].startsWith("-")) {
                    sparqlEndpoint = args[i];
                } else if (args[i].equals("-dbdriver")) {
                    sqlConnParams.dbDriver = args[i++ + 1];
                } else if (args[i].equals("-db")) {
                    sqlConnParams.dbServer = args[i++ + 1];
                } else if (args[i].equals("-login")) {
                    sqlConnParams.dbLogin = args[i++ + 1];
                } else if (args[i].equals("-pw")) {
                    sqlConnParams.dbPassword = args[i++ + 1];
                } else if (args[i].equals("-output")) {
                    outputDir = args[i++ + 1];
                } else if (args[i].equals("-ignorefile")) {
                    ignoreFile = args[i++ + 1];
                } else if (args[i].equals("-qm")) {
                    queryMix = args[i++ + 1];
                }

                i++;
            }

            System.out.println("* Generating queries: ");
            System.out.println("  -type: " + queryType);
            System.out.println("  -f: " + queryFile);
            System.out.println("  -qm: " + queryMix);
            System.out.println("  -output: " + outputDir);
            System.out.println("  -ignorefile: " + ignoreFile);

            if (queryType.equals("sparql")) {
                System.out.println("  SPARQL endpoint: " + sparqlEndpoint);
                QueryGenerator qg = new SparqlQueryGenerator(sparqlEndpoint, outputDir);
                qg.generateQueries(queryFile, queryMix, ignoreFile);
            } else if (queryType.equals("sql")) {
                System.out.println("  DB server: " + sqlConnParams.dbServer);
                System.out.println("  DB login: " + sqlConnParams.dbLogin);
                System.out.println("  DB password: " + sqlConnParams.dbPassword);
                System.out.println("  DB driver: " + sqlConnParams.dbDriver);

                QueryGenerator qg = new SqlQueryGenerator(sqlConnParams, outputDir);
                qg.generateQueries(queryFile, queryMix, ignoreFile);
            }

        } else {
		usage();
		System.exit(1);
	}

    }


}
