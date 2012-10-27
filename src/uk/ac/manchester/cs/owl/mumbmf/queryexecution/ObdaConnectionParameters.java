package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/10/2012
 * Time: 16:56
 * The University of Manchester
 */


public class ObdaConnectionParameters {

    public String obdaFile = "";
    public String owlFile = "";

    public ObdaConnectionParameters() {
    }

    public ObdaConnectionParameters(String obdaFile, String owlFile) {
        this.obdaFile = obdaFile;
        this.owlFile = owlFile;
    }
}
