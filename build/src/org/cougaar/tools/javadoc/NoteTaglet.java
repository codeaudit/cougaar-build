package org.cougaar.tools.javadoc;
//  rip-off of the ToDo taglet with minor changes.

/*
 * @(#)NoteTaglet.java	1.4 01/09/21
 *
 * Copyright 1997-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A sample Taglet representing @note. This tag can be used in any kind of
 * {@link com.sun.javadoc.Doc}.  It is not an inline tag. The text is displayed
 * in red to remind the developer to perform a task.  For
 * example, "@note This is important!" would be shown as:
 * <DL>
* <DT>
* <B>To Do:</B>
* <DD><table cellpadding=2 cellspacing=0><tr><td bgcolor="red">This is important!
 * </td></tr></table></DD>
* </DL>
*
 * @since 1.4
 */

public class NoteTaglet implements Taglet {
    
  private String NAME = "note";
  private String HEADER = "Note:";
    
  /**
   * Return the name of this custom tag.
   */
  public String getName() {
    return NAME;
  }
    
  /**
   * Will return true since this <code>NoteTaglet</code>
   * can be used in field documentation.
   * @return true since this <code>NoteTaglet</code>
   * can be used in field documentation and false
   * otherwise.
   */
  public boolean inField() {
    return true;
  }

  /**
   * Will return true since this <code>NoteTaglet</code>
   * can be used in constructor documentation.
   * @return true since this <code>NoteTaglet</code>
   * can be used in constructor documentation and false
   * otherwise.
   */
  public boolean inConstructor() {
    return true; 
  }
    
  /**
   * Will return true since this <code>NoteTaglet</code>
   * can be used in method documentation.
   * @return true since this <code>NoteTaglet</code>
   * can be used in method documentation and false
   * otherwise.
   */
  public boolean inMethod() {
    return true;
  }
    
  /**
   * Will return true since this <code>NoteTaglet</code>
   * can be used in method documentation.
   * @return true since this <code>NoteTaglet</code>
   * can be used in overview documentation and false
   * otherwise.
   */
  public boolean inOverview() {
    return true;
  }

  /**
   * Will return true since this <code>NoteTaglet</code>
   * can be used in package documentation.
   * @return true since this <code>NoteTaglet</code>
   * can be used in package documentation and false
   * otherwise.
   */
  public boolean inPackage() {
    return true;
  }

  /**
   * Will return true since this <code>NoteTaglet</code>
   * can be used in type documentation (classes or interfaces).
   * @return true since this <code>NoteTaglet</code>
   * can be used in type documentation and false
   * otherwise.
   */
  public boolean inType() {
    return true;
  }
    
  /**
   * Will return false since this <code>NoteTaglet</code>
   * is not an inline tag.
   * @return false since this <code>NoteTaglet</code>
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
    NoteTaglet tag = new NoteTaglet();
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
    return "<DT><B>" + HEADER + "</B><DD>"
      + "<table cellpadding=2 cellspacing=0><tr><td bgcolor=\"red\">"
      + tag.text() 
      + "</td></tr></table></DD>\n";
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
    String result = "\n<DT><B>" + HEADER + "</B><DD>";
    result += "<table cellpadding=2 cellspacing=0><tr><td bgcolor=\"red\">";
    for (int i = 0; i < tags.length; i++) {
      if (i > 0) {
        result += ", ";
      }
      result += tags[i].text();
    }
    return result + "</td></tr></table></DD>\n";
  }
}


