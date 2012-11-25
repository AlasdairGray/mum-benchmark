package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;

public class SparqlQuery {
    HttpURLConnection conn;
    Long start;
    Long end;
    String queryString;

    public SparqlQuery(String sparqlEndpoint, String query, byte queryType, String defaultGraph, int timeout) {
        String urlString = null;
        try {
            queryString = query;
            char delim = sparqlEndpoint.indexOf('?') == -1 ? '?' : '&';
            if (queryType == Query.UPDATE_TYPE)
                urlString = sparqlEndpoint;
            else {
                if (!addQueryLnParameter(sparqlEndpoint)) {
                    urlString = sparqlEndpoint + delim + "query=" + URLEncoder.encode(query, "UTF-8");
                } else {
                    urlString = sparqlEndpoint + "query?queryLn=SPARQL" + "&query=" + URLEncoder.encode(query, "UTF-8");
                    System.out.println(urlString);
                }
                delim = '&';
                if (defaultGraph != null)
                    urlString += delim + "default-graph-uri=" + defaultGraph;
            }

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            configureConnection(query, queryType, timeout, defaultGraph);
        } catch (UnsupportedEncodingException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(-1);
        } catch (MalformedURLException e) {
            System.err.println(e.toString() + " for URL: " + urlString);
            System.err.println(sparqlEndpoint);
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private boolean addQueryLnParameter(String sparqlEndpoint) {
        if (sparqlEndpoint.contains("openrdf")) {
            return true;
        }
        return false;
    }

    private void configureConnection(String query, byte queryType, int timeout, String defaultGraph)
            throws IOException {
        if (queryType == Query.UPDATE_TYPE)
            conn.setRequestMethod("POST");
        else
            conn.setRequestMethod("GET");
        conn.setDefaultUseCaches(false);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setReadTimeout(timeout);
        if (queryType == Query.DESCRIBE_TYPE || queryType == Query.CONSTRUCT_TYPE)
            conn.setRequestProperty("Accept", "application/rdf+xml");
        else
            conn.setRequestProperty("Accept", "application/sparql-results+xml");

        if (queryType == Query.UPDATE_TYPE) {
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStream out = conn.getOutputStream();
            String queryParamName = TestDriver.sparqlUpdateQueryParameter + "=";
            out.write(queryParamName.getBytes());
            out.write(URLEncoder.encode(query, "UTF-8").getBytes());
            if (defaultGraph != null) {
                out.write("&default-graph-uri=".getBytes());
                out.write(defaultGraph.getBytes());
            }
            out.flush();
        }
    }

    public InputStream exec() {
        try {
            conn.connect();

        } catch (IOException e) {
            System.err.println("Could not connect to SPARQL Service.");
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            start = System.nanoTime();
            int rc = conn.getResponseCode();
//            System.out.println("RESPONSE CODE: " + rc);
            if (rc < 200 || rc >= 300) {
                System.err.println("Query execution: Received error code " + rc + " from server");
                System.err.println("Error message: " + conn.getResponseMessage() + "\n\nFor query: \n");
                System.err.println(queryString + "\n");
            }
            return conn.getInputStream();
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.err.println("Query execution error:");
            e.printStackTrace();
            System.exit(-1);
            return null;
        }

    }

    protected double getExecutionTimeInSeconds() {
        end = System.nanoTime();
        Long interval = end - start;
        Thread.yield();
        return interval.doubleValue() / 1000000000;
    }

    public void close() {
        conn.disconnect();
    }
}
