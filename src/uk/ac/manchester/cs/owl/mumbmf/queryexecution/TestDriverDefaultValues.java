package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

import java.io.File;

public class TestDriverDefaultValues {
    // how many Query mixes are run for warm up    DEFAULT: 50
    public static int warmups = 50;

    // how many query mix runs are measured   DEFAULT: 100
    public static int nrRuns = 100;

    // how long to run a query  DEFAULT: 1min
    public static int runTime = 1;

    // seed for randomisation
    public static long seed = 808080L;

    // default RDF graph
    public static String defaultGraph = null;

    // file extension for the result files
    public static String resultFileExtension = "benchmark_result.xml";

    // default timeout
    public static int timeoutInMs = 0;

    // default db driver class
    public static String driverClassName = "com.mysql.jdbc.Driver";

    // default fetch size
    public static int fetchSize = 100;

// TODO: are these needed?
    public static File usecaseFile = new File("");
    public static boolean qualification = false;
    public static String qualificationFile = "run.qual";
    public static int qmsPerPeriod = 50;
    public static double percentDifference = 0.0;
    public static int nrOfPeriods = 5;
    public static String updateQueryParameter = "update";


}
