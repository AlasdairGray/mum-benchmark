package uk.ac.manchester.cs.owl.mumbmf.queryexecution;

import it.unibz.krdb.obda.gui.swing.exception.InvalidMappingException;
import it.unibz.krdb.obda.gui.swing.exception.InvalidPredicateDeclarationException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.*;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlapi3.OWLConnection;
import it.unibz.krdb.obda.owlapi3.OWLResultSet;
import it.unibz.krdb.obda.owlapi3.OWLStatement;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import java.io.File;
import java.io.IOException;


/**
 * Created by
 * User: Samantha Bail
 * Date: 27/07/2012
 * Time: 14:34
 * The University of Manchester
 */


public class ObdaConnection implements ServerConnection {

    private OWLStatement statement;
    private OWLConnection conn;

    private int timeout;

    private OWLOntology ontology;
    private OWLResultSet rs;
    private QuestOWL reasoner;

    //initialize the data factory:
    OBDADataFactory obdaDataFactory = OBDADataFactoryImpl.getInstance();

    private static Logger logger = Logger.getLogger(ObdaConnection.class);

    /**
     * Constructor
     * @param owlFile  the OWL file containting the ontology
     * @param obdaFile the OBDA file containing mappings
     * @param timeout  query timeout
     */
    public ObdaConnection(String owlFile, String obdaFile, int timeout) {
        this.timeout = timeout;

        // Loading the OWL file
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            ontology = manager.loadOntologyFromOntologyDocument((new File(owlFile)));
        } catch (OWLOntologyCreationException ex) {
            java.util.logging.Logger.getLogger(ObdaConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        // Loading the OBDA data
        OBDAModel obdaModel = obdaDataFactory.getOBDAModel();

        ModelIOManager ioManager = new ModelIOManager(obdaModel);

        try {
            ioManager.load(obdaFile);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ObdaConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InvalidPredicateDeclarationException ex) {
            java.util.logging.Logger.getLogger(ObdaConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InvalidMappingException ex) {
            java.util.logging.Logger.getLogger(ObdaConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        // Creating a new instance of the Quest reasoner
        QuestOWLFactory factory = new QuestOWLFactory();
        factory.setOBDAController(obdaModel);

        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        factory.setPreferenceHolder(p);

        reasoner = (QuestOWL) factory.createReasoner(ontology, new SimpleConfiguration());

        try {
            conn = reasoner.getConnection();
            statement = conn.createStatement();

        } catch (OWLException e) {
            java.util.logging.Logger.getLogger(ObdaConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
        } catch (OBDAException e) {
            java.util.logging.Logger.getLogger(ObdaConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
        }
    }


    /**
     * Execute Query with Query Object
     * @param query     the query
     * @param queryType the query type
     */
    public void executeQuery(Query query, byte queryType) {
        executeQuery(query.getQueryString(), queryType, query.getNr(), query.getQueryMix());
    }

    /**
     * executes a query from the query mix
     * @param queryString the query string
     * @param queryType   the query type
     * @param queryNr     counter for the query
     * @param queryMix    the query mix that contains the query
     */
    private void executeQuery(String queryString, byte queryType, int queryNr, QueryMix queryMix) {
        double timeInSeconds;

        try {
            long start = System.nanoTime();
            OWLResultSet results = statement.execute(queryString);
            long stop = System.nanoTime();
            Long interval = stop - start;
            timeInSeconds = interval.doubleValue() / 1000000000;

            int resultCount = 0;
//             count the results
            while (results.nextRow())
                resultCount++;

            int queryMixRun = queryMix.getRun() + 1;

            if (logger.isEnabledFor(Level.ALL) && queryType != 3 && queryMixRun > 0)
                logResultInfo(queryNr, queryMixRun, timeInSeconds,
                        queryString, queryType, 0,
                        resultCount);

//            update the query mix to keep track of results
            queryMix.setCurrent(resultCount, timeInSeconds);
            results.close();

        } catch (OWLException e) {
            System.err.println("\n\nError for Query " + queryNr + ":\n\n" + queryString);
            System.exit(-1);
        }
    }


    private void logResultInfo(int queryNr, int queryMixRun, double timeInSeconds,
                               String queryString, byte queryType, int resultSizeInBytes,
                               int resultCount) {
        StringBuffer sb = new StringBuffer(1000);
        sb.append("\n\n\tQuery " + queryNr + " of run " + queryMixRun + " has been executed ");
        sb.append("in " + String.format("%.6f", timeInSeconds) + " seconds.\n");
        sb.append("\n\tQuery string:\n\n");
        sb.append(queryString);
        sb.append("\n\n");

        // Log the results
        if (queryType == Query.DESCRIBE_TYPE)
            sb.append("\tQuery(Describe) result (" + resultSizeInBytes + " Bytes): \n\n");
        else
            sb.append("\tQuery results (" + resultCount + " results): \n\n");
        logger.log(Level.ALL, sb.toString());
    }


    /**
     * Executes a query and returns the results
     * @param query the query
     * @return an OWL result set
     */
    public OWLResultSet executeSimpleQuery(String query) {
        try {
            rs = statement.execute(query);
            return rs;
        } catch (OWLException ex) {
            java.util.logging.Logger.getLogger(ObdaConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return null;
    }


    /**
     * closes the connection etc.
     */
    public void close() {
        try {
            statement.close();
            conn.close();
            reasoner.dispose();
        } catch (OWLException e) {
            e.printStackTrace();
        }
    }


}
