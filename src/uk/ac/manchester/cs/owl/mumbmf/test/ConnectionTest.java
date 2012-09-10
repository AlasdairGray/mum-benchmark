package uk.ac.manchester.cs.owl.mumbmf.test;

import it.unibz.krdb.obda.owlapi3.OWLResultSet;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.*;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.MultiQueryGenerator;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.QueryGenerator;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.SparqlQueryGenerator;
import uk.ac.manchester.cs.owl.mumbmf.querygenerator.SqlQueryGenerator;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/07/2012
 * Time: 15:19
 * The University of Manchester
 */


public class ConnectionTest {


    /**
     * SPARQL:
     * Simple query:  ok
     * Generate queries:
     * Run benchmark:
     * <p/>
     * <p/>
     * SQL:
     * Simple query:  ok
     * Generate queries:
     * Run benchmark:
     * <p/>
     * Quest:
     * Simple query:
     * Generate queries:
     * Run benchmark:
     * @param args
     * @throws Exception
     */


    //    static String sparqlEndpoint = "http://130.88.192.86:8080/sparql/";
    static String sparqlEndpoint = "http://130.88.192.86:8890/sparql/";

    //    static String sparqlQueryFile = "fishqueries_sandra/SPARQL-test.xml";
    static String sparqlQueryFile = "fishmark_queries/sparql.xml";
    static String sqlQueryFile = "fishmark_queries/sql.xml";
    static String questQueryFile = "fishmark_queries/OBDA.xml";

    static String sparqlOutputDir = "testqueries/sparql/";
    static String sqlOutputDir = "testqueries/sql/";
    static String questOutputDir = "testqueries/obda/";
    static String testqueriesOutput = "testqueries";

    static String userName = "fishdelish";
    static String password = "fishdelish";
    static String dbName = "jdbc:mysql://130.88.192.86/fbapp";
    static String dbClass = "com.mysql.jdbc.Driver";

    static SqlConnectionParameters sqlconnparams = new SqlConnectionParameters(dbName, userName, password, dbClass);

    //Quest
    static String owl = "OBDA/fishdelish.owl";
    static String obda = "OBDA/fishdelish.obda";

    static String[] sparqlparams = {sparqlEndpoint, "-runs", "1", "-w", "1"};
    static String[] sqlparams = {"-sql", "-db", dbName, "-user", userName, "-password", password, "-runs", "1", "-w", "1"};
    static String[] obdaparams = {"-obda", "-obdafile", obda, "-owlfile", owl, "-runs", "1", "-w", "1"};


    public static void main(String[] args) throws OWLException, JDOMException, IOException {

//        runTestSQLQuery();
//        runTestSPARQLQuery();

        runQueryGenerationTest();
    }


    private static void runMultiQueryGenerationTest() {
//        List<String> files = new ArrayList<String>();
//        files.add(sqlQueryFile);
//        files.add(sparqlQueryFile);
//        files.add(questQueryFile);
//
//        QueryGenerator generator = new SqlQueryGenerator(dbName, userName, password, dbClass, sqlOutputDir);
//
//        MultiQueryGenerator multiGen = new MultiQueryGenerator(files, generator, testqueriesOutput);
//        multiGen.generateMultipleQueryTypes();

    }

    public static void runSQLBenchmark() {
        TestDriver diver = new TestDriver(sqlparams);
        diver.setUseCaseFile("fishbase_usecases/default/sql.txt");
        diver.init();
        diver.run();
    }


    public static void runOBDABenchmark() {

        TestDriver diver = new TestDriver(obdaparams);
        diver.setUseCaseFile("fishbase_usecases/default/obda.txt");
        diver.init();
        diver.run();
    }

    public static void runSPARQLBenchmark() {
        TestDriver diver = new TestDriver(sparqlparams);
        diver.setUseCaseFile("fishbase_usecases/default/sparql.txt");
        diver.init();
        diver.run();

    }


    public static void runQueryGenerationTest() {

        QueryGenerator generator = new SparqlQueryGenerator(sparqlEndpoint, sparqlOutputDir);
        generator.generateQueries(sparqlQueryFile, "", "");
//
//        QueryGenerator generator = new SqlQueryGenerator(sqlconnparams, sqlOutputDir);
//        generator.generateQueries(sqlQueryFile);
//
//        QueryGenerator generator = new OBDAQueryGenerator(owl, obda, questOutputDir);
//        generator.generateQueries(questQueryFile);

    }

