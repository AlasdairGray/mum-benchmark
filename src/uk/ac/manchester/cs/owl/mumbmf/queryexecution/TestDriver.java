package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.log4j.Logger;
import uk.ac.manchester.cs.owl.mumbmf.util.Util;

import java.io.*;
import java.util.*;

public class TestDriver {
    protected QueryMix queryMix;// The benchmark querymix
    protected int warmups = TestDriverDefaultValues.warmups;
    protected ServerConnection server;// only important for single threaded runs
    protected File usecaseFile = TestDriverDefaultValues.usecaseFile;// where to
    protected int nrRuns = TestDriverDefaultValues.nrRuns;
    protected long seed = TestDriverDefaultValues.seed;// For the random number generators

    protected String resultOutputDir = TestDriverDefaultValues.resultOutputDir;


    protected String sparqlEndpoint = null;
    protected String sparqlUpdateEndpoint = null;
    protected static String sparqlUpdateQueryParameter = TestDriverDefaultValues.updateQueryParameter;
    protected String defaultGraph = TestDriverDefaultValues.defaultGraph;

    protected String xmlResultFileExtension = TestDriverDefaultValues.resultFileExtension;
    protected String updateFile = null;
    protected boolean[] ignoreQueries;// Queries to ignore

    protected String type = "";
    protected boolean multithreading = false;
    protected int nrThreads;
    protected int timeout = TestDriverDefaultValues.timeoutInMs;
    protected boolean qualification = TestDriverDefaultValues.qualification;
    protected String qualificationFile = TestDriverDefaultValues.qualificationFile;

    protected SqlConnectionParameters sqlConnParams = new SqlConnectionParameters();

    protected String obdaFile;
    protected String owlFile;

    protected String bmStart;
    protected String bmEnd;

    protected String queryType;


    protected String stardogDb, stardogUrl, stardogLogin, stardogPassword;


    //    querymixes per measuring period
    protected int qmsPerPeriod = TestDriverDefaultValues.qmsPerPeriod;// Querymixes

    //    difference between min and max measurement period
    protected double percentDifference = TestDriverDefaultValues.percentDifference;// Difference

    //    the last nrofperiods periods are compared
    protected int nrOfPeriods = TestDriverDefaultValues.nrOfPeriods;// The last

    //    measure rampup yes/no
    protected boolean rampup = false;

    public TestDriver(String[] args) {
        System.out.print("\n* Reading Test Driver data... ");

        processProgramParameters(args);

        System.out.flush();

        System.out.println("done.");

        if ((sparqlEndpoint != null || sqlConnParams.dbServer != null || obdaFile != null) && !multithreading) {
            if (type.equals("sql")) {
                queryType = "sql";
                server = new SqlConnection(sqlConnParams, timeout);
            } else if (type.equals("obda")) {
                queryType = "obda";
                server = new ObdaConnection(owlFile, obdaFile, timeout);
            } else if (type.equals("sparql")) {
                queryType = "sparql";
                server = new SparqlConnection(sparqlEndpoint, sparqlUpdateEndpoint, defaultGraph, timeout);
            } else if (type.equals("stardog")) {
                queryType = "stardog";
                StardogConnectionParameters params = new StardogConnectionParameters(stardogLogin, stardogPassword, stardogDb, stardogUrl);
                server = new StardogConnection(params, timeout);
            }
        } else if (multithreading) {
            // TODO: add multithreading feature
        } else {
            UsageInfo.printUiUsage();
            System.exit(-1);
        }

        TestDriverShutdown tds = new TestDriverShutdown(this);
        Runtime.getRuntime().addShutdownHook(tds);
    }

    public void setUseCaseFile(String f) {
        this.usecaseFile = new File(f);
    }

