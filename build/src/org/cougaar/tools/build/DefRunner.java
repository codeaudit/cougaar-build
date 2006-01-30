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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/** A tool for executing various COUGAAR code generators as specified by 
 * .def files.  This supports the ANT build method.
 **/
public class DefRunner {
  /**
   * Run the code generator against the given file and a command line
   **/
  public static void runDef(String defile, String argline) {
    List<String> argx = explode(argline, " ");
    
    String classname = (String) argx.get(0);
    
    List<String> nargs = new ArrayList<String>();
    for (String a : argx) {
      nargs.add(a);
    }
    nargs.add(defile);          // last argument is always the def file
    
    String[] args = (String[]) nargs.toArray(new String[nargs.size()]);
    
    // prepend the standard build prefix when it is probably wrong.
    if (classname.indexOf('.')==-1) {
      classname = "org.cougaar.tools.build."+classname;
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
  
  private static final List<String> explode(String s, String seps) {
    StringTokenizer tokens = new StringTokenizer(s, seps);
    List<String> v = new ArrayList<String>();
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
