package test;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import uk.ac.manchester.cs.owl.mumbmf.RunBenchmark;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.Query;
import uk.ac.manchester.cs.owl.mumbmf.queryexecution.SparqlQuery;
import uk.ac.manchester.cs.owl.mumbmf.util.Util;

import java.io.InputStream;

/**
 * Created by
 * User: Samantha Bail
 * Date: 27/10/2012
 * Time: 12:20
 * The University of Manchester
 */


public class QueryTest {

    private static String serviceURL = "http://130.88.192.86:8890/sparql/";
    private static String queryString = "PREFIX fd: <http://fishdelish.cs.man.ac.uk/rdf/vocab/resource/>\n" +
            "                        SELECT ?comname\n" +
            "                        WHERE {\n" +
            "                        ?nameID fd:comnames_ComName ?comname .\n" +
            "                        ?nameID fd:comnames_Language \"English\" .\n" +
            "                        } LIMIT 5";

    private static String queryString2 = "PREFIX fd: <http://fishdelish.cs.man.ac.uk/rdf/vocab/resource/>\n" +
            "                        SELECT ?genus ?species\n" +
            "                        WHERE {\n" +
            "                        ?code fd:species_Genus ?genus .\n" +
            "                        ?code fd:species_Species ?species .\n" +
            "                        } LIMIT 5";

    private static String defaultGraph = null;
    private static int timeout = 0;

    public static void main(String[] args) {
//        runGenTest();
        runMultiTest();
    }

    private static void runMultiTest() {


        String[] params = {"-genargs", "/Users/samantha/code/testfiles/fishmark/gargs.txt",
                "-querysets", "50",
                "-runs", "100",
                "-w", "50",

//                mysql 30k
                "-sql", "\"-ucf /Users/samantha/code/mum-benchmark/data/example/fishmark_usecases/sql_usecase.txt " +
                "-results /Users/samantha/code/testfiles/fishmark/results/mysql30k/ " +
                "-dbserver jdbc:mysql://130.88.192.86/fishmark30k -dblogin fishdelish -dbpw fishdelish\"",

//                mysql complete
                "-sql", "\"-ucf /Users/samantha/code/mum-benchmark/data/example/fishmark_usecases/sql_usecase.txt " +
                "-results /Users/samantha/code/testfiles/fishmark/results/mysqlcomplete/ " +
                "-dbserver jdbc:mysql://130.88.192.86/fishbase_complete -dblogin fishdelish -dbpw fishdelish\"",

//                virtuoso
                "-sparql", "\"-sparqlendpoint http://cspc017.cs.man.ac.uk:8890/sparql/ " +
                "-results /Users/samantha/code/testfiles/fishmark/results/virtuoso/ " +
                "-ucf /Users/samantha/code/mum-benchmark/data/example/fishmark_usecases/sparql_usecase.txt\"",

//               quest
                "-obda", "\"-obdafile /Users/samantha/code/testfiles/fishmark/obda/fishdelish.obda " +
                "-owlfile /Users/samantha/code/testfiles/fishmark/obda/fishdelish.owl " +
                "-results /Users/samantha/code/testfiles/fishmark/results/quest/ " +
                "-ucf /Users/samantha/code/mum-benchmark/data/example/fishmark_usecases/obda_usecase.txt\"",

//                stardog
                "-stardog", "\"-stardogurl http://cspc017.cs.man.ac.uk:5822/ " +
                "-stardogdb fishmarkallspecies " +
                "-stardoglogin admin -stardogpw admin " +
                "-results /Users/samantha/code/testfiles/fishmark/results/stardog/ " +
                "-ucf /Users/samantha/code/mum-benchmark/data/example/fishmark_usecases/sparql_usecase.txt\"",

//                sesame
                "-sparql", "\"-sparqlendpoint http://cspc017.cs.man.ac.uk:8080/openrdf-workbench/repositories/fishmarkallspecies/ " +
                "-results /Users/samantha/code/testfiles/fishmark/results/sesame/ " +
                "-ucf /Users/samantha/code/mum-benchmark/data/example/fishmark_usecases/sparql_usecase.txt\""
        };

        String[] params2 = {"-genargs", "/Users/samantha/code/testfiles/fishmark/gargs.txt",
                "-querysets", "50",
                "-runs", "100",
                "-w", "50",

//                mysql complete
                "-sql", "\"-ucf /Users/samantha/code/mum-benchmark/data/example/fishmark_usecases/sql_usecase.txt " +
                "-results /Users/samantha/code/testfiles/fishmark/results/mysqlcomplete/ " +
                "-dbserver jdbc:mysql://130.88.192.86/fishbase_complete -dblogin fishdelish -dbpw fishdelish\""
        };


        String start =  Util.getTimeStamp();

        RunBenchmark.runGenerateAndBenchmarkMultipleQuerySets(params2);

        String stop =  Util.getTimeStamp();

        System.out.println("BENCHMARK COMPLETE. ");
        System.out.println("START:  " + start);
        System.out.println("STOP:   " + stop);

    }

