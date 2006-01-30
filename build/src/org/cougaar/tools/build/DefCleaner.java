/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
import java.util.List;

/** A tool for cleaning up generated code files as listed in
 * .gen files.  This supports the ANT build method.
 **/
public class DefCleaner {

    private static void parse(String filename) {
        File genFile = new File(filename);
        InputStream in;
        List<File> filesToDelete = new ArrayList<File>();
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
        for (File fileToDelete : filesToDelete) {
            try {
                fileToDelete.delete();
                System.out.println("deleted " + fileToDelete);
            } catch (Exception e) {
                System.err.println(e + ": " + fileToDelete);
            }
        }
    }

    public static void main(String args[]) {
      for (String defname : args) {
        parse(defname);
      }
    }
}
