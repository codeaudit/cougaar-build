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

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.util.*;


class PGParser {

  public static final String DEFAULT_FILENAME = "properties.def";
  public static final String TIMEPHASED = "timephased";

  private boolean isVerbose = false;
  private InputStream inputStream = null;
  


  public PGParser(boolean isVerbose) {
    this.isVerbose = isVerbose;
  }

  public PGParser(InputStream input, boolean isVerbose) {
    this.isVerbose = isVerbose;
    inputStream = input;
  }
  
  Hashtable table = new Hashtable();
  public boolean hasContext(String context) {
    return (table.get(context) != null);
  }

  public Hashtable getContext(String context) {
    Object o = table.get(context);
    Hashtable ct = null;
    if (o instanceof Hashtable) {
      ct = (Hashtable) o;
    } else {
      ct = new Hashtable();
      table.put(context, ct);
    }
    return ct;
  }
  
  public void put(String context, String key, String value) {
    Hashtable ct = getContext(context);
    ct.put(key, value);
  }
  
  public String get(String context, String key) {
    Hashtable ct = getContext(context);
    return (String)ct.get(key);
  }
  
  public Enumeration getContexts() {
    return table.keys();
  }
  
  public void parse() throws IOException {
    parse(inputStream);
  }

  public void parse(InputStream s) throws IOException {
    InputStreamReader isr = new InputStreamReader(s);
    BufferedReader br = new BufferedReader(isr);
    String context = "global";
    int i;
    
    getContext(context);
    for (String line = br.readLine(); line != null; line=br.readLine()) {
      line = line.trim();
      if (line.length() == 0 || line.startsWith(";")) {
        // empty or comment line
      }  else {
        while (line.endsWith("\\")) {
          String more = br.readLine();
          if (more == null) throw new IOException("Unexpected EOF");
          line = line.substring(0,line.length()-1)+more.trim();
        }
        if (line.startsWith("[") && line.endsWith("]")) { // new context
          context = line.substring(1,line.length()-1);
          debug("Parsing PropertyGroup \""+context+"\"");
          if (hasContext(context)) {
            System.err.println("Multiple definition of PropertyGroup "+context);
          }
          getContext(context);  // setup the new context
        }
        else if ( (i = line.indexOf("=")) >= 0) { // key/value pair
          String key = line.substring(0, i);
          String value = line.substring(i+1, line.length());
          put(context, key, value);
        } 
        else {
          debug("Bad line \""+line+"\"");
        }
      }
    }
  }

  public void debug(String s) {
    if (isVerbose)
      System.err.println(s);
  }

}






