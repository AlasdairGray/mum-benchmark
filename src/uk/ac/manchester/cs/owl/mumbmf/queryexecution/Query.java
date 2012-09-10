package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

import java.io.*;
import java.util.Vector;

/**
 * Created by
 * User: Samantha Bail
 * Date: 19/06/2012
 * Time: 22:07
 * The University of Manchester
 */


public class Query {

    private String queryString;
    private int nr;
    private Byte queryType;
    private QueryMix queryMix;

    // query type constants
    public static final byte SELECT_TYPE = 1;
    public static final byte DESCRIBE_TYPE = 2;
    public static final byte CONSTRUCT_TYPE = 3;
    public static final byte UPDATE_TYPE = 4;


    public Query(File queryFile) {

        BufferedReader queryReader = null;
        System.out.println("  Reading query file " + queryFile);
        try {
            queryReader = new BufferedReader(new InputStreamReader(new FileInputStream(queryFile)));
            StringBuilder sb = new StringBuilder();

            while (true) {
                String line = queryReader.readLine();
                if (line == null)
                    break;
                else {
                    sb.append(line);
                    sb.append("\n");
                }
            }
            queryString = sb.toString();
//            TODO: extend to allow other query types - for now it's only select
            queryType = SELECT_TYPE;// default: Select query

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        } finally {
            try {
                if (queryReader != null)
                    queryReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("  Done initialising " + queryFile.getName());

    }


    /*
    * returns a String of the Query
    */
    public String getQueryString() {
        return queryString;
    }


    /*


    /*
      * get the byte type representation of this query type string
      */
    private byte getQueryType(String stringType) {
        if (stringType.toLowerCase().equals("select"))
            return SELECT_TYPE;
        else if (stringType.toLowerCase().equals("describe"))
            return DESCRIBE_TYPE;
        else if (stringType.toLowerCase().equals("construct"))
            return CONSTRUCT_TYPE;
        else if (stringType.toLowerCase().equals("update"))
            return UPDATE_TYPE;
        else
            return 0;
    }

    public int getNr() {
        return nr;
    }

    public void setNr(int nr) {
        this.nr = nr;
    }

    public Byte getQueryType() {
        return queryType;
    }

    public QueryMix getQueryMix() {
        return queryMix;
    }

    public void setQueryMix(QueryMix queryMix) {
        this.queryMix = queryMix;
    }


}
