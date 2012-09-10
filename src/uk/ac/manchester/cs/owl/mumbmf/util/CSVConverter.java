package uk.ac.manchester.cs.owl.mumbmf.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

public class CSVConverter {

    public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException {
        File inputDir = new File(args[0]);


        int i = 0;
        for (File xmlFile : inputDir.listFiles()) {
            if (xmlFile.getName().endsWith(".xml")) {

                InputStream inputStream = new FileInputStream(xmlFile);
                Reader reader = new InputStreamReader(inputStream, "UTF-8");

                InputSource is = new InputSource(reader);
                is.setEncoding("UTF-8");


                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//                Document doc = dBuilder.parse(xmlFile);

                Document doc = dBuilder.parse(is);
                doc.getDocumentElement().normalize();

                NodeList nList = doc.getElementsByTagName("query");

                String outDir = args[1];
                String sep = System.getProperty("file.separator");

                if (!outDir.endsWith(sep))
                    outDir += sep;

                String outFile = outDir + i++ + "result.csv";

                System.out.println("Input: " + xmlFile.getAbsolutePath());
                System.out.println("Output: " + outFile);

                BufferedWriter out = new BufferedWriter(new FileWriter(new File(outFile)));
                out.append("\nQuery Nr., Execute Count, Avg. Query Execution Time, aqetg (?), Queries Per Second, Min. Query Execution Time," +
                        "Max. Query Execution Time, Avg. Results, Min. Results, Max. Results, Timeout Count\n");

                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        System.out.println("-----------------------");
                        Element eElement = (Element) nNode;

                        String queryNr = eElement.getAttribute("nr");
                        System.out.println("Query Number: " + queryNr);
                        out.append(queryNr + ",");

                        String executecount = getTagValue("executecount", eElement);
                        System.out.println("Nr. Runs: " + executecount);
                        out.append(executecount + ",");

                        String aqet = getTagValue("aqet", eElement);
                        System.out.println("aqet: " + aqet);
                        out.append(aqet + ",");

                        String aqetg = getTagValue("aqetg", eElement);
                        System.out.println("aqetg: " + aqetg);
                        out.append(aqetg + ",");

                        String qps = getTagValue("qps", eElement);
                        System.out.println("qps: " + qps);
                        out.append(qps + ",");

                        String minqet = getTagValue("minqet", eElement);
                        System.out.println("minqet: " + minqet);
                        out.append(minqet + ",");

                        String maxqet = getTagValue("maxqet", eElement);
                        System.out.println("maxqet: " + maxqet);
                        out.append(maxqet + ",");

                        String avgresults = getTagValue("avgresults", eElement);
                        System.out.println("avgresults: " + avgresults);
                        out.append(avgresults + ",");

                        String minresults = getTagValue("minresults", eElement);
                        System.out.println("minresults: " + minresults);
                        out.append(minresults + ",");

                        String maxresults = getTagValue("maxresults", eElement);
                        System.out.println("maxresults: " + maxresults);
                        out.append(maxresults + ",");

                        String timeoutcount = getTagValue("timeoutcount", eElement);
                        System.out.println("timeoutcount: " + timeoutcount);
                        out.append(timeoutcount + "\n");
                    }
                }

                out.close();
            }
        }
    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

        Node nValue = (Node) nlList.item(0);

        return nValue.getNodeValue();
    }
}