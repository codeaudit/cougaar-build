package org.cougaar.tools.javadoc;

/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
import com.sun.tools.doclets.*;
import com.sun.javadoc.*;
import java.util.*;
import java.io.*;

// Standard doclet 
import com.sun.tools.doclets.standard.*;

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
      configuration().setOptions(root);
      (new CougaarDoclet()).startGeneration(root);
    } catch (DocletAbortException exc) {
      return false; // message has already been displayed
    }
    return true;
  }

  protected void startGeneration(RootDoc root) throws DocletAbortException {
    super.startGeneration(root);

    ParameterListWriter.generate(root);
  }
}

class ParameterListWriter extends HtmlStandardWriter {

  public final static String PARAMETERFILE = "Parameters.html";

  public ParameterListWriter(String filename) throws IOException {
    super(filename);
  }

  /**
   * Generate the package index.
   *
   * @param root the root of the doc tree.
   */
  public static void generate(RootDoc root) throws DocletAbortException {
    ParameterListWriter packgen;
    String filename = PARAMETERFILE;
    try {
      packgen = new ParameterListWriter(filename);
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
    
    printHeader("Paramater List");
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
      em();
      println(param);
      emEnd();
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
    //sortParameterInfo();
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

  public class Tuple {
    public Doc doc;
    public Tag tag;
    public String param = null;
    public String text = null;
    public Tuple(Doc doc, Tag tag) {
      this.doc = doc; 
      this.tag = tag;

      // this is a huge crock.  We'd be better off with a real parser.
      StringReader sr = new StringReader(tag.text());
      StreamTokenizer st = new StreamTokenizer(sr);
      int t;

      try {
        if ((t = st.nextToken()) == StreamTokenizer.TT_WORD) {
          param = st.sval;

          StringBuffer sb = null;
          while ((t = st.nextToken()) != StreamTokenizer.TT_EOF) {
            if (sb == null) {
              sb = new StringBuffer();
            } else {
              sb.append(" ");
            }

            if (t == StreamTokenizer.TT_WORD) {
              sb.append(st.sval);
            } 
            // other tokens are ignored and we aren't parsing numbers.
          }
          if (sb != null) text = sb.toString();
        } else {
          Standard.configuration().standardmessage.
            error("doclet.parameter_syntax_error", 
                  doc.toString());
        }
      } catch (IOException ioe) {
        Standard.configuration().standardmessage.
          error("doclet.exception_encountered",
                ioe.toString(), PARAMETERFILE);
      }
    }
  }
}