    private static void runGenTest() {
        String[] params = {"-seedtype", "sql",
                "-sql", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/sql.xml",
                "-obda", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/obda.xml",
                "-sparql", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/sparql.xml",
                "-output", "/Users/samantha/code/mum-benchmark/data/example/queries/",
                "-qmfile", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/querymix.txt",
                "-dbserver", "jdbc:mysql://130.88.192.86/fbapp",
                "-dblogin", "fishdelish", "-dbpw", "fishdelish"};
        String[] params2 = {"-seedtype", "obda",
                "-sql", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/sql.xml",
                "-obda", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/obda.xml",
                "-sparql", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/sparql.xml",
                "-output", "/Users/samantha/code/mum-benchmark/data/example/queries/",
                "-qmfile", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/querymix.txt",
                "-dbserver", "jdbc:mysql://130.88.192.86/fbapp",
                "-sparqlendpoint", "http://130.88.192.86:8890/sparql/",
                "-obdafile", "/Users/samantha/code/testfiles/fishmark/obda/fishdelish.obda",
                "-owlfile", "/Users/samantha/code/testfiles/fishmark/obda/fishdelish.owl",
                "-dblogin", "fishdelish", "-dbpw", "fishdelish"};

        String[] params3 = {"-seedtype", "sparql",
                "-sparql", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/sparql.xml",
                "-output", "/Users/samantha/code/mum-benchmark/data/example/queries/",
                "-qmfile", "/Users/samantha/code/mum-benchmark/data/example/fishmark_queries/querymix.txt",
                "-sparqlendpoint", "http://130.88.192.86:8890/sparql/"};

        RunBenchmark.runMultiQueryGeneration(params3);


    }

    public static void runSimpleQueryTest(String[] args) {
        for (int i = 0; i < 5; i++) {
            SparqlQuery qe = new SparqlQuery(serviceURL, queryString, Query.SELECT_TYPE, defaultGraph, timeout);
            InputStream is = qe.exec();
//        Scanner s = new Scanner(is);
//        while (s.hasNext()) {
//            System.out.println(s.next());
//        }

            SAXBuilder builder = new SAXBuilder();
            builder.setValidation(false);
            builder.setIgnoringElementContentWhitespace(true);
            org.jdom.Document doc = null;
            try {
                doc = builder.build(is);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }

            System.out.println(doc.toString());
            DOMOutputter domOut = new DOMOutputter();
            try {
                org.w3c.dom.Document d = domOut.output(doc);
            } catch (JDOMException e) {
                e.printStackTrace();
            }

            System.out.println("\n==========QUERY 2===========");

            SparqlQuery qe2 = new SparqlQuery(serviceURL, queryString2, Query.SELECT_TYPE, defaultGraph, timeout);
            InputStream is2 = qe2.exec();
//        Scanner s2 = new Scanner(is2);
//        while (s2.hasNext()) {
//            System.out.println(s2.next());
//        }

            org.jdom.Document doc2 = null;
            try {
                doc2 = builder.build(is2);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }

            System.out.println(doc2.toString());
        }
    }
}