    /**
     * test sparql querying with FishSPARQLConnection using a simple query
     * works
     * @throws org.jdom.JDOMException
     * @throws java.io.IOException
     */
    public static void runTestSPARQLQuery() throws JDOMException, IOException {
        String queryString = "\n" +
                "                PREFIX fd: <http://fishdelish.cs.man.ac.uk/rdf/vocab/resource/>\n" +
                "                SELECT ?order ?family ?genus ?species ?occurence ?fbname ?name ?dangerous \n" +
                "                WHERE {\n" +
                "\t\t\t\t\t?nameID fd:comnames_ComName ?name .\n" +
                "\t\t\t\t\t?nameID fd:comnames_C_Code ?ccode . \n" +
                "\t\t\t\t\t?nameID fd:comnames_SpecCode ?x.\n" +
                "\t\t\t\t\t?x fd:species_Genus ?genus .\n" +
                "\t\t\t\t\t?x fd:species_Species ?species .\n" +
                "\t\t\t\t\t?x fd:species_Dangerous ?dangerous .\n" +
                "\t\t\t\t\t?x fd:species_DemersPelag \"reef-associated\" .\n" +
                "\t\t\t\t\tOPTIONAL {?x fd:species_FBname ?fbname }.\n" +
                "\t\t\t\t\t?x fd:species_FamCode ?f .\n" +
                "\t\t\t\t\t?f fd:families_Family  ?family .\n" +
                "\t\t\t\t\t?f  fd:families_Order ?order .\n" +
                "\t\t\t\t\t?c fd:country_SpecCode ?x.\n" +
                "\t\t\t\t\t?c fd:country_Status ?occurence .\n" +
                "\t\t\t\t\t?c fd:country_C_Code ?cf .\n" +
                "\t\t\t\t\t?cf fd:countref_PAESE \"Tahiti\" .   \n" +
                "                } LIMIT 20 ";


        SparqlConnection conn = new SparqlConnection(sparqlEndpoint, null, 0);

        org.jdom.Document doc = conn.executeSimpleQuery(queryString);
        System.out.println("Query executed.");

        DOMOutputter domOut = new org.jdom.output.DOMOutputter();

        NodeList results = domOut.output(doc).getElementsByTagName("result");
        for (int i = 0; i < results.getLength(); i++) {
            Node n = results.item(i);
            System.out.println(n.getTextContent());
        }
        System.out.println(results.getLength());

    }

