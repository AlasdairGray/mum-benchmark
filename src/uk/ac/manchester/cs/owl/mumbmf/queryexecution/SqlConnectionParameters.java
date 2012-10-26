package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

/**
 * Created by
 * User: Samantha Bail
 * Date: 06/09/2012
 * Time: 23:24
 * The University of Manchester
 */


public class SqlConnectionParameters {

    public String dbServer;
    public String dbLogin;
    public String dbPassword;
    public String dbDriver = "com.mysql.jdbc.Driver";

    public SqlConnectionParameters() {

    }


    public SqlConnectionParameters(String dbServer, String dbLogin, String dbPassword, String dbDriver) {
        this.dbServer = dbServer;
        this.dbLogin = dbLogin;
        this.dbPassword = dbPassword;
        this.dbDriver = dbDriver;
    }
}
