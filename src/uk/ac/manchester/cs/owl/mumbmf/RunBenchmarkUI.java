package uk.ac.manchester.cs.owl.mumbmf;

import uk.ac.manchester.cs.owl.mumbmf.queryexecution.ObdaConnectionParameters;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SparqlConnectionParameters;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SqlConnectionParameters;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.TestDriver;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by
 * User: Samantha Bail
 * Date: 19/06/2012
 * Time: 19:47
 * The University of Manchester
 */

public class RunBenchmarkUI {


    public static void multiQueryGeneration(String[] params) {

        String seedType = "";  // mandatory
        String queryOutputDir = "";
        String obdaTemplate = "";
        String sparqlTemplate = "";
        String sqlTemplate = "";
        String qmFile = "";
        String ignoreFile = "";

        // one set of connection parameters is required
        SqlConnectionParameters sqlConn = new SqlConnectionParameters();
        SparqlConnectionParameters sparqlConn = new SparqlConnectionParameters();
        ObdaConnectionParameters obdaConn = new ObdaConnectionParameters();

        int i = 0;
        while (i < params.length) {
            if (params[i].equals("-seedtype")) {
                seedType = params[i++ + 1];
            } else if (params[i].equals("-output")) {
                queryOutputDir = params[i++ + 1];
            } else if (params[i].equals("-dbdriver")) {
                sqlConn.dbDriver = params[i++ + 1];
            } else if (params[i].equals("-dbserver")) {
                sqlConn.dbServer = params[i++ + 1];
            } else if (params[i].equals("-dblogin")) {
                sqlConn.dbLogin = params[i++ + 1];
            } else if (params[i].equals("-dbpw")) {
                sqlConn.dbPassword = params[i++ + 1];
            } else if (params[i].equals("-obdafile")) {
                obdaConn.obdaFile = params[i++ + 1];
            } else if (params[i].equals("-owlfile")) {
                obdaConn.owlFile = params[i++ + 1];
            } else if (params[i].equals("-sparqlendpoint")) {
                sparqlConn.sparqlEndpoint = params[i++ + 1];
            } else if (params[i].equals("-sql")) {
                sqlTemplate = params[i++ + 1];
            } else if (params[i].equals("-sparql")) {
                sparqlTemplate = params[i++ + 1];
            } else if (params[i].equals("-obda")) {
                obdaTemplate = params[i++ + 1];
            } else if (params[i].equals("-qmfile")) {
                qmFile = params[i++ + 1];
            } else if (params[i].equals("-igfile")) {
                ignoreFile = params[i++ + 1];
            }
            i++;
        }

//        if no connection parameters are given, exit
        if (sqlConn.dbServer.equals("") && sparqlConn.sparqlEndpoint.equals("") && obdaConn.obdaFile.equals("")) {
            printUsage();
            System.exit(1);
        }

//        add all template files to a map
        Map<String, String> queryTemplates = new HashMap<String, String>();
        queryTemplates.put("obda", obdaTemplate);
        queryTemplates.put("sql", sqlTemplate);
        queryTemplates.put("sparql", sparqlTemplate);

        QueryGenerator qg = null;
        if (seedType.equals("obda")) {
            qg = new ObdaQueryGenerator(obdaConn.owlFile, obdaConn.obdaFile, queryOutputDir);
        } else if (seedType.equals("sql")) {
            qg = new SqlQueryGenerator(sqlConn, queryOutputDir);
        } else if (seedType.equals("sparql")) {
            qg = new SparqlQueryGenerator(sparqlConn.sparqlEndpoint, queryOutputDir);
        }

        MultiQueryGenerator gen = new MultiQueryGenerator(seedType, queryTemplates, qg, queryOutputDir);
        gen.generateMultipleQueryTypes(qmFile, ignoreFile);

    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        String flag = args[0];
        String[] params = new String[args.length - 1];
        System.arraycopy(args, 1, params, 0, args.length - 1);

        if (flag.equals("-b")) {  // run benchmark

            runBenchmark(params);

        } else if (flag.equals("-g")) {  // generate queries for a single query type
            runQueryGeneration(params);

        } else if (flag.equals("-m")) {
            runGenerateBenchmarkLoop(params);

        } else {
            printUsage();
            System.exit(1);
        }

    }

