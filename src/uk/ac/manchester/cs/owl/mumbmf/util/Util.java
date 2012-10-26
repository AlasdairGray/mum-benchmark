package uk.ac.manchester.cs.owl.mumbmf.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by
 * User: Samantha Bail
 * Date: 30/08/2012
 * Time: 16:00
 * The University of Manchester
 */


public class Util {

    /**
     * deepCopy a file from one location to another
     * @param sourceFile source file
     * @param targetFile destination file
     */
    public static void copyFile(File sourceFile, File targetFile) {
        try {

            InputStream in = new FileInputStream(sourceFile);
            //to append the file.
            // OutputStream out = new FileOutputStream(f2,true);
            //to overwrite the file.
            OutputStream out = new FileOutputStream(targetFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String getTimeStamp() {
        /**
         * @return current timestamp in filename-friendly format
         */
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HHmmssSS");
        return sdf.format(cal.getTime());

    }

}