    /**
     * Read which query mixes (directories) are used in the use case
     * @return
     */
    private List<String> getUseCaseQuerymixes() {
        List<String> files = new ArrayList<String>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(usecaseFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] querymixInfo = line.split("=");
                if (querymixInfo.length != 2) {
                    System.err.println("Invalid entry in use case file " + usecaseFile + ":\n");
                    System.err.println(line);
                    System.exit(-1);
                }
                if (querymixInfo[0].toLowerCase().equals("querymix"))
                    files.add(querymixInfo[1]);
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return files;
    }

    /*
      * Get the querymix ordering information
      */
    private List<Integer[]> getQuerymixRuns(List<String> querymixDirs) {
        List<Integer[]> runs = new ArrayList<Integer[]>();
        for (String querymixDir : querymixDirs) {
            runs.add(getQueryMixInfo(new File(querymixDir, "querymix.txt")));
        }
        return runs;
    }

    private List<Integer> getMaxQueryNrs(List<Integer[]> querymixRuns) {
        List<Integer> maxNrs = new ArrayList<Integer>();
        for (Integer[] run : querymixRuns) {
            Integer maxQueryNr = 0;
            for (int i = 0; i < run.length; i++) {
                if (run[i] != null && run[i] > maxQueryNr)
                    maxQueryNr = run[i];
            }
            maxNrs.add(maxQueryNr);
        }
        return maxNrs;
    }

    private List<boolean[]> getIgnoredQueries(List<String> querymixDirs, List<Integer> maxQueryNrs) {
        List<boolean[]> ignoredQueries = new ArrayList<boolean[]>();
        Iterator<String> queryMixDirIterator = querymixDirs.iterator();
        Iterator<Integer> maxQueryNrIterator = maxQueryNrs.iterator();
        while (queryMixDirIterator.hasNext()) {
            File ignoreFile = new File(queryMixDirIterator.next(), "ignoreQueries.txt");
            int maxQueryNr = maxQueryNrIterator.next();

            boolean[] ignoreQueries = new boolean[maxQueryNr];
            if (!ignoreFile.exists()) {
                for (int i = 0; i < ignoreQueries.length; i++) {
                    ignoreQueries[i] = false;
                }
            } else
                ignoreQueries = getIgnoreQueryInfo(ignoreFile, maxQueryNr);

            ignoredQueries.add(ignoreQueries);
        }
        return ignoredQueries;
    }

    /**
     * fills a list with the queries to be run in this query mix
     * @param querymixDirs a list of query mix directories (as stated in the use case file)
     * @param queryRuns    a list of query ID sequences that represent a query mix
     * @param maxQueryNrs  the max query ID for each query mix
     * @return
     */
    private List<Query[]> getQueries(List<String> querymixDirs,
                                     List<Integer[]> queryRuns, List<Integer> maxQueryNrs) {
        List<Query[]> allQueries = new ArrayList<Query[]>();

        Iterator<String> queryMixDirIterator = querymixDirs.iterator();
        Iterator<Integer[]> queryRunIterator = queryRuns.iterator();
        Iterator<Integer> maxQueryNrIterator = maxQueryNrs.iterator();

        while (queryMixDirIterator.hasNext()) {
//            queryRun is the sequence of IDs for the current query mix (e.g. 0 1 2 1 3)
            Integer[] queryRun = queryRunIterator.next();

//            queryDir is the directory for the queries in the current query mix
            String queryDir = queryMixDirIterator.next();

//            maxQueryNr is the highest ID in the current query mix (e.g. 3)
            Integer maxQueryNr = maxQueryNrIterator.next();

//            initialise an array for the correct number of queries
            Query[] queries = new Query[maxQueryNr];

            System.out.println("* Initialising queries...");

            for (int i = 0; i < queryRun.length; i++) {
                if (queryRun[i] != null) {
                    Integer qnr = queryRun[i];
                    if (queries[qnr - 1] == null) {
                        File queryFile = getQueryFile(queryDir, qnr);
                        queries[qnr - 1] = new Query(queryFile);
                    }
                }
            }
            allQueries.add(queries);
            System.out.println("* Done initialising all queries. Starting query execution...\n");

        }
        return allQueries;
    }