    private static void runGenerateBenchmarkLoop(String[] params) {
        int i = 0;

        String benchmarkArgsFile = "";
        String generateArgsFile = "";
        int querySets = 1;

        while (i < params.length) {
            if (params[i].equals("-bargs")) {
                benchmarkArgsFile = params[i++ + 1];
            } else if (params[i].equals("-gargs")) {
                generateArgsFile = params[i++ + 1];
            } else if (params[i].equals("-querysets")) {
                querySets = Integer.parseInt(params[i++ + 1]);
            }
            i++;
        }

        try {
            String[] bmargs = readLines(benchmarkArgsFile);
            String[] genargs = readLines(generateArgsFile);

            for (int j = 0; j < querySets; j++) {
                runQueryGeneration(genargs);
                runBenchmark(bmargs);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runBenchmark(String[] params) {
        TestDriver driver = new TestDriver(params);
        driver.init();
        driver.run();
    }

    private static void runQueryGeneration(String[] params) {
        SqlConnectionParameters sqlConnParams = new SqlConnectionParameters();
        String queryType = "";
        String queryFile = "";
        String sparqlEndpoint = "";
        String outputDir = "";
        String ignoreFile = "[none]";
        String queryMix = "";
        String obdaFile = "";
        String owlFile = "";

        int i = 0;

        while (i < params.length) {
            if (params[i].equals("-f")) {
                queryFile = params[i++ + 1];
            } else if (params[i].equals("-type")) {
                queryType = params[i++ + 1];
            } else if (!params[i].startsWith("-")) {
                sparqlEndpoint = params[i];
            } else if (params[i].equals("-dbdriver")) {
                sqlConnParams.dbDriver = params[i++ + 1];
            } else if (params[i].equals("-db")) {
                sqlConnParams.dbServer = params[i++ + 1];
            } else if (params[i].equals("-login")) {
                sqlConnParams.dbLogin = params[i++ + 1];
            } else if (params[i].equals("-pw")) {
                sqlConnParams.dbPassword = params[i++ + 1];
            } else if (params[i].equals("-output")) {
                outputDir = params[i++ + 1];
            } else if (params[i].equals("-ignorefile")) {
                ignoreFile = params[i++ + 1];
            } else if (params[i].equals("-qm")) {
                queryMix = params[i++ + 1];
            } else if (params[i].equals("-obdafile")) {
                obdaFile = params[i++ + 1];
            } else if (params[i].equals("-owlfile")) {
                owlFile = params[i++ + 1];
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

        } else if (queryType.equals("obda")) {
            QueryGenerator qg = new ObdaQueryGenerator(owlFile, obdaFile, outputDir);
            qg.generateQueries(queryFile, queryMix, ignoreFile);
        }
    }

    public static String[] readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        bufferedReader.close();


        return sb.toString().split("\\s+");
    }

    private static void printUsage() {
        System.out.println("\nThe benchmark should be run with one of the following flags as the first command line argument:");
        System.out.println("\t-g to generate a set of queries");
        System.out.println("\t-b to run the benchmark");
        System.out.println("\t-m to generate a set of queries and run the benchmark multiple times");

        System.out.println("\nThe following are command line arguments to control the behaviour of the query generation process.");
        System.out.println("\t-f <query file> The XML file containing the parameterised queries for the benchmark.");
        System.out.println("\t-type [sparql|sql] Specify whether to generate sparql or sql queries.");
        System.out.println("\t-output <output directoyr> The location to store the generated query files.");
        System.out.println("\t-ignorefile <filename> File which specifies the queries to be ignored (can be empty).");
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
        System.out.println("\t-mt <number of clients> Benchmark with multiple concurrent clients, currently not implemented.");
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

        System.out.println("\nThe following are command line arguments to control the behaviour of the query generation and benchmarking loop:");
        System.out.println("\t-gargs <filename> Plain text file which contains the arguments to generate queries.");
        System.out.println("\t-bargs <filename> Plain text file which contains the arguments to run the benchmark.");
        System.out.println("\t-querysets <number of sets> (Default: 1) Number of query sets to generate (i.e. number of generate/benchmark loops).");


    }


}