package org.cougaar.tools.javadoc;
/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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
 * 
 * Based on ToDo example taglet.
 */

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A simple Taglet representing @property. 
 */

public class PropertyTaglet implements Taglet {
    
  private String NAME = "property";
  private String SINGULAR_HEADER = "System Property:";
  private String PLURAL_HEADER = "System Properties:";
    
  /**
   * Return the name of this custom tag.
   */
  public String getName() {
    return NAME;
  }
    
  /**
   * Will return true since this <code>ToDoTaglet</code>
   * can be used in field documentation.
   * @return true since this <code>ToDoTaglet</code>
   * can be used in field documentation and false
   * otherwise.
   */
  public boolean inField() {
    return true;
  }

  /**
   * Will return true since this <code>ToDoTaglet</code>
   * can be used in constructor documentation.
   * @return true since this <code>ToDoTaglet</code>
   * can be used in constructor documentation and false
   * otherwise.
   */
  public boolean inConstructor() {
    return true; 
  }
    
  /**
   * Will return true since this <code>ToDoTaglet</code>
   * can be used in method documentation.
   * @return true since this <code>ToDoTaglet</code>
   * can be used in method documentation and false
   * otherwise.
   */
  public boolean inMethod() {
    return true;
  }
    
  /**
   * Will return true since this <code>ToDoTaglet</code>
   * can be used in method documentation.
   * @return true since this <code>ToDoTaglet</code>
   * can be used in overview documentation and false
   * otherwise.
   */
  public boolean inOverview() {
    return true;
  }

  /**
   * Will return true since this <code>ToDoTaglet</code>
   * can be used in package documentation.
   * @return true since this <code>ToDoTaglet</code>
   * can be used in package documentation and false
   * otherwise.
   */
  public boolean inPackage() {
    return true;
  }

  /**
   * Will return true since this <code>ToDoTaglet</code>
   * can be used in type documentation (classes or interfaces).
   * @return true since this <code>ToDoTaglet</code>
   * can be used in type documentation and false
   * otherwise.
   */
  public boolean inType() {
    return true;
  }
    
  /**
   * Will return false since this <code>ToDoTaglet</code>
   * is not an inline tag.
   * @return false since this <code>ToDoTaglet</code>
   * is not an inline tag.
   */
    
  public boolean isInlineTag() {
    return false;
  }
    
  /**
   * Register this Taglet.
   * @param tagletMap  the map to register this tag to.
   */
  public static void register(Map tagletMap) {
    PropertyTaglet tag = new PropertyTaglet();
    Taglet t = (Taglet) tagletMap.get(tag.getName());
    if (t != null) {
      tagletMap.remove(tag.getName());
    }
    tagletMap.put(tag.getName(), tag);
  }

  /**
   * Given the <code>Tag</code> representation of this custom
   * tag, return its string representation.
   * @param tag he <code>Tag</code> representation of this custom tag.
   */
  public String toString(Tag tag) {
    Tuple t = new Tuple(tag);
    return "<DT><B>" + SINGULAR_HEADER + "</B><DD>"
      + "<table cellpadding=2 cellspacing=0><tr valign=\"top\">"+
      "<td>"+ t.param +"</td>"+
      "<td>"+ t.text +"</td>"+
      "</tr></table></DD>\n";
  }
    
  /**
   * Given an array of <code>Tag</code>s representing this custom
   * tag, return its string representation.
   * @param tags the array of <code>Tag</code>s representing of this custom tag.
   */
  public String toString(Tag[] tags) {
    if (tags.length == 0) {
      return null;
    }
    String result = "\n<DT><B>" + (tags.length == 1 ? SINGULAR_HEADER : PLURAL_HEADER) + "</B><DD>";
    result += "<table cellpadding=2 cellspacing=0>";
    for (int i = 0; i < tags.length; i++) {
      Tuple t = new Tuple((Tag)tags[i]);
      result += "<tr valign=\"top\"><td>"+ t.param +"</td>"+
        "<td>"+ t.text +"</td></tr>";
    }
    return result + "</table></DD>\n";
  }

  public static class Tuple implements Comparable
  {
    public Tag tag;
    public String param = null;
    public String text = null;
    public final static String white = " \t\n\r"; // whitespace chars
    public Tuple(Tag tag) {
      this.tag = tag;

      String tt = tag.text();
      int i;
      int l = tt.length();
      int state =0;             // what are we looking for?
      int p0=-1,p1=-1,t0=-1;

      // scan the tag text
      for (i=0;i<l;i++) {
        char c = tt.charAt(i);
        boolean isWhite = (white.indexOf(c)>=0);
        switch (state) {
        case 0:                 // looking for param start
          if (!isWhite) {
            p0=i;
            state=1;
          }
          break;
        case 1:                 // looking for param end
          if (isWhite) {
            p1 = i;
            state=2;
          }
          break;
        case 2:                 // looking for text start
          if (!isWhite) {
            t0 = i;
            state=3;
          }
          break;
        default:                // shouldn't get here
          break;
        }
        if (state==3) break;
      }

      if (t0>=0) {
        param = tt.substring(p0,p1);
        text = tt.substring(t0);
      } else {
      }
    }

    public int compareTo(Object o) {
      if (!(o instanceof Tuple)) return -1;
      Tuple t = (Tuple) o;
      int v = param.compareTo(t.param);
      if (v != 0) return v;

      return 0;
    }        
  }

}