    public static void runTestSQLQuery() {
        String query = "SELECT families.FamilyOrder , families.Family, species.Genus,species.Species,  country.Status, species.FBname, \n" +
                "             comnames.ComName, species.Dangerous\n" +
                "             FROM fbapp.families, fbapp.species, fbapp.comnames, fbapp.countref, fbapp.country\n" +
                "             WHERE countref.C_Code=country.C_Code \n" +
                "             AND country.SpecCode=species.SpecCode \n" +
                "             AND comnames.SpecCode=species.SpecCode\n" +
                "             AND species.FamCode = families.FamCode \n" +
                "             AND countref.PAESE=\"Tahiti\"\n" +
                "             AND species.DemersPelag=\"reef-associated\" LIMIT 10";

        SqlConnection conn = new SqlConnection(sqlconnparams, 0);

        System.out.println("Executing query.");
        ResultSet rs = conn.executeSimpleQuery(query);

        String r, r2;
        try {
            int count = 0;
            while (rs.next()) {

                r = rs.getString("Genus");
                r2 = rs.getString("Species");

                System.out.println(r + " " + r2);
                count++;
            }
            System.out.println("RESULTS: " + count);
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void runQuestQuery() throws OWLException {
        String q0 = " PREFIX : <http://www.semanticweb.org/ontologies/2012/4/fishdelish.owl#>\n" +
                "\t\t\t\tPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "\t\t\t\tSELECT  ?order ?family ?genus ?species  ?fbname ?name ?occurence ?dangerous \n" +
                "                WHERE {\n" +
                "                $speciesID a :Species .\n" +
                "\t\t\t\t$speciesID :genus ?genus .\n" +
                "\t\t\t\t$speciesID :species ?species .\n" +
                "\t\t\t\t$speciesID :FBname ?fbname .\n" +
                "\t\t\t\t$speciesID :isDangerous ?dangerous .\n" +
                "\t\t\t\t$speciesID :demersPelag \"reef-associated\"^^xsd:string .\n" +
                "\t\t\t\t$speciesID :isFoundInCountry ?c .\n" +
                "\t\t\t\t$speciesID :hasCommonName ?nameID .\n" +
                "\t\t\t\t$speciesID :belongsToFamily ?f .\n" +
                "\t\t\t\t$f a :Family .\n" +
                "\t\t\t\t$f :familyName  ?family .\n" +
                "\t\t\t\t$f :familyOrder ?order .\n" +
                "\t\t\t\t$nameID a :CommonName .\n" +
                "                $nameID :speciesCommonName ?name .\n" +
                "                $countryID :countryName \"Tahiti\"^^xsd:string .\n" +
                "                $countryID :refersToCountry ?c .\n" +
                "                $c :status ?occurence .              \n" +
                "                } ";


        String q1 = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>    " +
                "PREFIX : <http://www.semanticweb.org/ontologies/2012/4/fishdelish.owl#>    " +
                "SELECT ?type ?country ?genus ?species " +
                "WHERE {  " +
                "$speciesID a :Species . " +
                "$speciesID :genus ?genus .  " +
                "$speciesID :species ?species .   " +
                "$speciesID :hasCommonName ?nameID . " +
                "$nameID a :CommonName .       " +
                "$nameID :nameLanguage 'english'^^xsd:string .  " +
                "$nameID :speciesCommonName 'Andre's lanternfish'^^xsd:string .  " +
                "$nameID :nameLanguageType ?type .  " +
                "$nameID :belongsToCountry ?countryID .  " +
                "$countryID a :Country . " +
                "$countryID :countryName ?country . " +
                "} LIMIT 10";


        String q2 = " PREFIX : <http://www.semanticweb.org/ontologies/2012/4/fishdelish.owl#>\n" +
                "\t\t\t\tPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "\t\t\t\tselect ?SpecCode  ?Author ?FBname ?SpeciesRef ?author ?year ?comments ?family ?order ?class ?PictureName ?Photographer ?picID ?Entered ?demersPelag \n" +
                "\t\t\t\t?AnaCat\n" +
                "\t\t\t\t?description \n" +
                "\t\t\t\twhere {\n" +
                "\t\t\t\t$p a :Picture .\n" +
                "\t\t\t\t$p :pictureName ?PictureName .\n" +
                "\t\t\t\t$p :author ?Photographer .\n" +
                "\t\t\t\t$p :pictureID ?picID .\n" +
                "\t\t\t\t$p :entered ?Entered .\n" +
                "\t\t\t\t$p :isPictureOf ?id .\n" +
                "\t\t\t\t$id a :Species .\n" +
                "\t\t\t\t$id :speciesID ?SpecCode .\n" +
                "\t\t\t\t$id :species \"hagiangensis\"^^xsd:string .\n" +
                "\t\t\t\t$id :genus \"Paracobitis\"^^xsd:string .\n" +
                "\t\t\t\t$id :author ?Author .\n" +
                "\t\t\t\t$id :FBname ?FBname .\n" +
                "\t\t\t\t$id :comments ?comments .\n" +
                "\t\t\t\t$id :speciesReferenceNo ?SpeciesRef .\n" +
                "\t\t\t\t$id :belongsToFamily ?f .\n" +
                "\t\t\t\t$id :hasSpeciesReference ?y .\n" +
                "\t\t\t\t$y a :Reference .\n" +
                "\t\t\t\t$y :author ?author .\n" +
                "\t\t\t\t$y :year ?year .\t\n" +
                "\t\t\t\t$f a :Family .\n" +
                "\t\t\t\t$f :familyName  ?family .\n" +
                "\t\t\t\t$f :familyOrder ?order .\n" +
                "\t\t\t\t$f :class ?class .\n" +
                "\t\t\t\t$id :demersPelag ?demersPelag .\n" +
                "\t\t\t\t$id :anaCat ?AnaCat .\n" +
                "\t\t\t\t$id :hasMorphology ?m .\n" +
                "\t\t\t\t$id :pictureName ?picPreferedName .\n" +
                "\t\t\t\t$m a :Morphology .\n" +
                "\t\t\t\t$m :addChars ?description .\n" +
                "\t\t\t\t}";

        ObdaConnection ex = new ObdaConnection(owl, obda, 0);
        OWLResultSet rs = ex.executeSimpleQuery(q0);

        while (rs.nextRow()) {

            OWLLiteral l = rs.getOWLLiteral(1);
            String name = l.getLiteral();
            System.out.println(name);
        }

        ex.close();
    }


}
