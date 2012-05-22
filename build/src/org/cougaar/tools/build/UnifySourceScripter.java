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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class searches for Cougaar source code in subdirectories
 * of the $COUGAAR_INSTALL_PATH and prints a Bash script to
 * generate a unified (symbolically-linked) source view.
 * <p>
 * The script output of this class is included in the Cougaar
 * release.  This script can also be run by developers to
 * add new modules, packages, and third-party source
 * (e.g. the JDK's unzipped "src.zip").
 * <p>
 * This class is fairly dumb, so watch out for symbolic link
 * loops, bad file permissions, and other unexpected errors.
 * <p>
 * For example, suppose the current directory contained this
 * subset of the Cougaar source files (in "cougaar-src.zip"):
 * <pre>
 *    core/src/org/cougaar/core/agent/Agent.java
 *    core/src/org/cougaar/core/node/Node.java
 *    core/examples/org/cougaar/core/examples/Test.java
 *    util/src/org/cougaar/util/UnaryPredicate.java
 *    util/src/org/cougaar/util/log/Logger.java
 *    util/src/org/cougaar/core/component/Service.java
 * </pre>
 * The unified source would look like:
 * <pre>
 *    src/org/cougaar/core/agent/Agent.java
 *    src/org/cougaar/core/node/Node.java
 *    src/org/cougaar/core/examples/Test.java
 *    src/org/cougaar/util/UnaryPredicate.java
 *    src/org/cougaar/util/log/Logger.java
 *    src/org/cougaar/core/component/Service.java
 * </pre>
 * To use the minimal number of symbolic links, we would do:
 * <pre>
 *    core=$COUGAAR_INSTALL_PATH/core
 *    util=$COUGAAR_INSTALL_PATH/util
 *    mkdir src; cd src
 *      mkdir org; cd org
 *        mkdir cougaar; cd cougaar
 *          mkdir core; cd core
 *            ln -s $core/src/org/cougaar/core/agent
 *            ln -s $util/src/org/cougaar/core/component
 *            ln -s $core/examples/org/cougaar/core/examples
 *            ln -s $core/src/org/cougaar/core/node
 *          cd ..
 *          ln -s $util/src/org/cougaar/util
 *        cd ..
 *      cd ..
 *    cd ..
 * </pre>
 * This class does exactly that -- it searches the directories
 * and generates the above script.
 * <p>
 * File conflicts are handled with symbolic links that point to
 * the first matching file.  For example, if we had:
 * <pre>
 *     core/src/foo/A.java
 *     util/src/foo/A.java
 * </pre>
 * then the output would look like:
 * <pre>
 *     ln -s core/src/foo/A.java A.java#core
 *     ln -s util/src/foo/A.java A.java#util
 *     ln -s A.java#core A.java
 * </pre>
 */
public class UnifySourceScripter {

  public static final boolean EXCLUDE_CVS = true;

  private static final String[] CODE_DIRS = { 
    "src",
    "examples",
  };

  public static void main(String[] args) throws Exception {
    run();
  }

  public static void run() throws Exception {
    PrintStream out = System.out;

    out.println("#! /bin/sh");

    printCopywrite(out);

    out.println(
        "# Generated "+(new Date())+
        "\n# By "+UnifySourceScripter.class.getName());

    printNotes(out);

    out.println("BASE=$COUGAAR_INSTALL_PATH\n");

    out.println(
        "if [ -z $BASE ] || [ ! -d $BASE ]; then"+
        "\n  echo \"Missing directory: $BASE\""+
        "\n  exit 1"+
        "\nfi"+
        "\n");

    DirEntry rootDe = new DirEntry("src");

    File baseDir = new File(".");
    File[] files = baseDir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File f = files[i];
      if (f.isDirectory()) {
        String fname = f.getName();
        out.println(fname+"=$BASE/"+fname);
        inRootDir(rootDe, f);
      }
    }

    out.println(
        "\nmake_dir () {"+
        "\n  mkdir $1 || exit 1"+
        "\n}"+
        "\n"+
        "\nlink () {"+
        "\n  ln -s $* || exit 1"+
        "\n}"+
        "\n");

    printDirActions(out, rootDe, "");

    printReadme(out, "src");
  }

  /**
   * Recurse down a root subdirectory for second-level directories
   * of interest ("src" and "examples").
   *
   * E.g. this finds "core/src", "core/examples", "util/src", etc.
   */
  private static void inRootDir(
      DirEntry rootDe, File dir) throws Exception {
    String path = dir.getPath()+File.separator;
    for (int i = 0; i < CODE_DIRS.length; i++) {
      File subdir = new File(path+CODE_DIRS[i]);
      if (subdir.isDirectory()) {
        File[] files = subdir.listFiles();
        for (int j = 0; j < files.length; j++) {
          File f = files[j];
          if (f.isDirectory()) {
            findIn(rootDe, f);
          }
        }
      }
    }
  }
  
  /**
   * Recursively find and split directory entries off of
   * the parent entry.
   * <p>
   * This does all the interesting work...
   * <p>
   * @note recursive!
   */
  public static void findIn(
      DirEntry parentDe,
      File dir) throws Exception {
    String dirname = dir.getName();
    if (EXCLUDE_CVS && (dirname.equals("CVS") || dirname.equals(".svn"))) {
      return;
    }
    Object obj = parentDe.getSubDir(dirname);
    if (obj == null) {
      // new directory, so we'll link
      parentDe.addSubDir(dirname, dir);
      return;
    }

    DirEntry dirEntry;
    if (obj instanceof DirEntry) {
      // already split this directory
      dirEntry = (DirEntry) obj;
    } else if (obj instanceof File) {
      // we must split this directory
      File oldDir = (File) obj;
      if (!(oldDir.isDirectory())) {
        throw new RuntimeException(
            "unexpected non-dir: "+oldDir);
      }
      // replace with new dirEntry
      dirEntry = new DirEntry(dirname);
      parentDe.addSubDir(dirname, dirEntry);
      // fill oldDir into my dirEntry
      File[] files = oldDir.listFiles();
      for (int i = 0; i < files.length; i++) {
        File f = files[i];
        String fname = f.getName();
        if (f.isDirectory()) {
          dirEntry.addSubDir(fname, f);
        } else {
          dirEntry.addFile(fname, f);
        }
      }
    } else {
      throw new RuntimeException(
          "unexpected object ("+
          obj.getClass().getName()+
          "): "+obj);
    }

    // add my subdirectories and files
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File f = files[i];
      if (f.isDirectory()) {
        // recurse!
        findIn(dirEntry, f);
      } else {
        String fname = f.getName();
        dirEntry.addFile(fname, f);
      }
    }
  }

  /** Our class that represents a conflicted directory */
  private static class DirEntry {
    private final String name;
    // Map<String, (File|DirEntry)>
    private final Map<String,Object> subdirs = new HashMap<String,Object>();
    // Map<String, List<File>>
    private Map<String, List<File>> files = new HashMap<String,List<File>>();
    
    public DirEntry(String name) {
      this.name = name;
    }
    public String getName() {
      return name;
    }
    public Object getSubDir(String dirname) {
      return subdirs.get(dirname);
    }
    public Iterator iterateSubDirs() {
      return subdirs.entrySet().iterator();
    }
    public void addSubDir(String dirname, Object obj) {
      // assert (obj instanceof DirEntry || 
      //         (obj instanceof File &&
      //          ((File) obj).isDirectory()));
      subdirs.put(dirname, obj);
    }
    public List<File> getFiles(String fname) {
      return files.get(fname);
    }
    public Collection<String> getFilenames() {
      return files.keySet();
    }
    
    public void addFile(String fname, File f) {
      List<File> l = files.get(fname);
      if (l == null) {
        l = new ArrayList<File>(2);
        files.put(fname, l);
      }
      l.add(f);
    }
  }

  private static void printCopywrite(PrintStream out) {
    out.println(
        "\n# <copyright>"+
        "\n#  "+
        "\n#  Copyright 1997-2004 BBNT Solutions, LLC"+
        "\n#  under sponsorship of the Defense Advanced Research Projects"+
        "\n#  Agency (DARPA)."+
        "\n# "+
        "\n#  You can redistribute this software and/or modify it under the"+
        "\n#  terms of the Cougaar Open Source License as published on the"+
        "\n#  Cougaar Open Source Website (www.cougaar.org)."+
        "\n# "+
        "\n#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS"+
        "\n#  \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT"+
        "\n#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR"+
        "\n#  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT"+
        "\n#  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,"+
        "\n#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT"+
        "\n#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,"+
        "\n#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY"+
        "\n#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT"+
        "\n#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE"+
        "\n#  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."+
        "\n#  "+
        "\n# </copyright>"+
        "\n");
  }

  private static void printNotes(PrintStream out) {
    out.println(
        "\n# This script creates a soft-linked unified view of the Cougaar"+
        "\n# source code.  This is very handy for navigating the source,"+
        "\n# running IDEs, and running debuggers such as OptimizeIt."+
        "\n#"+
        "\n# This script uses symbolic links (ln -s).  Bash and a compatible"+
        "\n# operating system are required (e.g. Linux).  Windows Cygwin users"+
        "\n# of Emacs should see the symbolic link utilities at:"+
        "\n#     http://centaur.maths.qmul.ac.uk/Emacs/files/w32-symlinks.el"+
        "\n# or, for VI and other non-Cygwin applications, see:"+
        "\n#     http://hermitte.free.fr/cygwin/#Win32"+
        "\n#"+
        "\n# Run this script in the $COUGAAR_INSTALL_PATH.  It will create"+
        "\n# a \"src/\" directory with links to the source contained in the"+
        "\n# \"cougaar-src.zip\".  Note that this script has little error"+
        "\n# checking, so use at your own risk..."+
        "\n#"+
        "\n# This script was machine-generated by the Java class:"+
        "\n#     "+UnifySourceScripter.class.getName()+
        "\n# which is included in the Cougaar release \"lib/build.jar\"."+
        "\n# The class can be used to generate a new script that includes"+
        "\n# your local development packages and third-party source"+
        "\n# (e.g. the JDK's unzipped \"$JAVA_HOME/src.zip\")"+
        "\n");
  }

  private static void printReadme(PrintStream out, String dir) {
    out.println(
        "\nif [ ! -f "+dir+"/README ]; then"+
        "\ncat > "+dir+"/README << EOF"+
        "\nThis is a symbolically-linked source view, generated by:"+
        "\n  cd \\$COUGAAR_INSTALL_PATH"+
        "\n  $0"+
        "\n"+
        "\nSee the above script for details."+
        "\nEOF"+
        "\nfi");
  }

  /**
   * Print the script actions for a completed DirEntry, and recurse
   * into the sub-dir-entries.
   * <p>
   * @note recursive!
   */
  private static void printDirActions(
      PrintStream out,
      DirEntry de, 
      String prefix) {
    out.println(prefix+"make_dir "+de.getName()+"; cd "+de.getName());
    for (Iterator iter = de.iterateSubDirs();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      Object val = me.getValue();
      if (val instanceof DirEntry) {
        DirEntry sde = (DirEntry) val;
        // recurse!
        printDirActions(out, sde, prefix+"  ");
      } else if (val instanceof File) {
        File f = (File) val;
        String fpath = f.getPath();
        fpath = "$"+fpath.substring(2);
        out.println(prefix+"  link "+fpath);
      }
    }
    
    for (String fn : de.getFilenames()) {
      String fname = null;
      String firstAlias = null;
      for (File f : de.getFiles(fn)) {
        if (fname == null) {
          fname = f.getName();
        }
        String fpath = f.getPath();
        fpath = "$"+fpath.substring(2);
        int sep = fpath.indexOf(File.separator);
        String falias = 
          fname+
          "#"+
          (sep > 0 ? fpath.substring(1,sep) : fpath);
        // check if (de.getFile(falias) != null) ?
        out.println(prefix+"  link "+fpath+" "+falias);
        if (firstAlias == null) {
          firstAlias = falias;
        }
      }
      out.println(prefix+"  link "+firstAlias+" "+fname);
    }
    out.println(prefix+"cd ..");
  }
}
