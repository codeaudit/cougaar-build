/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.tools.build;

import java.io.*;
import java.util.*;

class VersionWriter {
  private String[] args;
  boolean isVerbose = false;
  boolean didIt = false;

  String version = null;

  File targetdir = null;

  private void readFile(String filename) {
    InputStream stream = null;
    try {
      targetdir = new File(System.getProperty("user.dir"));
      if (filename.equals("-")) {
        debug("Reading from standard input.");
        stream = new java.io.DataInputStream(System.in);
      } else {
        debug("Reading \""+filename+"\".");
        stream = new FileInputStream(filename);

        int p;
        if ((p=filename.lastIndexOf(File.separatorChar))!=-1) {
          targetdir = new File(filename.substring(0,p));
        }
      }
      
      InputStreamReader isr = new InputStreamReader(stream);
      BufferedReader br = new BufferedReader(isr);
      
      for (String line = br.readLine(); line != null; line=br.readLine()) {
        if (!(line.startsWith(";") ||
              line.startsWith("#"))) {
          line=line.trim();
          // not a comment
          if (line.length()>0) {
            if (version != null) {
              version=version+"/"+line;
            } else {
              version=line;
            }
          }
        }
      }

      br.close();
      didIt = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void writeFile() {
    try {
      PrintWriter filelist = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(targetdir,"version.gen"))));
      filelist.println("Version.java");
      filelist.close();

      PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(targetdir,"Version.java"))));
    out.println("/*\n"+
                " * <copyright>\n"+
                " * Copyright 1997-2000 Defense Advanced Research Projects Agency (DARPA)\n"+
                " * and ALPINE (A BBN Technologies (BBN) and Raytheon Systems Company\n"+
                " * (RSC) Consortium). This software to be used in accordance with the\n"+
                " * COUGAAR license agreement.  The license agreement and other\n"+
                " * information on the Cognitive Agent Architecture (COUGAAR) Project can\n"+
                " * be found at http://www.cougaar.org or email: info@cougaar.org.\n"+
                " * </copyright>\n"+
                " */");
      out.println();
      out.println("// source machine generated at "+new java.util.Date()+" - Do not edit");
      out.println("/* @"+"generated */");
      out.println();
      out.println("package alp;");
      if (version == null) version = "unknown";
      out.println("public final class Version {\n"+
                  "  public final static String version = \""+version+"\";\n"+
                  "  public final static long buildTime = "+System.currentTimeMillis()+"L;\n"+
                  "}");
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void debug(String s) {
    if (isVerbose)
      System.err.println(s);
  }

  void usage(String s) {
    System.err.println(s);
    System.err.println("Usage: VersionWriter [-v] version.def");
    System.exit(1);
  }

  public void start() {
    for (int i=0; i<args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-")) { // parse flags
        if (arg.equals("-")) {
          readFile("-");
        } else if (arg.equals("-v")) {
          isVerbose = (!isVerbose);
        } else {
          usage("Unknown option \""+arg+"\"");
        }
      } else {                  // deal with files
        readFile(arg);
      }
    }
    if (!didIt) {
      readFile("version.def");
    }

    writeFile();
  }

  public VersionWriter(String args[]) {
    this.args=args;
  }

  public static void main(String args[]) {
    VersionWriter vw = new VersionWriter(args);
    vw.start();
  }
}

