/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.tools.build;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
