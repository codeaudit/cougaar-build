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
import java.lang.reflect.*;

/** A tool for executing various COUGAAR code generators as specified by 
 * .def files.  This supports the ANT build method.
 **/
public class DefRunner {
  /**
   * Run the code generator against the given file and a command line
   **/
  public static void runDef(String defile, String argline) {
    List argx = explode(argline, " ");
    int l = argx.size();

    String classname = (String) argx.get(0);

    List nargs = new ArrayList();
    for (int i = 1; i<l; i++) {
      String a = (String) argx.get(i);
      // hack for old-style compatability
      if (defile.endsWith(a)) {
        System.err.println(defile+": skipping redundant arg "+a);
      } else {
        nargs.add(a);
      }
    }
    nargs.add(defile);          // last argument is always the def file

    String[] args = (String[]) nargs.toArray(new String[nargs.size()]);

    if (true) {
      System.err.print(classname+".main([");
      for (int k=0;k<args.length;k++) {
        if (k!=0) System.err.print(", ");
        System.err.print(args[k]);
      }
      System.err.println("])...");
    }

    // cribbed from Bootstrapper
    try {
      ClassLoader cl = DefRunner.class.getClassLoader();
      Class realnode = cl.loadClass(classname);
      Class argl[] = new Class[] { String[].class };

      Method main = realnode.getMethod("main", argl);
      Object[] argv = new Object[] { args };
      main.invoke(null,argv);
    } catch (Exception e) {
      System.err.println("Failed to RunDef "+classname+": ");
      e.printStackTrace();
    }
  }

  private static final List explode(String s, String seps) {
    StringTokenizer tokens = new StringTokenizer(s, seps);
    List v = new ArrayList();
    while (tokens.hasMoreTokens()) {
      v.add(tokens.nextToken());
    }
    return v;
  }

  /**
   * Read the first line of a file and construct and run the command
   * to generate code from that file.
   **/
  public static void parse(String filename, String options) {
    InputStream in;
    try {
      if (filename.equals("-")) {
        in = new DataInputStream(System.in);
      } else {
        try {
          in = new FileInputStream(filename);
        } catch (FileNotFoundException fe) {
          in = DefRunner.class.getClassLoader().getResourceAsStream(filename);
        }
      }
      if (in == null) {
        System.err.println("File "+filename+" could not be opened.");
      }
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader br = new BufferedReader(isr);

      // read the first line - it'll tell us what to do next
      String line = br.readLine();
      if (line != null) {
        int i;

        // check for old syntax
        if ( (i = line.indexOf("!generate:")) != -1) {
	  String args = line.substring(i+10).trim();
	  if (options != null) {
	    args += " " + options;
	  }
          runDef(filename, args);
        } else if ( (i = line.indexOf("!")) != -1) {
	  String args = line.substring(i+1).trim();
	  if (options != null) {
	    args += " " + options;
	  }
          runDef(filename, args);
        } else {
          System.err.println("File \""+filename+"\" is a broken def file!");
        }
      }
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) {
    String options = null;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-o")) {
          options = args[++i];
      } else {
          parse(arg, options);
      }
    }
  }
}
