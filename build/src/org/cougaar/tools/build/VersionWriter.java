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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Write a class Version.java using <code>version.def</code> to get the Version number,
 * and system properties <code>repository.tag</code>, <code>repository.time</code>, 
 * and <code>repository.modified</code> to get Repository information.
 **/
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

      // Make package match getTargetDir()
      println(out,"package " + getPackageFromDir(getSourceDir()) + ";");
      if (version == null) version = "unknown";
      String rtag = System.getProperty("repository.tag");
      String rdate = System.getProperty("repository.time");
      boolean rmod = "true".equals(System.getProperty("repository.modified"));
      long rtime = -1;
      if (rdate != null) {
        try {
          // example rdate: "4/5/2002 16:00:07"  (GMT?)
          SimpleDateFormat df = new SimpleDateFormat("M/d/yyyy H:mm:ss");
          Date d = df.parse(rdate);
          rtime = d.getTime();
        } catch (Exception eDateFormat) {
        }
      }
      println(out,
          "public final class Version {\n"+
          "  public final static String version = \""+
          version+"\";\n"+
          "  public final static long buildTime = "+
          System.currentTimeMillis()+"L;\n"+
          "  public final static String repositoryTag = "+
          ((rtag != null) ? ("\""+rtag+"\"") : "null")+";\n"+
          "  public final static boolean repositoryModified = "+
          ((rtag != null) ? rmod : false)+";\n"+
          "  public final static long repositoryTime = "+
          rtime+"L;\n"+
          "  public final static void main(String args[]) {\n"+
          "    System.out.println(\"version=\"+version);\n"+
          "    System.out.println(\"build time=\"+new java.util.Date(buildTime));");
      if (rtag != null) {
        println(out, "    System.out.println(\"repository tag="+rtag+
            (rmod?" (modified)":"")+"\");");
      }
      if (rdate != null) {
        println(out, "    System.out.println(\"repository time="+
            ((rtime > 0) ?
             ("\"+new java.util.Date(repositoryTime));") :
             ("\\\""+rdate+"\\\"\");")));
      }
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

