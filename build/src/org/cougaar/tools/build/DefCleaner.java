/*
 * <copyright>
 *  Copyright 2000-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.tools.build;

import java.io.*;
import java.util.*;

/** A tool for cleaning up generated code files as listed in
 * .gen files.  This supports the ANT build method.
 **/
public class DefCleaner {

    private static void parse(String filename) {
        File genFile = new File(filename);
        InputStream in;
        List filesToDelete = new ArrayList();
        try {
            if (filename.equals("-")) {
                in = new DataInputStream(System.in);
            } else {
                try {
                    in = new FileInputStream(filename);
                } catch (FileNotFoundException fe) {
                    in = DefCleaner.class.getClassLoader().getResourceAsStream(filename);
                }
            }
            if (in == null) {
                System.err.println("File "+filename+" could not be opened.");
            }
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                filesToDelete.add(new File(genFile.getParentFile(), line));
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Iterator i = filesToDelete.iterator(); i.hasNext(); ) {
            File fileToDelete = (File) i.next();
            try {
                fileToDelete.delete();
                System.out.println("deleted " + fileToDelete);
            } catch (Exception e) {
                System.err.println(e + ": " + fileToDelete);
            }
        }
    }

    public static void main(String args[]) {
        for (int i = 0; i<args.length; i++) {
            String defname = args[i];
            parse(defname);
        }
    }
}
