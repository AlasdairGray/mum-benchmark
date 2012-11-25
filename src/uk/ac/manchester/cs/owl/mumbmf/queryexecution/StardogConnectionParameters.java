package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

/**
 * Created by
 * User: Samantha Bail
 * Date: 24/11/2012
 * Time: 22:53
 * The University of Manchester
 */


public class StardogConnectionParameters {

    public String login = "admin";
    public String password = "admin";
    public String dbName = "";
    public String dbUrl = "";

    public StardogConnectionParameters() {

    }

    public StardogConnectionParameters(String login, String password, String dbName, String dbUrl) {
        this.login = login;
        this.password = password;
        this.dbName = dbName;
        this.dbUrl = dbUrl;
    }

}