    /*
    * Read in query mixes / queries
    */
    public void init() {
        bmStart = Util.getTimeStamp();

        Query[] queries = null;
        Integer[] queryRun = null;

        List<String> querymixDirs = getUseCaseQuerymixes();

        List<Integer[]> queryRuns = getQuerymixRuns(querymixDirs);

        List<Integer> maxQueryPerRun = getMaxQueryNrs(queryRuns);

        List<boolean[]> ignoreQueries = getIgnoredQueries(querymixDirs, maxQueryPerRun);

        List<Query[]> queriesOfQuerymixes = getQueries(querymixDirs, queryRuns, maxQueryPerRun);

        queries = setupQueries(maxQueryPerRun, queriesOfQuerymixes);

        this.ignoreQueries = setupIgnoreQueries(maxQueryPerRun, ignoreQueries);

        queryRun = setupQueryRun(maxQueryPerRun, queryRuns);

        queryMix = new QueryMix(queries, queryRun);

    }

    /**
     * sets up queries in the query mix
     * @param maxQueryPerRun
     * @param queriesOfQuerymixes
     * @return
     */
    private Query[] setupQueries(List<Integer> maxQueryPerRun,
                                 List<Query[]> queriesOfQuerymixes) {
        Query[] queries;
        int nrOfQueries = 0;

        for (int i : maxQueryPerRun)
            nrOfQueries += i;
        queries = new Query[nrOfQueries];

        for (int i = 0; i < queries.length; i++) {
            queries[i] = null;
        }

        int queryOffset = 0;

        Iterator<Integer> maxQueryNrIterator = maxQueryPerRun.iterator();
        Iterator<Query[]> queriesIterator = queriesOfQuerymixes.iterator();
        while (queriesIterator.hasNext()) {
            Query[] qs = queriesIterator.next();
            int queryIndex = 0;
            for (Query query : qs) {
                queries[queryOffset + queryIndex] = query;
                queryIndex++;
            }

            queryOffset += maxQueryNrIterator.next();
        }
        return queries;
    }

    /**
     * Helper function to find the correct file containing a query
     * Had to add this in order to deal with queries which have more complex
     * file names than just "queryx.txt"
     * @param queryDir the directory containing the queries
     * @param qnr      the query number
     * @return the query file
     */
    private File getQueryFile(String queryDir, Integer qnr) {
        File dir = new File(queryDir);
        String queryFileId = "query" + qnr + "_";
        for (File f : dir.listFiles()) {
            if (f.getName().contains(queryFileId)) {
                return f;
            }
        }
        return null;
    }

