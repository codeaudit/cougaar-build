/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.tools.build;

import java.io.*;
import java.util.*;

public class VersionWriter extends WriterBase {
  private String[] args;
  boolean isVerbose = false;
  boolean didIt = false;

  String version = null;
  String deffilename = null;


  private void readFile(String filename) {
    InputStream stream = null;
    try {
      setDirectories(filename);
      if (filename.equals("-")) {
        debug("Reading from standard input.");
        stream = new java.io.DataInputStream(System.in);
      } else {
        deffilename = filename;
        debug("Reading \""+filename+"\".");
        stream = new FileInputStream(filename);
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
      PrintWriter filelist = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getTargetDir(),"version.gen"))));
      println(filelist,"Version.java");
      filelist.close();

      PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getTargetDir(),"Version.java"))));
      writeCR(out, deffilename);
      println(out);
      println(out,"package org.cougaar;");
      if (version == null) version = "unknown";
      String rtag = System.getProperty("repository.tag");
      String rtim = System.getProperty("repository.time");
      boolean rmod = "true".equals(System.getProperty("repository.modified"));
      println(out,"public final class Version {\n"+
                  "  public final static String version = \""+version+"\";\n"+
                  "  public final static long buildTime = "+System.currentTimeMillis()+"L;\n"+
		  "  public final static void main(String args[]) {\n"+
		  "    System.out.println(\"version=\"+version);\n"+
		  "    System.out.println(\"build time=\"+new java.util.Date(buildTime));");
      println(out, "    System.out.println(\"repository tag="+rtag+
	  (rmod?" (modified)":"")+"\");");
      println(out, "    System.out.println(\"repository time="+rtim+"\");");
      println(out,"  }\n"+
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
        } else if (arg.equals("-d")) {
          targetDirName = args[++i];
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

