package org.cougaar.tools.javadoc;

/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

// note that this requires that the tools.jar from jdk be present at compile-time.

// basic packages
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.DocletAbortException;
import com.sun.tools.doclets.standard.ConfigurationStandard;
import com.sun.tools.doclets.standard.HtmlStandardWriter;
import com.sun.tools.doclets.standard.Standard;

/**
 * An extension of the "Standard" Doclet for generating files which 
 * additionally outputs a list of parameters (usually System Properties) 
 * that control behavior of various classes and methods.
 *
 * usage: javadoc -doclet org.cougaar.tools.javadoc.Cougaar foo.java ...
 *
 * @property org.cougaar.tools.javadoc.dummy An ignored property mentioned
 * here only to illustrate the sort thing found by this doclet.
 **/

// javac -d . -g -classpath /usr/local/java/jdk/lib/tools.jar:. Cougaar.java

public class CougaarDoclet
  extends Standard
{

  /** just like Standard except creates a Cougaar instance instead of
   * a Standard Doclet instance.
   **/
  public static boolean start(RootDoc root) throws IOException {
    try { 
      root = hackRootDoc(root);
      configuration().setOptions(root);
      (new CougaarDoclet()).startGeneration(root);
    } catch (DocletAbortException exc) {
      return false; // message has already been displayed
    }
    return true;
  }

  protected void startGeneration(RootDoc root) throws DocletAbortException {
    super.startGeneration(root);

    ParameterListWriter.generate(configuration, root);
  }

  private static RootDoc hackRootDoc(final RootDoc root) {
    InvocationHandler handler = new InvocationHandler() {
        public Object invoke(Object proxy, Method method, Object[] args)
          throws Throwable
        {
          // drop overly verbose notices.  Should probably add a -verbose flag.
          if ("printNotice".equals(method.getName())) {
            return null;
          } else {
            return method.invoke(root,args);
          }
        }
      };
    RootDoc nrd = (RootDoc) Proxy.newProxyInstance(RootDoc.class.getClassLoader(),
                                                   new Class[] { RootDoc.class },
                                                   handler);
    
    return nrd;
  }

}

class ParameterListWriter extends HtmlStandardWriter { // was HtmlStandardWriter

  public final static String PARAMETERFILE = "Parameters.html";

  public ParameterListWriter(ConfigurationStandard cs, String filename) throws IOException {
    super(cs, filename);
  }

  /**
   * Generate the package index.
   *
   * @param root the root of the doc tree.
   */
  public static void generate(ConfigurationStandard cs, RootDoc root) throws DocletAbortException {
    ParameterListWriter packgen;
    String filename = PARAMETERFILE;
    try {
      packgen = new ParameterListWriter(cs, filename);
      packgen.generateParameterListFile(root);
      packgen.close();
    } catch (IOException exc) {
      Standard.configuration().standardmessage.error("doclet.exception_encountered", 
                                                     exc.toString(), filename);
      throw new DocletAbortException();
    }
  }

  protected void generateParameterListFile(RootDoc root) {
    buildParameterInfo(root);
    
    // Note that this line includes some SCRIPT tags that the default Java HTML renderer
    // can't handle. Ugly.
    printHtmlHeader("Parameter List");
    println("Catalog of System Properties");
    p();
    
    ul();
    for (Iterator it = parameters.iterator(); it.hasNext(); ) {
      Tuple t = (Tuple)it.next();
      //String text = t.tag.text();
      Doc d = t.doc;
      String param = t.param;   // parse out param from text
      String info = t.text;       // remainder of text
      li();
      bold();
      println(param);
      boldEnd();
      println(info);
      print(" (See ");
      if (d instanceof ClassDoc) {
        print("class ");
        printClassLink((ClassDoc) d);            
      } else if (d instanceof MemberDoc) {
        MemberDoc md = (MemberDoc) d;
        print(md.qualifiedName());
        print(" in ");
        printClassLink((ClassDoc) md.containingClass());
      }
      println(")");
      //liEnd();
    }
    ulEnd();

    printBodyHtmlEnd();
  }

  private List parameters = new ArrayList();

