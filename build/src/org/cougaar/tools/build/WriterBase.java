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
                " *  \n"+
                " *  Copyright 1997-2004 BBNT Solutions, LLC\n"+
                " *  under sponsorship of the Defense Advanced Research Projects\n"+
                " *  Agency (DARPA).\n"+
                " * \n"+
                " *  You can redistribute this software and/or modify it under the\n"+
                " *  terms of the Cougaar Open Source License as published on the\n"+
                " *  Cougaar Open Source Website (www.cougaar.org).\n"+
                " * \n"+
                " *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS\n"+
                " *  \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT\n"+
                " *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR\n"+
                " *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT\n"+
                " *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,\n"+
                " *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT\n"+
                " *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n"+
                " *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"+
                " *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n"+
                " *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"+
                " *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"+
                " *  \n"+
                " * </copyright>\n"+
                " */");
    println(out);
    if (source != null) source = source.replace('\\', '/'); // stupid windows!
    println(out,"/* @"+"generated "+new java.util.Date()+((source==null)?"":(" from "+source))+" - DO NOT HAND EDIT */");
  }
}
