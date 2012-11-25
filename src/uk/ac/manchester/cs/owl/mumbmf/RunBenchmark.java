package uk.ac.manchester.cs.owl.mumbmf;

import uk.ac.manchester.cs.owl.mumbmf.queryexecution.*;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by
 * User: Samantha Bail
 * Date: 19/06/2012
 * Time: 19:47
 * The University of Manchester
 */

public class RunBenchmark {


    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            UsageInfo.printUiUsage();
            System.exit(1);
        }
        String flag = args[0];
        String[] params = new String[args.length - 1];
        System.arraycopy(args, 1, params, 0, args.length - 1);

        if (flag.equals("-b")) {  // run benchmark
            runBenchmark(params);

        } else if (flag.equals("-g")) {  // generate queries for a single query type
            runMultiQueryGeneration(params);

        } else if (flag.equals("-m")) {
            runGenerateAndBenchmarkMultipleQuerySets(params);

        } else {
            UsageInfo.printUiUsage();
            System.exit(1);
        }

    }

    /**
     * @param params
     */
    public static void runGenerateAndBenchmarkMultipleQuerySets(String[] params) {
        int i = 0;

        String generateArgsFile = "";
        int querySets = 1;
        List<List<String>> paramList = new ArrayList<List<String>>();
        List<String> globalParams = new ArrayList<String>();
        while (i < params.length) {
            if (params[i].equals("-obda")) {
                paramList.add(getParams("obda", params[i++ + 1]));
            } else if (params[i].equals("-sql")) {
                paramList.add(getParams("sql", params[i++ + 1]));
            } else if (params[i].equals("-stardog")) {
                paramList.add(getParams("stardog", params[i++ + 1]));
            } else if (params[i].equals("-sparql")) {
                paramList.add(getParams("sparql", params[i++ + 1]));
            } else if (params[i].equals("-genargs")) {
                generateArgsFile = params[i++ + 1];
            } else if (params[i].equals("-querysets")) {
                querySets = Integer.parseInt(params[i++ + 1]);
            } else {
                globalParams.add(params[i]);
                globalParams.add(params[i++ + 1]);
            }
            i++;
        }


        for (List<String> p : paramList) {
            p.addAll(globalParams);
        }

        try {
            String[] genargs = readLines(generateArgsFile);

            for (int j = 0; j < querySets; j++) {
                runMultiQueryGeneration(genargs);
                for (List<String> p : paramList) {
                    for (String s : p) {
                        System.out.println(s);
                    }
                    runBenchmark(p.toArray(new String[p.size()]));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * helper to split a string of parameters into an array
     * @param type
     * @param args
     * @return
     */
    private static List<String> getParams(String type, String args) {
        String arglist = args.substring(1, args.length() - 1);
        List<String> paramList = new ArrayList<String>();
        paramList.add("-type");
        paramList.add(type);
        Collections.addAll(paramList, arglist.split("\\s+"));
        return paramList;
    }

    /**
     * runs a benchmark on a given set of parameters
     * @param params
     */
    private static void runBenchmark(String[] params) {

        TestDriver driver = new TestDriver(params);
        driver.init();
        driver.run();
    }


    public static void runMultiQueryGeneration(String[] params) {

        String seedType = "";  // mandatory
        String queryOutputDir = "";
        String obdaTemplate = "";
        String sparqlTemplate = "";
        String stardogTemplate = "";
        String sqlTemplate = "";
        String qmFile = "";
        String ignoreFile = "";

        // one set of connection parameters is required
        SqlConnectionParameters sqlConn = new SqlConnectionParameters();
        SparqlConnectionParameters sparqlConn = new SparqlConnectionParameters();
        ObdaConnectionParameters obdaConn = new ObdaConnectionParameters();
        StardogConnectionParameters stardogConn = new StardogConnectionParameters();

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
            } else if (params[i].equals("-stardogdb")) {
                stardogConn.dbName = params[i++ + 1];
            } else if (params[i].equals("-stardogurl")) {
                stardogConn.dbUrl = params[i++ + 1];
            } else if (params[i].equals("-stardoglogin")) {
                stardogConn.login = params[i++ + 1];
            } else if (params[i].equals("-stardogpw")) {
                stardogConn.password = params[i++ + 1];
            }
            i++;
        }


//        if no connection parameters are given, exit
        if (sqlConn.dbServer.equals("") && sparqlConn.sparqlEndpoint.equals("")
                && obdaConn.obdaFile.equals("") && stardogConn.dbUrl.equals("")) {
            UsageInfo.printUiUsage();
            System.exit(1);
        }

//        add all template files to a map
        Map<String, String> queryTemplates = new HashMap<String, String>();
        queryTemplates.put("obda", obdaTemplate);
        queryTemplates.put("sql", sqlTemplate);
        queryTemplates.put("sparql", sparqlTemplate);
        queryTemplates.put("stardog", stardogTemplate);

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

    public static String[] readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
            sb.append(" ");
        }
        bufferedReader.close();

        return sb.toString().split("\\s+");
    }


}
