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

import java.io.File;
import java.io.PrintWriter;

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

  // Given a file handle, construct a package name
  // that matches. Allows each module to have a Version that could 
  // be different.
  protected String getPackageFromDir(File directory) {
    if (directory == null)
      return ".";

    String dirName = directory.getAbsolutePath();

    // Now: which piece of this is the package?
    // Assume Version is under a "src" dir
    if (dirName.lastIndexOf("src") != -1) {
      dirName = dirName.substring(dirName.lastIndexOf("src") + 4);
    } else {
    }

    StringBuffer dirBuf = new StringBuffer(dirName);
    // put in the "."
    int ix = 0;
    while ((ix = dirBuf.indexOf(File.separator)) != -1) {
      dirBuf.replace(ix, ix+1, ".");
    }

    return dirBuf.substring(0);
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
                " *  Copyright 1997-2003 BBNT Solutions, LLC\n"+
                " *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).\n"+
                " * \n"+
                " *  This program is free software; you can redistribute it and/or modify\n"+
                " *  it under the terms of the Cougaar Open Source License as published by\n"+
                " *  DARPA on the Cougaar Open Source Website (www.cougaar.org).\n"+
                " * \n"+
                " *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS\n"+
                " *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR\n"+
                " *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF\n"+
                " *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT\n"+
                " *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT\n"+
                " *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL\n"+
                " *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,\n"+
                " *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR\n"+
                " *  PERFORMANCE OF THE COUGAAR SOFTWARE.\n"+
                " * </copyright>\n"+
                " */");
    println(out);
    if (source != null) source = source.replace('\\', '/'); // stupid windows!
    println(out,"/* @"+"generated "+new java.util.Date()+((source==null)?"":(" from "+source))+" - DO NOT HAND EDIT */");
  }
}