  private void buildParameterInfo(RootDoc root) {
    ClassDoc[] classes = root.classes();
    for (int i = 0; i < classes.length; i++) {
      ClassDoc cd = classes[i];
      collect(cd);
      collect(cd.fields());
      collect(cd.methods());
      collect(cd.constructors());
    }
    Collections.sort(parameters);
  }

  private void collect(Doc doc) {
    Tag[] pts = doc.tags("property");
    for (int i=0;i<pts.length;i++) {
      parameters.add(new Tuple(doc, pts[i]));
    }
  }
  private void collect(Doc[] docs) {
    for (int i=0;i<docs.length;i++) {
      collect(docs[i]);
    }
  }
}

class EventListWriter extends HtmlStandardWriter { // was HtmlStandardWriter

  public final static String EVENTFILE = "CougaarEvents.html";

  public EventListWriter(ConfigurationStandard cs, String filename) throws IOException {
    super(cs, filename);
  }

  /**
   * Generate the package index.
   *
   * @param root the root of the doc tree.
   */
  public static void generate(ConfigurationStandard cs, RootDoc root) throws DocletAbortException {
    EventListWriter packgen;
    String filename = EVENTFILE;
    try {
      packgen = new EventListWriter(cs, filename);
      packgen.generateEventListFile(root);
      packgen.close();
    } catch (IOException exc) {
      Standard.configuration().standardmessage.error("doclet.exception_encountered", 
                                                     exc.toString(), filename);
      throw new DocletAbortException();
    }
  }

  protected void generateEventListFile(RootDoc root) {
    buildEventInfo(root);
    
    // Note that this line includes some SCRIPT tags that the default Java HTML renderer
    // can't handle. Ugly.
    printHtmlHeader("Cougaar Event List");
    println("Catalog of Cougaar Events");
    p();
    
    ul();
    for (Iterator it = events.iterator(); it.hasNext(); ) {
      Tuple t = (Tuple)it.next();
      //String text = t.tag.text();
      Doc d = t.doc;
      String param = t.param;   // parse out param from text
      String info = t.text;       // remainder of text
      li();
      bold();
      println(param);
      boldEnd();
      println(info);
      print(" (See ");
      if (d instanceof ClassDoc) {
        print("class ");
        printClassLink((ClassDoc) d);            
      } else if (d instanceof MemberDoc) {
        MemberDoc md = (MemberDoc) d;
        print(md.qualifiedName());
        print(" in ");
        printClassLink((ClassDoc) md.containingClass());
      }
      println(")");
      //liEnd();
    }
    ulEnd();

    printBodyHtmlEnd();
  }

  private List events = new ArrayList();

  private void buildEventInfo(RootDoc root) {
    ClassDoc[] classes = root.classes();
    for (int i = 0; i < classes.length; i++) {
      ClassDoc cd = classes[i];
      collect(cd);
      collect(cd.fields());
      collect(cd.methods());
      collect(cd.constructors());
    }
    Collections.sort(events);
  }

  private void collect(Doc doc) {
    Tag[] pts = doc.tags("event");
    for (int i=0;i<pts.length;i++) {
      events.add(new Tuple(doc, pts[i]));
    }
  }
  private void collect(Doc[] docs) {
    for (int i=0;i<docs.length;i++) {
      collect(docs[i]);
    }
  }
}

class Tuple 
  implements Comparable
{
  public Doc doc;
  public Tag tag;
  public String param = null;
  public String text = null;
  public final static String white = " \t\n\r"; // whitespace chars
  public Tuple(Doc doc, Tag tag) {
    this.doc = doc; 
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
        Standard.configuration().standardmessage.
          error("doclet.parameter_syntax_error", 
                "StateError: "+doc.toString());
        break;
      }
      if (state==3) break;
    }

    if (t0>=0) {
      param = tt.substring(p0,p1);
      text = tt.substring(t0);
    } else {
      Standard.configuration().standardmessage.
        error("doclet.parameter_syntax_error", 
              doc.toString());
    }
  }

  public int compareTo(Object o) {
    if (!(o instanceof Tuple)) return -1;
    Tuple t = (Tuple) o;
    int v = param.compareTo(t.param);
    if (v != 0) return v;

    return doc.compareTo(t.doc);
  }        
}
