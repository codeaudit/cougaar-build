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
  public static final String HAS_RELATIONSHIPS = "hasrelationships";
  public static final String LOCAL = "local";

  public static final String PG_SOURCE = "source";
  public static final String PRIMARY = "primary";
  public static final String INCLUDED = "included";

  //private boolean isVerbose = false;
  private boolean isVerbose = true;
  
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
    parse(s, true);
  }

  public void parse(InputStream s, boolean top) throws IOException {
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
        if (line.startsWith("includedefs")) {
          i = line.indexOf("=");
          if (i > 0) {
            String fileName = line.substring(i + 1, line.length()).trim();
            InputStream stream = getClass().getClassLoader().getResourceAsStream(fileName);
	    try {
	      parse(stream, false);
	    } catch (Exception e) {
	      System.err.println ("Could not find resource " + fileName + 
				  " referenced by includedefs on classpath.  Aborting.");
	      System.exit (-1); // something better to do here than exit?  should we continue?
	    }
	    if (stream != null)
	      stream.close();
          } else {
            debug("Bad line \""+line+"\"");
          }
        }

        if (line.startsWith("[") && line.endsWith("]")) { // new context
          context = line.substring(1,line.length()-1);
          debug("Parsing PropertyGroup \""+context+"\"");
          if (hasContext(context)) {
            System.err.println("Multiple definition of PropertyGroup "+context);
          }
          getContext(context);  // setup the new context
          if (top) {
            put(context, PG_SOURCE, PRIMARY);
          } else {
            put(context, PG_SOURCE, INCLUDED);
          }
        }
        else if ( (i = line.indexOf("=")) >= 0) { // key/value pair
          //Don't include global values from included files.
          if (top || !(context.equals("global"))) {
            String key = line.substring(0, i);
            String value = line.substring(i+1, line.length());

            put(context, key, value);
          }
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

  public Object getValue(String context, String key, boolean lookGlobal, 
                         Object defaultValue) {
    Object value = get(context, key);
    if ((value == null) && (lookGlobal)) {
      value = get("global", key);
    }
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  public boolean getValueFlag(String context, String key, boolean lookGlobal, 
                              boolean defaultValue) {
    Object value = getValue(context, key, lookGlobal, 
                            new Boolean(defaultValue));
    
    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    } else {
      return Boolean.valueOf((String) value).booleanValue();
    }
  }

}






