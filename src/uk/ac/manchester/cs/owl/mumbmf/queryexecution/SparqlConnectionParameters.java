package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/10/2012
 * Time: 16:55
 * The University of Manchester
 */


public class SparqlConnectionParameters {

    public String sparqlEndpoint = "";

    public SparqlConnectionParameters() {

    }

    public SparqlConnectionParameters(String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
    }
}
