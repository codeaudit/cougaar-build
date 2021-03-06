package org.cougaar.tools.javadoc;
/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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
 * 
 * Based on ToDo example taglet.
 */

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A simple Taglet representing @property. 
 */

public class EventTaglet implements Taglet {
    
  private String NAME = "event";
  private String SINGULAR_HEADER = "Cougaar Event:";
  private String PLURAL_HEADER = "Cougaar Events:";
    
  /**
   * Return the name of this custom tag.
   */
  public String getName() {
    return NAME;
  }
    
  /**
   * Will return true since this <code>EventTaglet</code>
   * can be used in field documentation.
   * @return true since this <code>EventTaglet</code>
   * can be used in field documentation and false
   * otherwise.
   */
  public boolean inField() {
    return true;
  }

  /**
   * Will return true since this <code>EventTaglet</code>
   * can be used in constructor documentation.
   * @return true since this <code>EventTaglet</code>
   * can be used in constructor documentation and false
   * otherwise.
   */
  public boolean inConstructor() {
    return true; 
  }
    
  /**
   * Will return true since this <code>EventTaglet</code>
   * can be used in method documentation.
   * @return true since this <code>EventTaglet</code>
   * can be used in method documentation and false
   * otherwise.
   */
  public boolean inMethod() {
    return true;
  }
    
  /**
   * Will return true since this <code>EventTaglet</code>
   * can be used in method documentation.
   * @return true since this <code>EventTaglet</code>
   * can be used in overview documentation and false
   * otherwise.
   */
  public boolean inOverview() {
    return true;
  }

  /**
   * Will return true since this <code>EventTaglet</code>
   * can be used in package documentation.
   * @return true since this <code>EventTaglet</code>
   * can be used in package documentation and false
   * otherwise.
   */
  public boolean inPackage() {
    return true;
  }

  /**
   * Will return true since this <code>EventTaglet</code>
   * can be used in type documentation (classes or interfaces).
   * @return true since this <code>EventTaglet</code>
   * can be used in type documentation and false
   * otherwise.
   */
  public boolean inType() {
    return true;
  }
    
  /**
   * Will return false since this <code>EventTaglet</code>
   * is not an inline tag.
   * @return false since this <code>EventTaglet</code>
   * is not an inline tag.
   */
    
  public boolean isInlineTag() {
    return false;
  }
    
  /**
   * Register this Taglet.
   * @param tagletMap  the map to register this tag to.
   */
  @SuppressWarnings("unchecked")
  public static void register(Map tagletMap) {
    EventTaglet tag = new EventTaglet();
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
      Tuple t = new Tuple(tags[i]);
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