    /**
     * reads the file which contains the file path of the query mix
     * @param file
     * @return
     */
    private Integer[] getQueryMixInfo(File file) {
        System.out.println("* Reading query mix file: " + file.getAbsolutePath());
        ArrayList<Integer> qm = new ArrayList<Integer>();

        try {
            BufferedReader qmReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file)));

            StringBuffer data = new StringBuffer();
            String line = null;
            while ((line = qmReader.readLine()) != null) {
                if (!line.equals("")) {
                    data.append(line);
                    data.append(" ");
                }
            }

            StringTokenizer st = new StringTokenizer(data.toString());
            while (st.hasMoreTokens()) {
                qm.add(Integer.parseInt(st.nextToken()));
            }
            qmReader.close();
        } catch (IOException e) {
            System.err.println("Error processing query mix file: " + file);
            System.exit(-1);
        }
        return qm.toArray(new Integer[1]);
    }

    /**
     * reads the file that states which queries in the query mix are ignored
     * @param ignoreFile     the file with ignored query numbers
     * @param maxQueryNumber max number of queries
     * @return boolean array with 1 for "ignore query"
     */
    private boolean[] getIgnoreQueryInfo(File ignoreFile, int maxQueryNumber) {
        System.out.println("Reading query ignore file: " + ignoreFile);
        boolean[] ignoreQueries = new boolean[maxQueryNumber];

        if (ignoreFile.exists()) {
            for (int i = 0; i < maxQueryNumber; i++)
                ignoreQueries[i] = false;

            try {
                BufferedReader qmReader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(ignoreFile)));

                StringBuffer data = new StringBuffer();
                String line = null;
                while ((line = qmReader.readLine()) != null) {
                    if (!line.equals("")) {
                        data.append(line);
                        data.append(" ");
                    }
                }

                StringTokenizer st = new StringTokenizer(data.toString());
                while (st.hasMoreTokens()) {
                    Integer queryNr = Integer.parseInt(st.nextToken());
                    if (queryNr > 0 && queryNr <= maxQueryNumber)
                        ignoreQueries[queryNr - 1] = true;
                }
                qmReader.close();
            } catch (IOException e) {
                System.err.println("Error processing query ignore file: " + ignoreFile);
                System.exit(-1);
            }
        }
        return ignoreQueries;
    }

    /**
     * runs the actual benchmark
     */
    public void run() {
        int qmsPerPeriod = TestDriverDefaultValues.qmsPerPeriod;
        int qmsCounter = 0;
        int periodCounter = 0;
        double periodRuntime = 0;
        BufferedWriter measurementFile = null;
        try {
            measurementFile = new BufferedWriter(new FileWriter("steadystate.tsv"));
        } catch (IOException e) {
            System.err.println("Could not create file steadystate.tsv!");
            System.exit(-1);
        }

        for (int nrRun = -warmups; nrRun < nrRuns; nrRun++) {
            long startTime = System.currentTimeMillis();
            queryMix.setRun(nrRun);
            while (queryMix.hasNext()) {
                Query nextQuery = queryMix.getNext();

                // Don't run update queries on warm-up
                if (nrRun < 0 && nextQuery.getQueryType() == Query.UPDATE_TYPE) {
                    queryMix.setCurrent(0, -1.0);
                    continue;
                }

                if (ignoreQueries[nextQuery.getNr() - 1])
                    queryMix.setCurrent(0, -1.0);
                else {
                    System.out.println("  Executing query " + nextQuery.getNr());
                    server.executeQuery(nextQuery, nextQuery.getQueryType());
                }
            }

            // Ignore warm-up measures
            if (nrRun >= 0) {
                qmsCounter++;
                periodRuntime += queryMix.getQueryMixRuntime();
            }

            // Write out period data
            if (qmsCounter == qmsPerPeriod) {
                periodCounter++;
                try {
                    measurementFile.append(periodCounter + "\t" + periodRuntime + "\n");
                    measurementFile.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                periodRuntime = 0;
                qmsCounter = 0;
            }

            System.out.println(nrRun
                    + ": "
                    + String.format(Locale.US, "%.2f", queryMix
                    .getQueryMixRuntime() * 1000) + "ms, total: "
                    + (System.currentTimeMillis() - startTime) + "ms");
            queryMix.finishRun();
        }
//        logger.log(Level.ALL, printResults(true));

        try {
            bmEnd = Util.getTimeStamp();
            File resultOut = new File(resultOutputDir);
            resultOut.mkdirs();
            String timeStampedResultFile = resultOut.getAbsolutePath() + "/" + bmStart + "_" + xmlResultFileExtension;
            FileWriter resultWriter = new FileWriter(timeStampedResultFile);
            resultWriter.append(printXMLResults(true));
            resultWriter.flush();
            resultWriter.close();
            measurementFile.close();

            System.out.println("\n* Benchmark completed. Result saved to " + timeStampedResultFile + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * TODO: tidy
     * Get Result String
     * @param all
     */
    public String printResults(boolean all) {
        StringBuffer sb = new StringBuffer(100);
        double singleMultiRatio = 0.0;

//		sb.append("Scale factor:           " + parameterPool.getScalefactor()
//				+ "\n");
        sb.append("Number of warmup runs:  " + warmups + "\n");
        if (multithreading)
            sb.append("Number of clients:      " + nrThreads + "\n");
        sb.append("Seed:                   " + seed + "\n");
        sb.append("Number of query mix runs (without warmups): "
                + queryMix.getQueryMixRuns() + " times\n");
        sb.append("min/max Querymix runtime: "
                + String.format(Locale.US, "%.4fs", queryMix
                .getMinQueryMixRuntime())
                + " / "
                + String.format(Locale.US, "%.4fs", queryMix
                .getMaxQueryMixRuntime()) + "\n");
        if (multithreading) {
            sb.append("Total runtime (sum):    "
                    + String.format(Locale.US, "%.3f", queryMix
                    .getTotalRuntime()) + " seconds\n");
            sb.append("Total actual runtime:   "
                    + String.format(Locale.US, "%.3f", queryMix
                    .getMultiThreadRuntime()) + " seconds\n");
            singleMultiRatio = queryMix.getTotalRuntime()
                    / queryMix.getMultiThreadRuntime();
        } else
            sb.append("Total runtime:          "
                    + String.format(Locale.US, "%.3f", queryMix
                    .getTotalRuntime()) + " seconds\n");
        if (multithreading)
            sb.append("QMpH:                   "
                    + String.format(Locale.US, "%.2f", queryMix
                    .getMultiThreadQmpH()) + " query mixes per hour\n");
        else
            sb.append("QMpH:                   "
                    + String.format(Locale.US, "%.2f", queryMix.getQmph())
                    + " query mixes per hour\n");
        sb.append("CQET:                   "
                + String.format(Locale.US, "%.5f", queryMix.getCQET())
                + " seconds average runtime of query mix\n");
        sb.append("CQET (geom.):           "
                + String.format(Locale.US, "%.5f", queryMix
                .getQueryMixGeometricMean())
                + " seconds geometric mean runtime of query mix\n");

        if (all) {
            sb.append("\n");
            // Print per query statistics
            Query[] queries = (Query[]) queryMix.getQueries();
            double[] qmin = queryMix.getQmin();
            double[] qmax = queryMix.getQmax();
            double[] qavga = queryMix.getAqet();// Arithmetic mean
            double[] avgResults = queryMix.getAvgResults();
            double[] qavgg = queryMix.getGeoMean();
            int[] qTimeout = queryMix.getTimeoutsPerQuery();
            int[] minResults = queryMix.getMinResults();
            int[] maxResults = queryMix.getMaxResults();
            int[] nrq = queryMix.getRunsPerQuery();
            for (int i = 0; i < qmin.length; i++) {
                if (queries[i] != null) {
                    sb.append("Metrics for Query:      " + (i + 1) + "\n");
                    sb.append("Count:                  " + nrq[i]
                            + " times executed in whole run\n");
                    sb.append("AQET:                   "
                            + String.format(Locale.US, "%.6f", qavga[i])
                            + " seconds (arithmetic mean)\n");
                    sb.append("AQET(geom.):            "
                            + String.format(Locale.US, "%.6f", qavgg[i])
                            + " seconds (geometric mean)\n");
                    if (multithreading)
                        sb.append("QPS:                    "
                                + String.format(Locale.US, "%.2f",
                                singleMultiRatio / qavga[i])
                                + " Queries per second\n");
                    else
                        sb
                                .append("QPS:                    "
                                        + String.format(Locale.US, "%.2f",
                                        1 / qavga[i])
                                        + " Queries per second\n");
                    sb
                            .append("minQET/maxQET:          "
                                    + String
                                    .format(Locale.US, "%.8fs", qmin[i])
                                    + " / "
                                    + String
                                    .format(Locale.US, "%.8fs", qmax[i])
                                    + "\n");
                    if (queries[i].getQueryType() == Query.SELECT_TYPE) {
                        sb.append("Average result count:   "
                                + String.format(Locale.US, "%.2f",
                                avgResults[i]) + "\n");
                        sb.append("min/max result count:   " + minResults[i]
                                + " / " + maxResults[i] + "\n");
                    } else {
                        sb.append("Average result (Bytes): "
                                + String.format(Locale.US, "%.2f",
                                avgResults[i]) + "\n");
                        sb.append("min/max result (Bytes): " + minResults[i]
                                + " / " + maxResults[i] + "\n");
                    }
                    sb
                            .append("Number of timeouts:     " + qTimeout[i]
                                    + "\n\n");
                }
            }
        }

        return sb.toString();
    }


    /**
     * TODO: rewrite to create an XML DOM rather than a string
     * Get XML Result String
     * @param all
     * @return
     */
    public String printXMLResults(boolean all) {
        StringBuilder sb = new StringBuilder(100);
        double singleMultiRatio = 0.0;
        String queryLang = "sparql";
        if (type.equals("sql")) {
            queryLang = "sql";
        } else if (type.equals("obda")) {
            queryLang = "obda";
        }

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<bsbm");
        sb.append(" lang=\"");
        sb.append(queryLang);
        sb.append("\"");
        sb.append(" start=\"");
        sb.append(bmStart);
        sb.append("\" end=\"");
        sb.append(bmEnd);
        sb.append("\">\n");

        sb.append("  <querymix>\n");
//		sb.append("     <scalefactor>" + parameterPool.getScalefactor()
//				+ "</scalefactor>\n");
        sb.append("     <warmups>" + warmups + "</warmups>\n");
        if (multithreading)
            sb.append("     <nrthreads>" + nrThreads + "</nrthreads>\n");
        sb.append("     <seed>" + seed + "</seed>\n");
        sb.append("     <querymixruns>" + queryMix.getQueryMixRuns()
                + "</querymixruns>\n");
        sb.append("     <minquerymixruntime>"
                + String.format(Locale.US, "%.4f", queryMix
                .getMinQueryMixRuntime()) + "</minquerymixruntime>\n");
        sb.append("     <maxquerymixruntime>"
                + String.format(Locale.US, "%.4f", queryMix
                .getMaxQueryMixRuntime()) + "</maxquerymixruntime>\n");
        if (multithreading) {
            sb.append("     <totalruntime>"
                    + String.format(Locale.US, "%.3f", queryMix
                    .getTotalRuntime()) + "</totalruntime>\n");
            sb.append("     <actualtotalruntime>"
                    + String.format(Locale.US, "%.3f", queryMix
                    .getMultiThreadRuntime())
                    + "</actualtotalruntime>\n");
            singleMultiRatio = queryMix.getTotalRuntime()
                    / queryMix.getMultiThreadRuntime();
        } else {
            sb.append("     <totalruntime>"
                    + String.format(Locale.US, "%.3f", queryMix
                    .getTotalRuntime()) + "</totalruntime>\n");
            singleMultiRatio = 1;
        }

        if (multithreading)
            sb.append("     <qmph>"
                    + String.format(Locale.US, "%.2f", queryMix
                    .getMultiThreadQmpH()) + "</qmph>\n");
        else
            sb.append("     <qmph>"
                    + String.format(Locale.US, "%.2f", queryMix.getQmph())
                    + "</qmph>\n");
        sb.append("     <cqet>"
                + String.format(Locale.US, "%.5f", queryMix.getCQET())
                + "</cqet>\n");
        sb.append("     <cqetg>"
                + String.format(Locale.US, "%.5f", queryMix
                .getQueryMixGeometricMean()) + "</cqetg>\n");
        sb.append("  </querymix>\n");

        if (all) {
            sb.append("  <queries>\n");
            // Print per query statistics
            Query[] queries = (Query[]) queryMix.getQueries();
            double[] qmin = queryMix.getQmin();
            double[] qmax = queryMix.getQmax();
            double[] qavga = queryMix.getAqet();
            double[] avgResults = queryMix.getAvgResults();
            double[] qavgg = queryMix.getGeoMean();
            int[] qTimeout = queryMix.getTimeoutsPerQuery();
            int[] minResults = queryMix.getMinResults();
            int[] maxResults = queryMix.getMaxResults();
            int[] nrq = queryMix.getRunsPerQuery();
            for (int i = 0; i < qmin.length; i++) {
                if (queries[i] != null) {
                    sb.append("    <query nr=\"" + (i + 1) + "\">\n");
                    sb.append("      <querystring><![CDATA[\n");
                    sb.append(queries[i].getQueryString());
                    sb.append("\n       ]]>\n");
                    sb.append("      </querystring>\n");


                    sb.append("      <executecount>" + nrq[i]
                            + "</executecount>\n");
                    sb.append("      <aqet>"
                            + String.format(Locale.US, "%.6f", qavga[i])
                            + "</aqet>\n");
                    sb.append("      <aqetg>"
                            + String.format(Locale.US, "%.6f", qavgg[i])
                            + "</aqetg>\n");
                    sb.append("      <qps>"
                            + String.format(Locale.US, "%.2f", singleMultiRatio
                            / qavga[i]) + "</qps>\n");
                    sb.append("      <minqet>"
                            + String.format(Locale.US, "%.8f", qmin[i])
                            + "</minqet>\n");
                    sb.append("      <maxqet>"
                            + String.format(Locale.US, "%.8f", qmax[i])
                            + "</maxqet>\n");
                    sb.append("      <avgresults>"
                            + String.format(Locale.US, "%.2f", avgResults[i])
                            + "</avgresults>\n");
                    sb.append("      <minresults>" + minResults[i]
                            + "</minresults>\n");
                    sb.append("      <maxresults>" + maxResults[i]
                            + "</maxresults>\n");
                    sb.append("      <timeoutcount>" + qTimeout[i]
                            + "</timeoutcount>\n");
                    sb.append("    </query>\n");
                } else {
                    sb.append("    <query nr=\"" + (i + 1) + "\">\n");
                    sb.append("      <executecount>0</executecount>\n");
                    sb.append("      <aqet>0.0</aqet>\n");
                    sb.append("    </query>\n");
                }
            }
            sb.append("  </queries>\n");
        }
        sb.append("</bsbm>\n");
        return sb.toString();
    }

    /**
     * shuts down the testdriver connection
     */

    static class TestDriverShutdown extends Thread {
        TestDriver testdriver;

        TestDriverShutdown(TestDriver t) {
            this.testdriver = t;
        }

        @Override
        public void run() {
            try {
                if (testdriver.server != null)
                    testdriver.server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    * Combine the query sequences of different query mixes
    */
    private Integer[] setupQueryRun(List<Integer> maxQueryPerRun,
                                    List<Integer[]> queryRuns) {
        Integer[] queryRun;
        int nrOfQueriesInRun = 0;

        for (Integer[] qr : queryRuns)
            nrOfQueriesInRun += qr.length;
        queryRun = new Integer[nrOfQueriesInRun];

        int indexOffset = 0;
        int queryOffset = 0;

        Iterator<Integer> maxQueryNrIterator = maxQueryPerRun.iterator();
        Iterator<Integer[]> queryRunIterator = queryRuns.iterator();
        while (queryRunIterator.hasNext()) {
            Integer[] qr = queryRunIterator.next();
            int queryIndex = 0;
            for (Integer queryNr : qr) {
                queryRun[indexOffset + queryIndex] = queryNr + queryOffset;
                queryIndex++;
            }
            indexOffset += qr.length;
            queryOffset += maxQueryNrIterator.next();
        }
        return queryRun;
    }

    /*
      * Combine ignored queries of different query mixes
      */
    private boolean[] setupIgnoreQueries(List<Integer> maxQueryPerRun,
                                         List<boolean[]> ignoreQueriesOfQuerymixes) {
        boolean[] ignoreQueries;
        int nrOfQueries = 0;

        for (int i : maxQueryPerRun)
            nrOfQueries += i;
        ignoreQueries = new boolean[nrOfQueries];

        int queryOffset = 0;

        Iterator<Integer> maxQueryNrIterator = maxQueryPerRun.iterator();
        Iterator<boolean[]> ignoreQueriesIterator = ignoreQueriesOfQuerymixes
                .iterator();
        while (ignoreQueriesIterator.hasNext()) {
            boolean[] iqs = ignoreQueriesIterator.next();
            int queryIndex = 0;
            for (boolean igQuery : iqs) {
                ignoreQueries[queryOffset + queryIndex] = igQuery;
                queryIndex++;
            }

            queryOffset += maxQueryNrIterator.next();
        }
        return ignoreQueries;
    }

    /*
    * Process the program parameters typed on the command line.
    */
    protected void processProgramParameters(String[] args) {
        int i = 0;
        while (i < args.length) {
            try {
                if (args[i].equals("-runs")) {
                    nrRuns = Integer.parseInt(args[i++ + 1]);
                } else if (args[i].equals("-w")) {
                    warmups = Integer.parseInt(args[i++ + 1]);
                } else if (args[i].equals("-results")) {
                    resultOutputDir = args[i++ + 1];
                } else if (args[i].equals("-dg")) {
                    defaultGraph = args[i++ + 1];
                } else if (args[i].equals("-type")) {
                    type = args[i++ + 1];
                } else if (args[i].equals("-mt")) {
                    if (rampup) throw new Exception("Incompatible options: -mt and -rampup");
                    multithreading = true;
                    nrThreads = Integer.parseInt(args[i++ + 1]);
                } else if (args[i].equals("-seed")) {
                    seed = Long.parseLong(args[i++ + 1]);
                } else if (args[i].equals("-t")) {
                    timeout = Integer.parseInt(args[i++ + 1]);
                } else if (args[i].equals("-dbdriver")) {
                    sqlConnParams.dbDriver = args[i++ + 1];
                } else if (args[i].equals("-qf")) {
                    qualificationFile = args[i++ + 1];
                } else if (args[i].equals("-dbserver")) {
                    sqlConnParams.dbServer = args[i++ + 1];
                } else if (args[i].equals("-dblogin")) {
                    sqlConnParams.dbLogin = args[i++ + 1];
                } else if (args[i].equals("-dbpw")) {
                    sqlConnParams.dbPassword = args[i++ + 1];
                } else if (args[i].equals("-obdafile")) {
                    obdaFile = args[i++ + 1];
                } else if (args[i].equals("-owlfile")) {
                    owlFile = args[i++ + 1];
                } else if (args[i].equals("-q")) {
                    qualification = true;
                    nrRuns = 15;
                } else if (args[i].equals("-rampup")) {
                    if (multithreading)
                        throw new Exception(
                                "Incompatible options: -mt and -rampup");
                    rampup = true;
                } else if (args[i].equals("-u")) {
                    sparqlUpdateEndpoint = args[i++ + 1];
                } else if (args[i].equals("-udataset")) {
                    updateFile = args[i++ + 1];
                } else if (args[i].equals("-ucf")) {
                    usecaseFile = new File(args[i++ + 1]);
                } else if (args[i].equals("-uqp")) {
                    sparqlUpdateQueryParameter = args[i++ + 1];
                } else if (args[i].equals("-sparqlendpoint")) {
                    sparqlEndpoint = args[i++ + 1];
                } else if (args[i].equals("-stardogdb")) {
                    stardogDb = args[i++ + 1];
                } else if (args[i].equals("-stardogurl")) {
                    stardogUrl = args[i++ + 1];
                } else if (args[i].equals("-stardoglogin")) {
                    stardogLogin = args[i++ + 1];
                } else if (args[i].equals("-stardogpw")) {
                    stardogPassword = args[i++ + 1];
                } else {
                    if (!args[i].equals("-help"))
                        System.err.println("Unknown parameter: " + args[i]);
                    UsageInfo.printUiUsage();
                    System.exit(-1);
                }

                i++;

            } catch (Exception e) {
                System.err.println("Invalid arguments:\n");
                e.printStackTrace();
                UsageInfo.printUiUsage();
                System.exit(-1);
            }
        }
    }


}
