/*
 * <copyright>
 *  Copyright 1997-2001 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.tools.build;

import java.io.*;

public class WriterBase {

    protected String targetDirName = null;
    private File targetDir = null;
    private File sourceDir = null;
    private String genFileName = "generated.gen";
    protected File getTargetDir() {
	return targetDir;
    }
    protected File getSourceDir() {
	return sourceDir;
    }
    protected String getGenFileName() {
        return genFileName;
    }

    protected void setDirectories(String defFileName) {
        if (defFileName.equals("-")) {
            sourceDir = new File(System.getProperty("user.dir"));
        } else {
            File defFile = new File(defFileName);
            sourceDir = defFile.getParentFile();
            String s = defFile.getName();
            int dotPos = s.lastIndexOf('.');
            if (dotPos < 0) {
                genFileName = s + ".gen";
            } else {
                genFileName = s.substring(0, dotPos) + ".gen";
            }
        }
	if (targetDirName != null) {
	    targetDir = new File(targetDirName);
	} else {
	    targetDir = sourceDir;
	}
    }

  protected final static void println(PrintWriter out, String s) {
    print(out, s);
    println(out);
  }

  protected final static void println(PrintWriter out) {
    out.println();
  }

  protected final static void print(PrintWriter out, String s) {
    int p = 0;                  // cursor position
    int l = s.length();         // length of s
    while (p<l) {
      int i = s.indexOf('\n', p);
      if (i == -1) {
        out.print(s.substring(p));
        p=l;
      } else {
        out.print(s.substring(p,i));
        out.println();
        p=i+1;
      }
    }
  }
  
  protected final static void writeCR(PrintWriter out) {
    writeCR(out, null);
  }
  protected final static void writeCR(PrintWriter out, String source) {
    println(out,"/*\n"+
                " * <copyright>\n"+
                " * Copyright 1997-2001 Defense Advanced Research Projects Agency (DARPA)\n"+
                " * and ALPINE (A BBN Technologies (BBN) and Raytheon Systems Company\n"+
                " * (RSC) Consortium). This software to be used in accordance with the\n"+
                " * COUGAAR license agreement.  The license agreement and other\n"+
                " * information on the Cognitive Agent Architecture (COUGAAR) Project can\n"+
                " * be found at http://www.cougaar.org or email: info@cougaar.org.\n"+
                " * </copyright>\n"+
                " */");
    println(out);
    println(out,"/* @"+"generated "+new java.util.Date()+((source==null)?"":(" from "+source))+" - DO NOT HAND EDIT */");
  }
}
