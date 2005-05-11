package org.cougaar.tools.javadoc;

/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

import com.sun.javadoc.*;
import com.sun.tools.doclets.formats.html.*;
import com.sun.tools.doclets.internal.toolkit.*;
import com.sun.tools.doclets.internal.toolkit.util.*;

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

public class CougaarDoclet extends HtmlDoclet
{

  /** just like Standard except creates a Cougaar instance instead of
   * a Standard Doclet instance.
   **/
  public static boolean start(RootDoc root) {
    CougaarDoclet doclet = new CougaarDoclet();
    return doclet.start(doclet, root);
  }

  protected void generateOtherFiles(RootDoc root, ClassTree classtree) throws Exception {
    super.generateOtherFiles(root,classtree);

    generateParameterList(root);
  }

  protected void generateParameterList(RootDoc root) throws Exception {
    ParameterListWriter packgen;
    packgen = new ParameterListWriter((ConfigurationImpl)configuration, ParameterListWriter.PARAMETERFILE);
    packgen.generateParameterListFile(root);
    packgen.close();
  }

  private static class ParameterListWriter extends com.sun.tools.doclets.formats.html.HtmlDocletWriter {
    public final static String PARAMETERFILE = "Parameters.html";

    public ParameterListWriter(ConfigurationImpl cs, String filename) throws IOException {
      super(cs, filename);
      this.cs = cs;
    }

    protected Configuration cs;
    public Configuration configuration() { return cs; }


    protected void generateParameterListFile(RootDoc root) {
      buildParameterInfo(root);
    
      // Note that this line includes some SCRIPT tags that the default Java HTML renderer
      // can't handle. Ugly.
      printFramesetHeader("Parameter List");
      println("Catalog of System Properties");
      p();
    
      ul();
      int i=0;
      for (Iterator it = parameters.iterator(); it.hasNext(); i++) {
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
          printLink(new LinkInfoImpl(i, (ClassDoc) d));
        } else if (d instanceof MemberDoc) {
          MemberDoc md = (MemberDoc) d;
          print(md.qualifiedName());
          print(" in ");
          printLink(new LinkInfoImpl(i, (ClassDoc) md.containingClass()));
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

  private static class Tuple implements Comparable {
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

      return doc.compareTo(t.doc);
    }        
  }
}
