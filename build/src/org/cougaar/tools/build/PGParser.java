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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;


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






