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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PGWriter extends WriterBase {
  private final static String NewTimeSpanText =
    "//NewTimeSpan implementation\n" +
    "  private long theStartTime = TimeSpan.MIN_VALUE;\n" +
    "  public long getStartTime() {\n" +
    "    return theStartTime;\n" +
    "  }\n" +
    "\n" +
    "  private long theEndTime = TimeSpan.MAX_VALUE;\n" +
    "  public long getEndTime() {\n" +
    "    return theEndTime;\n" +
    "  }\n" +
    "\n" +
    "  public void setTimeSpan(long startTime, long endTime) {\n" +
    
    "    if ((startTime >= MIN_VALUE) && \n" +
    "        (endTime <= MAX_VALUE) &&\n" +
    "        (endTime >= startTime + EPSILON)) {\n" +
    "      theStartTime = startTime;\n" +
    "      theEndTime = endTime;\n" +
    "    } else {\n" +
    "      throw new IllegalArgumentException();\n" +
    "    }\n" +
    "  }\n" +
    "\n" +
    "  public void setTimeSpan(TimeSpan timeSpan) {\n" +
    "    setTimeSpan(timeSpan.getStartTime(), timeSpan.getEndTime());\n" +
    "  }\n";
  
  class Writer {
    PrintWriter filelist = null;
    PGParser p;
    public Writer(PGParser p) {
      this.p = p;
      try {
        filelist = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getTargetDir(),getGenFileName()))));
        noteFile(getGenFileName());
      } catch (IOException ioe) { throw new RuntimeException(); }
    }
    public void noteFile(String s) {
      println(filelist,s);
    }
    public void done() {
      filelist.close();
    }
    
    void grokGlobal() {
    }
    
    /** convert a string like foo_bar_baz to FooBarBaz **/
    String toClassName(String s) {
      StringBuffer b = new StringBuffer();
      boolean isFirst = true;
      
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == '_') {
          isFirst= true;
        } else {
          if (isFirst && Character.isLowerCase(c))
            c = Character.toUpperCase(c);
          isFirst=false;
          b.append(c);
        }
      }
      
      return b.toString();
    }
    
    /** convert a string like foo_bar_baz to fooBarBaz **/
    String toVariableName(String s) {
      StringBuffer b = new StringBuffer();
      boolean isFirst = true;
      boolean isStart = true;
      
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == '_') {
          isFirst= true;
        } else {
          if (isStart && isFirst && Character.isLowerCase(c))
            c = Character.toUpperCase(c);
          isFirst=false; isStart=false;
          b.append(c);
        }
      }
      
      return b.toString();
    }
    
    /** convert a string like foo_bar_baz to FOO_BAR_BAZ **/
    String toConstantName(String s) {
      StringBuffer b = new StringBuffer();
      
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (Character.isLowerCase(c))
          c = Character.toUpperCase(c);
        b.append(c);
      }
      
      return b.toString();
    }
    
    List<String> explode(String s) { return explode(s, ' '); }
    List<String> explode(String s, char x) {
      List<String> r = new ArrayList<String>();
      int last = 0;
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == x) {
          if (i>last)
            r.add(s.substring(last,i));
          last = i+1;
        }
      }
      if (s.length()>last) r.add(s.substring(last));
      return r;
    }
    
    String findPackage(String context) {
      String pkg = p.get(context, "package");
      if (pkg == null)
        pkg = p.get("global", "package");
      if (pkg == null)
        pkg = "org.cougaar.planning.ldm.asset";
      return pkg;
    }
    
    void doPackaging(PrintWriter out, String context) {
      println(out,"package "+findPackage(context)+";");
      println(out);
      println(out,"import org.cougaar.planning.ldm.measure.*;");
      println(out,"import org.cougaar.planning.ldm.asset.*;");
      println(out,"import org.cougaar.planning.ldm.plan.*;");
      if (isTimePhased(context)) {
        println(out,"import org.cougaar.util.TimeSpan;");
      }
      println(out,"import java.util.*;");
      println(out);
      doImports(out, "global");
      if (!context.equals("global")) 
        doImports(out, context);
      
      if (hasRelationships(context)) {
        println(out,"import org.cougaar.planning.ldm.plan.HasRelationships;");
        println(out,"import org.cougaar.planning.ldm.plan.RelationshipSchedule;");
      }
    }
    void doImports(PrintWriter out, String context) {
      String importstr=(String)p.get(context,"import");
      if (importstr!=null) {
        for (String ve : explode(importstr, ',')) {
          println(out,"import "+ve+";");
        }
      }      
      println(out);
    }
    
    /** return DQ specification of context, first looking at local context,
     * then global, then default (true).
     **/
    boolean hasDQ(String context) {
      return p.getValueFlag(context, "hasDataQuality", true, true);
    }
    
    /** return HasRelationships specification of context, looking at local
     *  context, then global, then default (false).
     **/
    boolean hasRelationships(String context) {
      return p.getValueFlag(context, PGParser.HAS_RELATIONSHIPS, true, false);
    }
    
    /** return TimePhased specification of context, looking at local
     *  context, then global, then default (false).
     **/
    boolean isTimePhased(String context) {
      return p.getValueFlag(context, PGParser.TIMEPHASED, true, false);
    }
    
    /** return Local specification of context, looking at local
     *  context, then global, then default (false).
     **/
    boolean isLocal(String context) {
      return p.getValueFlag(context, PGParser.LOCAL, true, false);
    }
    
    String getExtraInterface(String context) {
      return (String) p.getValue(context, "interface", false, null);
    }
    
    void writeGetterIfc(PrintWriter out, String context, String className) {
      println(out,"/** Primary client interface for "+className+".");
      String doc = (String)p.get(context,"doc");
      if (doc != null)
        println(out," * "+doc);
      println(out," *  @see New"+className);
      println(out," *  @see "+className+"Impl");
      println(out," **/");
      println(out);
      doPackaging(out, context);
      
      // figure out what we're supposed to extend
      
      { 
        String dq = (hasDQ(context)?", org.cougaar.planning.ldm.dq.HasDataQuality":"");
        String local = (isLocal(context)?", LocalPG":"");
        String timePhased = (isTimePhased(context)?"TimePhased":"");
        String xifc = getExtraInterface(context);
        String xifcs = (xifc==null)?"":(", "+xifc);
        println(out,"public interface "+className+" extends "+timePhased+"PropertyGroup"+dq+local+xifcs+" {");
      }
      
      // declare the slots
      for (String slotspec : getAllSlotSpecs(context)) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        String etype = null;
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
          etype = ct.etype;
        }
        String clname = toClassName(name);
        
        String slotdoc = (String)p.get(context, name+".doc");
        String depp = (String)p.get(context,name+".deprecated");
        if (depp != null) {
          if (slotdoc != null) 
            slotdoc=slotdoc+"\n   * @deprecated "+depp;
          else
            slotdoc="@deprecated "+depp;
        }
        if (slotdoc!=null) 
          println(out,"  /** "+slotdoc+" **/");
        if (as!=null) {
          println(out,"  "+type+" "+as.getterSpec()+";");
        } else {
          println(out,"  "+type+" get"+clname+"();");
        }
        
        // extra collection type api
        if (etype != null) {
          println(out,"  /** test to see if an element is a member of the "+name+" Collection **/");
          println(out,"  boolean in"+clname+"("+etype+" element);");
          println(out);
          
          println(out,"  /** array getter for beans **/");
          println(out,"  "+etype+"[] get"+clname+"AsArray();");
          println(out);
          
          println(out,"  /** indexed getter for beans **/");
          println(out,"  "+etype+" getIndexed"+clname+"(int index);");
          println(out);
        }
        
        if (as != null) {
          println(out,as.handlerClassDef());
        }
      }      
      println(out);
      
      
      // delegation handling
      for (Argument dv : getAllDelegateSpecs(context)) {
        for (DelegateSpec ds : parseDelegateSpecs(p.get(context, dv.name+".delegate"))) {
          String slotdoc = (String)p.get(context, ds.name+".doc");
          if (slotdoc!=null) 
            println(out,"  /** "+slotdoc+" **/");
          
          println(out,"  "+ds.type+" "+ds.name+
                  "("+unparseArguments(ds.args,true)+");");
        }
      }
      
      println(out,"  // introspection and construction");
      println(out,"  /** the method of factoryClass that creates this type **/");
      println(out,"  String factoryMethod = \"new"+className+"\";");
      println(out,"  /** the (mutable) class type returned by factoryMethod **/");
      println(out,"  String mutableClass = \""+
              findPackage(context)+".New"+className+"\";");
      println(out,"  /** the factory class **/");
      println(out,"  Class factoryClass = "+findPackage("global")+".PropertyGroupFactory.class;");
      
      println(out,"  /** the (immutable) class type returned by domain factory **/");
      println(out,"   Class primaryClass = "+
              findPackage(context)+"."+className+".class;");
      
      println(out,"  String assetSetter = \"set"+
              className+"\";");
      println(out,"  String assetGetter = \"get"+
              className+"\";");
      println(out,"  /** The Null instance for indicating that the PG definitely has no value **/");
      println(out,"  "+className+" nullPG = new Null_"+className+"();");
      writeNullClass(out, context, className);
      writeFutureClass(out, context, className);
      println(out,"}");
    }
    
    void writeNullClass(PrintWriter out, String context, String className) {
      println(out);
      // null class implementation
      println(out,"/** Null_PG implementation for "+className+" **/");
      println(out,"final class Null_"+className+"\n"+
              "  implements "+className+", Null_PG\n"+
      "{");
      
      // declare the slots
      for (String slotspec : getAllSlotSpecs(context)) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        String etype = null;
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
          etype = ct.etype;
        }
        String clname = toClassName(name);
        
        String slotdoc = null;
        String depp = (String)p.get(context,name+".deprecated");
        if (depp != null) {
          slotdoc="@deprecated "+depp;
        }
        if (slotdoc!=null) 
          println(out,"  /** "+slotdoc+" **/");
        if (as!=null) {
          println(out,"  public "+type+" "+as.getterSpec()+"{ throw new UndefinedValueException(); }");
        } else {
          println(out,"  public "+type+" get"+clname+"() { throw new UndefinedValueException(); }");
        }
        
        // extra collection type api
        if (etype != null) {
          println(out,"  public boolean in"+clname+"("+etype+" element) { return false; }");
          println(out,"  public "+etype+"[] get"+clname+"AsArray() { return null; }");
          println(out,"  public "+etype+" getIndexed"+clname+"(int index) { throw new UndefinedValueException(); }");
        }
      }      
      
      for (Argument dv : getAllDelegateSpecs(context)) {
        // define delegate getter and setter if non-automatic
        String autop = (String)p.get(context, dv.name+".auto");
        // if it isn't automatic, define the setter and getter
        if (!(autop != null && Boolean.valueOf(autop).booleanValue())) {
          println(out,"  public "+dv.type+" get"+toClassName(dv.name)+"() {\n"+
                  "    throw new UndefinedValueException();\n"+
          "  }");
          println(out,"  public void set"+toClassName(dv.name)+"("+dv.type+" _"+dv.name+") {\n"+
                  "    throw new UndefinedValueException();\n"+
          "  }");
        }
        
        for (DelegateSpec ds : parseDelegateSpecs(p.get(context, dv.name+".delegate"))) {
          println(out,"  public "+ds.type+" "+ds.name+
                  "("+unparseArguments(ds.args,true)+") {"+
                  " throw new UndefinedValueException(); "+
          "}");
        }
      }
      
      
      
      // TimePhased getters
      if (isTimePhased(context)) {
        println(out,"  public long getStartTime() { throw new UndefinedValueException(); }");
        println(out,"  public long getEndTime() { throw new UndefinedValueException(); }");
      }
      
      println(out,"  public boolean equals(Object object) { throw new UndefinedValueException(); }");
      
      println(out,"  public Object clone() throws CloneNotSupportedException {\n"+
              "    throw new CloneNotSupportedException();\n"+
      "  }");
      println(out,"  public NewPropertyGroup unlock(Object key) { return null; }");
      println(out,"  public PropertyGroup lock(Object key) { return null; }");
      println(out,"  public PropertyGroup lock() { return null; }");
      println(out,"  public PropertyGroup copy() { return null; }");
      println(out,"  public Class getPrimaryClass(){return primaryClass;}");
      println(out,"  public String getAssetGetMethod() {return assetGetter;}");
      println(out,"  public String getAssetSetMethod() {return assetSetter;}");
      println(out,"  public Class getIntrospectionClass() {\n"+
              "    return "+className+"Impl.class;\n"+
      "  }");
      
      // implement PG basic api - null never has DQ
      println(out);
      println(out,"  public boolean hasDataQuality() { return false; }");
      
      if (hasDQ(context)) {     // if the class has it, we need to implement the getter
        println(out,"  public org.cougaar.planning.ldm.dq.DataQuality getDataQuality() { return null; }");
      }
      println(out,"}");
      
    }
    
    void writeFutureClass(PrintWriter out, String context, String className) {
      println(out);
      // null class implementation
      println(out,"/** Future PG implementation for "+className+" **/");
      println(out,"final class Future\n"+
              "  implements "+className+", Future_PG\n"+
      "{");
      // declare the slots
      for (String slotspec : getAllSlotSpecs(context)) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        String etype = null;
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
          etype = ct.etype;
        }
        String clname = toClassName(name);
        
        String slotdoc = null;
        String depp = (String)p.get(context,name+".deprecated");
        if (depp != null) {
          slotdoc="@deprecated "+depp;
        }
        if (slotdoc!=null) 
          println(out,"  /** "+slotdoc+" **/");
        if (as!=null) {
          println(out,"  public "+type+" "+as.getterSpec()+"{\n"+
                  "    waitForFinalize();\n"+
                  "    return _real."+as.getterCall()+";\n"+
          "  }");
        } else {
          println(out,"  public "+type+" get"+clname+"() {\n"+
                  "    waitForFinalize();\n"+
                  "    return _real.get"+clname+"();\n"+
          "  }");
        }
        
        
        // extra collection type api
        if (etype != null) {
          println(out,"  public boolean in"+clname+"("+etype+" element) {\n"+
                  "    waitForFinalize();\n"+
                  "    return _real.in"+clname+"(element);\n"+
          "  }");
          println(out,"  public "+etype+"[] get"+clname+"AsArray() {\n"+
                  "    waitForFinalize();\n"+
                  "    return _real.get"+clname+"AsArray();\n"+
          "  }");
          println(out,"  public "+etype+" getIndexed"+clname+"(int index) {\n"+
                  "    waitForFinalize();\n"+
                  "    return _real.getIndexed"+clname+"(index);\n"+
          "  }");
        }
      }      
      
      println(out,"  public boolean equals(Object object) {\n"+
              "    waitForFinalize();\n"+
              "    return _real.equals(object);\n"+
      "  }");
      
      for (Argument dv : getAllDelegateSpecs(context)) {
        for (DelegateSpec ds : parseDelegateSpecs(p.get(context, dv.name+".delegate"))) {
          println(out,"  public "+ds.type+" "+ds.name+
                  "("+unparseArguments(ds.args,true)+") {\n"+
                  "    waitForFinalize();\n"+
                  "    "+ ("void".equals(ds.type)?"":"return ")+"_real."+ds.name+
                  "("+unparseArguments(ds.args,false)+");\n"+
          "  }");
        }
      }
      
      
      
      // TimePhased getters
      if (isTimePhased(context)) {
        println(out,"  public long getStartTime() {\n" + 
                "    waitForFinalize();\n"+
                "    return _real.getStartTime();\n"+
        "  }");
        println(out,"  public long getEndTime() {\n" + 
                "    waitForFinalize();\n"+
                "    return _real.getEndTime();\n"+
        "  }");
      }
      
      println(out,"  public Object clone() throws CloneNotSupportedException {\n"+
              "    throw new CloneNotSupportedException();\n"+
      "  }");
      println(out,"  public NewPropertyGroup unlock(Object key) { return null; }");
      println(out,"  public PropertyGroup lock(Object key) { return null; }");
      println(out,"  public PropertyGroup lock() { return null; }");
      println(out,"  public PropertyGroup copy() { return null; }");
      println(out,"  public Class getPrimaryClass(){return primaryClass;}");
      println(out,"  public String getAssetGetMethod() {return assetGetter;}");
      println(out,"  public String getAssetSetMethod() {return assetSetter;}");
      println(out,"  public Class getIntrospectionClass() {\n"+
              "    return "+className+"Impl.class;\n"+
      "  }");
      
      // Futures do not have data quality, though the replacement instance
      // may.
      println(out,"  public synchronized boolean hasDataQuality() {\n"+
              "    return (_real!=null) && _real.hasDataQuality();\n"+
      "  }");
      if (hasDQ(context)) {     // if the class has it, we need to implement the getter
        println(out,"  public synchronized org.cougaar.planning.ldm.dq.DataQuality getDataQuality() {\n"+
                "    return (_real==null)?null:(_real.getDataQuality());\n"+
        "  }");
      }
      
      println(out);
      println(out,"  // Finalization support");
      println(out,"  private "+className+" _real = null;");
      println(out,"  public synchronized void finalize(PropertyGroup real) {\n"+
              "    if (real instanceof "+className+") {\n"+
              "      _real=("+className+") real;\n"+
              "      notifyAll();\n"+
              "    } else {\n"+
              "      throw new IllegalArgumentException(\"Finalization with wrong class: \"+real);\n"+
              "    }\n"+
      "  }");
      println(out,"  private synchronized void waitForFinalize() {\n"+
              "    while (_real == null) {\n"+
              "      try {\n"+
              "        wait();\n"+
              "      } catch (InterruptedException _ie) {\n"+
              "        // We should really let waitForFinalize throw InterruptedException\n"+
              "        Thread.interrupted();\n"+
              "      }\n"+
              "    }\n"+
      "  }");
      println(out,"}");
    }
    
    
    void writeSetterIfc(PrintWriter out, String context, String className) {
      println(out,"/** Additional methods for "+className);
      println(out," * offering mutators (set methods) for the object's owner");
      println(out," **/");
      println(out);
      doPackaging(out, context);
      
      String importstr=(String)p.get(context,"import");
      if (importstr!=null) {
        for (String ve : explode(importstr, ',')) {
          println(out,"import "+ve+";");
        }
      }
      println(out);
      
      String newclassName = "New"+className;
      
      // figure out what we're supposed to extend
      String timePhased = (isTimePhased(context)?"TimePhased":"");
      String dq = (hasDQ(context)?", org.cougaar.planning.ldm.dq.HasDataQuality":"");
      String hasRelationships = (hasRelationships(context)?", HasRelationships":"");
      String extendstring = "extends "+className+ ", New"+timePhased+"PropertyGroup"+dq+hasRelationships;
      
      println(out,"public interface "+newclassName+" "+extendstring+" {");
      
      // declare the slots
      for (String slotspec : getAllSlotSpecs(context)) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        String etype = null;
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
          etype = ct.etype;
        }
        
        String depp = (String)p.get(context,name+".deprecated");
        if (depp != null) 
          println(out,"  /** @deprecated "+depp+" **/");
        if (as != null) {
          println(out,"  void "+as.setterSpec()+";");
        } else {
          println(out,"  void set"+toClassName(name)+"("+
                  type+" "+name+");");
        }
        // mutators for collection types
        if (etype != null) {
          println(out,"  void clear"+toClassName(name)+"();");
          println(out,"  boolean removeFrom"+toClassName(name)+"("+etype+" _element);");
          println(out,"  boolean addTo"+toClassName(name)+"("+etype+" _element);");
        }
        
        if (as != null) {
          // handler installation method
          String htype = as.handlerName();
          println(out,"  void set"+htype+"("+className+"."+htype+" handler);");
          println(out,"  "+className+"."+htype+" get"+htype+"();");
        }
        
      }
      
      // delegation
      {
        for (Argument dv : getAllDelegateSpecs(context)) {
          // define delegate getter and setter if non-automatic
          String autop = (String)p.get(context, dv.name+".auto");
          // if it isn't automatic, define the setter and getter
          if (!(autop != null && Boolean.valueOf(autop).booleanValue())) {
            println(out,"  "+dv.type+" get"+toClassName(dv.name)+"();");
            println(out,"  void set"+toClassName(dv.name)+"("+dv.type+" _"+dv.name+");");
          }
          
        }
      }
      
      println(out,"}");
    }
    
    List<String> getAllSlotSpecs(String context) {
      List<String> slotspecs = getLocalSlotSpecs(context);
      String exts = p.get(context, "extends");
      
      if (exts != null && exts.length() > 0) {
        for (String ss : getAllSlotSpecs(exts)) {
          slotspecs.add(ss);
        }
      }
      
      return slotspecs;
    }        
    
    List<String> getLocalSlotSpecs(String context) {
      return parseSlots(p.get(context, "slots"));
    }
    
    List<Argument> getAllDelegateSpecs(String context) {
      List<Argument> slotspecs = getLocalDelegateSpecs(context);
      String exts = p.get(context, "extends");
      if (exts != null && exts.length() > 0) {
        for (Argument arg : getAllDelegateSpecs(exts)) {
          slotspecs.add(arg);
        }
      }
      return slotspecs;
    }        
    
    List<Argument> getLocalDelegateSpecs(String context) {
      return parseArguments(p.get(context, "delegates"));
    }
    
    List<String> parseSlots(String slotstr) {
      if (slotstr == null) slotstr ="";
      int s=0;
      char[] chars = slotstr.toCharArray();
      int l = chars.length;
      int parens = 0;
      List<String> slots = new ArrayList<String>();
      
      int p;
      for (p=0; p<l; p++) {
        char c = chars[p];
        // need to parse out parens
        if (c == '(') {
          parens++;
        } else if (c == ')') {
          parens--;
        } else if (c == ',' && parens==0) {
          slots.add(slotstr.substring(s,p).trim());
          s = p+1;
        } else {
          // just advance
        }
      }
      if (p > s) {
        slots.add(slotstr.substring(s,p).trim());
      }
      return slots;
    }
    
    
    void writeImpl(PrintWriter out, String context, String className) {
      println(out,"/** Implementation of "+className+".");
      println(out," *  @see "+className);
      println(out," *  @see New"+className);
      println(out," **/");
      println(out);
      doPackaging(out, context);
      println(out,"import java.io.ObjectOutputStream;");
      println(out,"import java.io.ObjectInputStream;");
      println(out,"import java.io.IOException;");
      println(out,"import java.beans.PropertyDescriptor;");
      println(out,"import java.beans.IndexedPropertyDescriptor;");
      
      String importstr=(String)p.get(context,"import");
      if (importstr!=null) {
        for (String ve : explode(importstr, ',')) {
          println(out,"import "+ve+";");
        }
      }
      println(out);
      // figure out what we're supposed to extend
      String exts = p.get(context, "extends");
      String extendstring = "";
      
      if (exts != null) {
        extendstring = extendstring+", "+exts;
      }
      
      String adapter = p.get(context, "adapter");
      if (adapter == null)
        adapter = "java.beans.SimpleBeanInfo";
      
      String implclassName = className+"Impl";
      String newclassName = "New"+className;
      // dataquality requires a subclass
      println(out,"public"+(hasDQ(context)?"":" final")+" class "+implclassName+" extends "+adapter+"\n"+
              "  implements "+newclassName+", Cloneable\n{");
      
      // constructor
      println(out,"  public "+implclassName+"() {");
      
      for (Argument dv : getAllDelegateSpecs(context)) {
        // define delegate getter and setter if non-automatic
        String autop = (String)p.get(context, dv.name+".auto");
        // if it is automatic, pre-initialize it
        if (autop != null && Boolean.valueOf(autop).booleanValue()) {
          println(out,"    "+dv.name+" = new "+dv.type+"(this);");
        }
      }
      println(out,"  }");
      println(out);
      
      boolean timephased = isTimePhased(context);
      if (timephased) {
        //handle time phased support separately because getters != setters
        println(out,NewTimeSpanText);
      }
      
      // declare the slots (including "inherited" ones)
      println(out,"  // Slots\n");
      List<String> slotspecs = getAllSlotSpecs(context);
      for (String slotspec : slotspecs) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        String etype = null;
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
          etype = ct.etype;
        }
        
        String var = (String)p.get(context,name+".var");
        if (var == null) { var = "the"+toClassName(name); } // set the default
        if (var.equals("")) { var = null; } // unset if specified as empty
        
        String getter =  (String)p.get(context,name+".getter");
        String setter =  (String)p.get(context,name+".setter");
        
        if (as != null) {
          // active slot - write dispatchers
          String htype = as.handlerName();
          var = "the"+htype;
          
          // storage for the handler
          println(out,"  private transient "+className+"."+htype+" "+var+" = null;");
          
          // handler installation
          println(out,"  public void set"+htype+"("+className+"."+htype+" handler) {\n"+
                  "    "+var+" = handler;\n"+
          "  }");
          println(out,"  public "+className+"."+htype+" get"+htype+"() {\n"+
                  "    return "+var+";\n"+
          "  }");
          
          // get dispatcher
          if (getter != null){
            println(out,getter);
          } else {
            println(out,"  public "+type+" "+as.getter(var));
          }
          
          // set dispatcher
          if (setter != null){
            println(out,setter);
          } else {
            println(out,"  public void "+as.setter(var));
          }
          
        } else {
          
          // storage
          if (var != null) {
            String init = (String)p.get(context,name+".init");
            print(out,"  private "+type+" "+var);
            if (init != null) {
              print(out," = new "+init+"()");
            }
            println(out,";");
            
            // Normal getter
            if (getter != null) {
              println(out,getter);
            } else {
              println(out, "  public "+type+" get"+toClassName(name)+"(){ "+
                      "return "+var+"; }");
            }
            
            // test for Collection types
            if (etype != null) {
              String clname = toClassName(name);
              println(out,"  public boolean in"+clname+"("+etype+" _element) {\n"+
                      "    return ("+var+"==null)?false:("+var+".contains(_element));\n"+
              "  }");
              
              println(out,"  public "+etype+"[] get"+clname+"AsArray() {");
              println(out,"    if ("+var+" == null) return new "+etype+"[0];");
              println(out,"    int l = "+var+".size();");
              println(out,"    "+etype+"[] v = new "+etype+"[l];");
              println(out,"    int i=0;");
              println(out,"    for (Iterator n="+var+".iterator(); n.hasNext(); ) {");
              println(out,"      v[i]=("+etype+") n.next();");
              println(out,"      i++;");
              println(out,"    }");
              println(out,"    return v;");
              println(out,"  }");
              
              println(out,"  public "+etype+" getIndexed"+clname+"(int _index) {");
              println(out,"    if ("+var+" == null) return null;");
              println(out,"    for (Iterator _i = "+var+".iterator(); _i.hasNext();) {");
              println(out,"      "+etype+" _e = ("+etype+") _i.next();");
              println(out,"      if (_index == 0) return _e;");
              println(out,"      _index--;");
              println(out,"    }");
              println(out,"    return null;");
              println(out,"  }");
            }
            
            // setter
            if (setter != null) {
              println(out,setter);
            } else {
              println(out,"  public void set"+toClassName(name)+"("+type+" "+name+") {");
              // special handling for string setters
              if (type.equals("String")) {
                String isInternable = p.get(context, name+".intern");
                if (isInternable != null && isInternable.equals("true")) {
                  println(out,"    if ("+name+"!=null) "+name+"="+name+".intern();");
                }
              }
              println(out,"    "+var+"="+name+";");
              println(out,"  }");
            }
            
            // mutators for collection types
            if (etype != null) {
              println(out,"  public void clear"+toClassName(name)+"() {\n"+
                      "    "+var+".clear();\n"+
              "  }");
              println(out,"  public boolean removeFrom"+toClassName(name)+"("+etype+" _element) {\n"+
                      "    return "+var+".remove(_element);\n"+
              "  }");
              println(out,"  public boolean addTo"+toClassName(name)+"("+etype+" _element) {\n"+
                      "    return "+var+".add(_element);\n"+
              "  }");
            }
            
          } else {                // var is specified as empty
            if (getter != null) println(out,getter);
            if (setter != null) println(out,setter);
          }
        }
      }
      
      // delgates
      {
        println(out);
        for (Argument dv : getAllDelegateSpecs(context)) {
          println(out,"  private "+dv.type+" "+dv.name+" = null;");
          
          // define delegate getter and setter if non-automatic
          String autop = (String)p.get(context, dv.name+".auto");
          // if it isn't automatic, define the setter and getter
          if (!(autop != null && Boolean.valueOf(autop).booleanValue())) {
            println(out,"  public "+dv.type+" get"+toClassName(dv.name)+"() {\n"+
                    "    return "+dv.name+";\n"+
            "  }");
            println(out,"  public void set"+toClassName(dv.name)+"("+dv.type+" _"+dv.name+") {\n"+
                    "    if ("+dv.name+" != null) throw new IllegalArgumentException(\""+
                    dv.name+" already set\");\n"+
                    "    "+dv.name+" = _"+dv.name+";\n"+
            "  }");
          }
          
          for (DelegateSpec ds : parseDelegateSpecs(p.get(context, dv.name+".delegate"))) {
            println(out,"  public "+ds.type+" "+ds.name+
                    "("+unparseArguments(ds.args,true)+") {"+
                    " "+ ("void".equals(ds.type)?"":"return ")+dv.name+"."+ds.name+
                    "("+unparseArguments(ds.args,false)+");"+
            "  }");
          }
        }
      }
      
      // copy constructor
      println(out);
      println(out,"  public "+implclassName+"("+className+" original) {");
      
      if (timephased) {
        // copy TimeSpan info
        println(out, "    setTimeSpan(original.getStartTime(), original.getEndTime());");
      }
      
      // Copy slot info
      for (String slotspec : slotspecs) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
        }
        
        if (as != null) {
          // do nothing - clones don't get the handler automatically.
        } else {
          String var = (String)p.get(context,name+".var");
          if (var == null) { var = "the"+toClassName(name); } // set the default
          if (var.equals("")) { var = null; } // unset if specified as empty
          if (var != null) {
            println(out,"    "+var+" = original.get"+toClassName(name)+"();");
          }
        }
      }
      println(out,"  }");
      println(out);
      
      // .equals 
      writeEquals(out, context, className);
      
      if (hasDQ(context)) {
        println(out,"  public boolean hasDataQuality() { return false; }");
        println(out,"  public org.cougaar.planning.ldm.dq.DataQuality getDataQuality() { return null; }");
        /*
         // classes without runtime support do not implement NewHasDataQuality!
          println(out,"  public void setDataQuality(org.cougaar.planning.ldm.dq.DataQuality dq) {\n"+
          "    throw new IllegalArgumentException(\"This instance does not support setting of DataQuality.\");\n"+
          "  }");
          */
        println(out);
        println(out,"  // static inner extension class for real DataQuality Support");
        println(out,"  public final static class DQ extends "+className+"Impl implements org.cougaar.planning.ldm.dq.NewHasDataQuality {");
        println(out,
                "   public DQ() {\n"+ // never copy data quality
                "    super();\n"+
                "   }");
        
        println(out,
                "   public DQ("+className+" original) {\n"+ // never copy data quality
                "    super(original);\n"+
                "   }");
        println(out,"   public Object clone() { return new DQ(this); }");
        println(out,"   private transient org.cougaar.planning.ldm.dq.DataQuality _dq = null;");
        println(out,"   public boolean hasDataQuality() { return (_dq!=null); }");
        println(out,"   public org.cougaar.planning.ldm.dq.DataQuality getDataQuality() { return _dq; }");
        println(out,"   public void setDataQuality(org.cougaar.planning.ldm.dq.DataQuality dq) { _dq=dq; }");
        println(out,
                "   private void writeObject(ObjectOutputStream out) throws IOException {\n"+
                "    out.defaultWriteObject();\n"+
                "    if (out instanceof org.cougaar.core.persist.PersistenceOutputStream) out.writeObject(_dq);\n"+
                "   }");
        println(out,
                "   private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {\n"+
                "    in.defaultReadObject();\n"+
                "    if (in instanceof org.cougaar.core.persist.PersistenceInputStream) _dq=(org.cougaar.planning.ldm.dq.DataQuality)in.readObject();\n"+
                "   }");
        println(out,"    ");
        println(out,
                "    private final static PropertyDescriptor properties[]=new PropertyDescriptor[1];\n"+
                "    static {\n"+
                "      try {\n"+
                "        properties[0]= new PropertyDescriptor(\"dataQuality\", DQ.class, \"getDataQuality\", null);\n"+
                "      } catch (Exception e) { e.printStackTrace(); }\n"+
                "    }\n"+
                "    public PropertyDescriptor[] getPropertyDescriptors() {\n"+
                "      PropertyDescriptor[] pds = super.properties;\n"+
                "      PropertyDescriptor[] ps = new PropertyDescriptor[pds.length+properties.length];\n"+
                "      System.arraycopy(pds, 0, ps, 0, pds.length);\n"+
                "      System.arraycopy(properties, 0, ps, pds.length, properties.length);\n"+
                "      return ps;\n"+
                "    }");
        println(out,"  }");
        println(out);
        
        
      } else {
        println(out,"  public final boolean hasDataQuality() { return false; }");
      }
      println(out);
      
      // lock and unlock methods
      println(out,"  private transient "+className+" _locked = null;");
      println(out,"  public PropertyGroup lock(Object key) {");
      println(out,"    if (_locked == null)_locked = new _Locked(key);");
      println(out,"    return _locked; }");
      println(out,"  public PropertyGroup lock() "+
                  "{ return lock(null); }");
      println(out,"  public NewPropertyGroup unlock(Object key) "+
                  "{ return this; }");
      println(out);
      
      // clone method
      writeCloneMethod(out, context, implclassName, "  ");
      println(out);
      
      println(out,
              "  public PropertyGroup copy() {\n"+
              "    try {\n"+
              "      return (PropertyGroup) clone();\n"+
              "    } catch (CloneNotSupportedException cnse) { return null;}\n"+
              "  }");
      println(out);
      
      // introspection methods
      println(out,
              "  public Class getPrimaryClass() {\n"+
              "    return primaryClass;\n"+
              "  }");
      println(out,
              "  public String getAssetGetMethod() {\n"+
              "    return assetGetter;\n"+
              "  }");
      println(out,
              "  public String getAssetSetMethod() {\n"+
              "    return assetSetter;\n"+
              "  }");
      println(out);
      
      // Serialization
      boolean needSerialization = false;
      for (String slotspec : slotspecs) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
        }
        
        if (type.equals("String")) {
          String isInternable = p.get(context, name+".intern");
          if (isInternable != null && isInternable.equals("true")) {
            needSerialization = true;
            break;
          }
        }
      }
      
      if (needSerialization) {
        println(out,"  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {");
        println(out,"    in.defaultReadObject();");
        for (String slotspec : slotspecs) {
          int s = slotspec.indexOf(" ");
          String type = slotspec.substring(0,s);
          String name = slotspec.substring(s+1);
          ActiveSlot as = parseActiveSlot(slotspec);
          if (as != null) {
            name = as.name;
          }
          CollectionType ct = parseCollectionType(type);
          if (ct != null) {
            type = ct.ctype;
          }
          
          if (as != null) {
            // serialized copies can't get the active slot remotely.
            continue;           
          }
          String var = (String)p.get(context,name+".var");
          if (var == null) { var = "the"+toClassName(name); } // set the default
          if (var.equals("")) { var = null; } // unset if specified as empty
          if (var != null) {
            if (type.equals("String")) {
              String isInternable = p.get(context, name+".intern");
              if (isInternable != null && isInternable.equals("true")) {
                println(out,"    if ("+var+"!= null) "+var+"="+var+".intern();");
              }
            }
          }
        }
        
        println(out,"  }");
        println(out);
      }
      
      writeBeanInfoBody(out, context, className);
      
      // inner (locked) class
      println(out,"  private final class _Locked extends java.beans.SimpleBeanInfo\n"+
                  "    implements "+className+", Cloneable, LockedPG\n"+
                  "  {");
      println(out,"    private transient Object theKey = null;");
      println(out,"    _Locked(Object key) { ");
      println(out,"      if (this.theKey == null) this.theKey = key;");
      println(out,"    }  ");
      println(out);
      println(out,"    public _Locked() {}");
      println(out);
      println(out,"    public PropertyGroup lock() { return this; }");
      println(out,"    public PropertyGroup lock(Object o) { return this; }");
      println(out);
      println(out,"    public NewPropertyGroup unlock(Object key) "+
                  "throws IllegalAccessException {");
      println(out,"       if( theKey.equals(key) ) {");
      println(out,"         return "+implclassName+".this;");
      println(out,"       } else {");
      println(out,"         throw new IllegalAccessException(\"unlock: mismatched internal and provided keys!\");");
      println(out,"       }");
      println(out,"    }");
      println(out);
      /*
       println(out,"    public PropertyGroup copy() {\n"+
       "      return new "+implclassName+"("+implclassName+".this);\n"+
       "    }");
       */
      println(out,"    public PropertyGroup copy() {\n"+
                  "      try {\n"+
                  "        return (PropertyGroup) clone();\n"+
                  "      } catch (CloneNotSupportedException cnse) { return null;}\n"+
                  "    }");
      println(out);
      
      println(out);
      
      writeCloneMethod(out, context, implclassName, "    ");
      println(out);
      
      if (timephased) {
        println(out,"    public long getStartTime() { return " + 
                implclassName + ".this.getStartTime(); }");
        println(out,"    public long getEndTime() { return " +
                implclassName + ".this.getEndTime(); }");
      }
      
      println(out,"    public boolean equals(Object object) { return " + 
              implclassName + ".this.equals(object); }");
      
      // dispatch getters
      for (String slotspec : slotspecs) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        if (as != null) {
          name = as.name;
        }
        String etype = null;
        CollectionType ct = parseCollectionType(type);
        if (ct != null) {
          type = ct.ctype;
          etype = ct.etype;
        }
        
        String getter = "get"+toClassName(name);
        if (as != null) {
          println(out,"    public "+as.rtype+" "+as.getterSpec()+" {\n"+
                  "      return "+implclassName+".this."+as.getterCall()+";\n"+
                  "    }");
        } else {
          println(out,"    public "+type+" "+getter+"() { "+
                  "return "+implclassName+".this."+getter+"(); }");
        }
        
        // test for Collection types
        if (etype != null) {
          String clname = toClassName(name);
          String ref = implclassName+".this.";
          println(out,"  public boolean in"+clname+"("+etype+" _element) {\n"+
                  "    return "+ref+"in"+clname+"(_element);\n"+
          "  }");
          println(out,"  public "+etype+"[] get"+clname+"AsArray() {");
          println(out,"    return "+ref+"get"+clname+"AsArray();");
          println(out,"  }");
          
          println(out,"  public "+etype+" getIndexed"+clname+"(int _index) {");
          println(out,"    return "+ref+"getIndexed"+clname+"(_index);");
          println(out,"  }");
        }
        
      }
      
      {
        for (Argument dv :getAllDelegateSpecs(context)) {
          for (DelegateSpec ds : parseDelegateSpecs(p.get(context, dv.name+".delegate"))) {
            println(out,"  public "+ds.type+" "+ds.name+
                    "("+unparseArguments(ds.args,true)+") {\n"+
                    "    "+("void".equals(ds.type)?"":"return ")+implclassName+".this."+ds.name+
                    "("+unparseArguments(ds.args,false)+");\n"+
            "  }");
          }
        }
      }
      
      if (hasDQ(context)) {
        println(out,"  public final boolean hasDataQuality() { return "+
                implclassName+".this.hasDataQuality(); }");
        println(out,"  public final org.cougaar.planning.ldm.dq.DataQuality getDataQuality() { return "+
                implclassName+".this.getDataQuality(); }");
      } else {
        println(out,"  public final boolean hasDataQuality() { return false; }");
      }
      
      // introspection method of locked
      println(out,
              "    public Class getPrimaryClass() {\n"+
              "      return primaryClass;\n"+
              "    }");
      println(out,
              "    public String getAssetGetMethod() {\n"+
              "      return assetGetter;\n"+
              "    }");
      println(out,
              "    public String getAssetSetMethod() {\n"+
              "      return assetSetter;\n"+
              "    }");
      println(out);
      
      // BeanInfo dispatchers
      println(out,"    public PropertyDescriptor[] getPropertyDescriptors() {");
      println(out,"      return properties;");
      println(out,"    }");
      println(out);
      println(out,"    public Class getIntrospectionClass() {");
      println(out,"      return "+className+"Impl.class;");
      println(out,"    }");
      println(out);
      
      println(out,"  }");
      println(out);
      
      println(out,"}");
    }
    
    protected void writeCloneMethod(PrintWriter out, String context, 
                                    String implclassName, 
                                    String leadingSpaces) {
      println(out,leadingSpaces+"public Object clone() throws CloneNotSupportedException {");
      List<Argument> delegateSpecs = getAllDelegateSpecs(context);
      if (delegateSpecs.size() == 0) {
        println(out, leadingSpaces+"  return new "+implclassName+"("+implclassName+".this);");
      } else {
        println(out, leadingSpaces+"  "+implclassName+" _tmp = new "+implclassName+"(this);");
        for (Argument delegate : delegateSpecs) {
          println(out, leadingSpaces+"  if ("+delegate.name+" != null) {");
          println(out, leadingSpaces+"    _tmp."+delegate.name+" = ("+delegate.type+") "+
                  delegate.name+".copy(_tmp);");
          println(out, leadingSpaces+"  }");
        }
        println(out, leadingSpaces+"  return _tmp;");
      }
      println(out, leadingSpaces+"}");
    }
    
    void writeBeanInfoBody( PrintWriter out, String context, String className ) {
      
      List<String> slotspecs = getAllSlotSpecs(context);
      
      // Add in time phased slots
      if (isTimePhased(context)) {
        slotspecs.add("long start_time");
        slotspecs.add("long end_time");
      }
      
      // figure out how many property slots we need
      int l = 0;                // props
      int asc = 0;              // methods
      int cc = 0;               // collections (not used)
      for (String slotspec : slotspecs) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        ActiveSlot as = parseActiveSlot(slotspec);
        // count the different slot types
        if (as == null) {         
          CollectionType ct = parseCollectionType(type);
          if (ct != null) {
            cc++;
          } else {
            l++;
          }
        } else {
          asc++;
        }
      }
      
      println(out,"  private final static PropertyDescriptor properties[] = new PropertyDescriptor["+(l+cc)+"];");
      if ((l+cc)>0) {
        println(out,"  static {");
        println(out,"    try {");
        int i = 0;
        String clname = toClassName(context);
        
        for (String slotspec : slotspecs) {
          int s = slotspec.indexOf(" ");
          String type = slotspec.substring(0,s);
          String name = slotspec.substring(s+1);
          ActiveSlot as = parseActiveSlot(slotspec);
          if (as != null) {
            name = as.name;
          }
          CollectionType ct = parseCollectionType(type);
          if (ct != null) {
            type = ct.ctype;
          }
          
          
          if (as == null) {
            // non-active slots
            if (ct == null) {
              // plain slots
              println(out,"      properties["+i+"]= new PropertyDescriptor(\""+
                      name+"\", "+
                      clname+".class, "+
                      "\"get"+toClassName(name)+"\", null);");
            } else {
              // collection slots
              println(out,"      properties["+i+"]= new IndexedPropertyDescriptor(\""+
                      name+"\", "+
                      clname+".class, "+
                      "\"get"+toClassName(name)+"AsArray\", null, "+
                      "\"getIndexed"+toClassName(name)+"\", null);");
            }
            i++;
          }
        }
        println(out,"    } catch (Exception e) { \n"+
                "      org.cougaar.util.log.Logging.getLogger("+clname+".class).error(\"Caught exception\",e);\n"+
        "    }");
        println(out,"  }");
      }
      println(out);
      println(out,"  public PropertyDescriptor[] getPropertyDescriptors() {");
      println(out,"    return properties;");
      println(out,"  }");
    }
    
    protected void writeEquals(PrintWriter out, String context, 
                               String className ) {
      
      if (delegatesEquals(context)) {
        return;
      }
      
      String otherVar = "other" + className;
      
      println(out,"  public boolean equals(Object other) {");
      println(out);
      println(out, "    if (!(other instanceof " + className + ")) {");
      println(out, "      return false;");
      println(out, "    }");
      println(out);
      println(out, "    " + className + " " + otherVar + " = (" + className + 
      ") other;");
      println(out);
      
      String implClassName = className + "Impl";
      
      for (String slotspec : getAllSlotSpecs(context)) {
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
        ActiveSlot as = parseActiveSlot(slotspec);
        
        String getter;
        
        if (as != null) {
          // Limit comparison to handler's .equals()
          getter = "get" + as.handlerName() + "()";
          type = as.handlerName();
        } else {
          String var = (String)p.get(context,name+".var");
          if (var == null) { 
            var = "the"+toClassName(name); 
          } // set the default
          if (var.equals("")) { var = null; } // unset if specified as empty
          
          getter =  (String)p.get(context,name+".getter");
          
          if (getter == null) {	  
            getter = "get" + toClassName(name) +  "()";
          } 
        }
        
        // Does method return an object or a primitive data type?
        if (primitiveDataType(type)) {
          println(out, "    if (!(" + getter + " == " + otherVar +
                  "." + getter + ")) {");
          println(out, "      return false;");
          println(out, "    }");
          println(out);
        } else if (as != null) {
          println(out, "    if (other instanceof " + implClassName + ") {");
          println(out, "      if (" + getter + " == null) {");
          println(out, "        if (((" + implClassName + ") " +
                  otherVar + ")." + getter + " != null) {");
          println(out, "          return false;");
          println(out, "        }");
          println(out, "      } else if (!(" + getter + ".equals(((" + 
                  implClassName + ") " + otherVar +
                  ")." + getter + "))) {");
          println(out, "        return false;");
          println(out, "      }");
          println(out, "    }");
          println(out);
        } else {
          println(out, "    if (" + getter + " == null) {");
          println(out, "      if (" + otherVar + "." + getter + " != null) {");
          println(out, "        return false;");
          println(out, "      }");
          println(out, "    } else if (!(" + getter + ".equals(" + otherVar +
                  "." + getter + "))) {");
          println(out, "      return false;");
          println(out, "    }");
          println(out);
        }
      }
      
      // Call .equals for all delegates
      List<Argument> vs = getAllDelegateSpecs(context);
      //BG only accessible from impl class so can't compare _Locked and Impl
      if (vs.size() > 0) {
        println(out, "    if (other instanceof " + implClassName + ") {");
        for (Argument dv : vs) {
          String getter = "get" + toClassName(dv.name) +  "()";
          println(out, "      if (" + getter + " == null) {");
          println(out, "        if (((" + implClassName + ") " + otherVar + 
                  ")." + getter + " != null) {");
          println(out, "          return false;");
          println(out, "        }");
          println(out, "      } else if (!(" + getter + ".equals(((" + 
                  implClassName + ") " + otherVar +
                  ")." + getter + "))) {");
          println(out, "        return false;");
          println(out, "      }");
          println(out);
        }
        println(out, "    }");
      }
      
      println(out, "    return true;");
      println(out, "  }");
      println(out);
    }
    
    void writePropertyGroup(String context) throws Exception {
      String className = toClassName(context);
      
      FileOutputStream fos;
      OutputStreamWriter osw;
      PrintWriter out;
      
      if (cleanp) {
        (new File(className.toString() + ".java")).delete();
        (new File("New"+ className.toString() + ".java")).delete();
        (new File(className.toString() + "Impl.java")).delete();
        (new File(className.toString() + "BeanInfo.java")).delete();
        return;
      }
      
      String outname = className.toString() + ".java";
      if (writeInterfaces) {
        debug("Writing GetterIfc \""+context+"\" to \""+outname+"\"");
        noteFile(outname);
        fos = new FileOutputStream(new File(getTargetDir(),outname));
        osw = new OutputStreamWriter(fos);
        out = new PrintWriter(osw);
        writeCR(out,deffilename);
        writeGetterIfc(out, context, className);
        out.close();
        
        outname = "New" + className.toString() + ".java";
        debug("Writing SetterIfc \""+context+"\" to \""+outname+"\"");
        noteFile(outname);
        fos = new FileOutputStream(new File(getTargetDir(),outname));
        osw = new OutputStreamWriter(fos);
        out = new PrintWriter(osw);
        writeCR(out,deffilename);
        writeSetterIfc(out, context, className);
        out.close();
      }
      
      outname = className.toString() + "Impl.java";
      debug("Writing Impl \""+context+"\" to \""+outname+"\"");
      noteFile(outname);
      fos = new FileOutputStream(new File(getTargetDir(),outname));
      osw = new OutputStreamWriter(fos);
      out = new PrintWriter(osw);
      writeCR(out,deffilename);
      writeImpl(out, context, className);
      out.close();
      
    }
    
    public void writeFactory () throws IOException {
      String outname = "PropertyGroupFactory.java";
      
      if (cleanp) {
        (new File(outname)).delete();
        return;
      }
      
      debug("Writing FactoryImplementation to \""+outname+"\"");
      noteFile(outname);
      FileOutputStream fos = new FileOutputStream(new File(getTargetDir(),outname));
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      PrintWriter out = new PrintWriter(osw);
      writeCR(out,deffilename);
      
      println(out,"/** AbstractFactory implementation for Properties.");
      println(out," * Prevents clients from needing to know the implementation");
      println(out," * class(es) of any of the properties.");
      println(out," **/");
      println(out);
      doPackaging(out,"global");
      
      println(out);
      String xclause = "";
      String exts = p.get("global", "factoryExtends");
      if (exts != null) {
        xclause = "extends "+exts+" ";
      }
      println(out,"public class PropertyGroupFactory "+xclause+"{");
      
      for (String context : p.getContexts()) {
        if (! context.equals("global") && 
            isPrimary(context) &&
            (p.get(context, "abstract") == null)) {
          String newclassName = "New"+context;
          String icn = context+"Impl";
          println(out,"  // brand-new instance factory");
          println(out,"  public static "+newclassName+" new"+context+"() {");
          println(out,"    return new "+icn+"();");
          println(out,"  }");
          
          boolean timephased = isTimePhased(context);
          if (timephased) {
            println(out,"  // brand-new instance factory");
            println(out,"  public static PropertyGroupSchedule new"+context+
            "Schedule() {");
            println(out,"    return new PropertyGroupSchedule(new"+context+"());");
            println(out,"  }");
          }
          
          println(out,"  // instance from prototype factory");
          println(out,"  public static "+newclassName+" new"+context+"("+context+" prototype) {");
          println(out,"    return new "+icn+"(prototype);");
          println(out,"  }");
          println(out);
          
          if (timephased) {
            println(out,"  // instance from prototype factory");
            println(out,"  public static PropertyGroupSchedule new"+context+
                    "Schedule("+context+" prototype) {");
            println(out,"    return new PropertyGroupSchedule(new"+context+"(prototype));");
            println(out,"  }");
            println(out);
            
            println(out,"  // instance from prototype schedule");
            println(out,"  public static PropertyGroupSchedule new"+context+
            "Schedule(PropertyGroupSchedule prototypeSchedule) {");
            println(out,"    if (!prototypeSchedule.getPGClass().equals("+context+
            ".class)) {");
            println(out,"      throw new IllegalArgumentException(\"new"+context+
                    "Schedule requires that getPGClass() on the PropertyGroupSchedule argument return "+
                    context+".class\");");
            println(out,"    }");
            println(out,"    return new PropertyGroupSchedule(prototypeSchedule);");
            println(out,"  }");
            println(out);
          }
        }
      }
      println(out,"  /** Abstract introspection information.");
      println(out,"   * Tuples are {<classname>, <factorymethodname>}");
      println(out,"   * return value of <factorymethodname> is <classname>.");
      println(out,"   * <factorymethodname> takes zero or one (prototype) argument.");
      println(out,"   **/");
      println(out,"  public static String properties[][]={");
      for (String context : p.getContexts()) {
        if (! context.equals("global") && 
            isPrimary(context) &&
            (p.get(context, "abstract") == null)) {
          String pkg = findPackage(context);
          print(out,"    {\""+pkg+"."+context+"\", \"new"+context+"\"}");
          if (isTimePhased(context)) {
            print(out,",");
            println(out);
            print(out,"    {\"org.cougaar.planning.ldm.asset.PropertyGroupSchedule\", \"new"+context+"Schedule\"}");
          }
          println(out, ",");
        }
      }
      println(out,"  };");
      
      
      println(out,"}");
      out.close();
    }
    
    protected boolean delegatesEquals(String context) {
      for (Argument dv :getAllDelegateSpecs(context)) {
        for (DelegateSpec ds : parseDelegateSpecs(p.get(context, dv.name+".delegate"))) {
          if ((ds.type.equals("boolean")) &&
              (ds.name.equals("equals")) &&
              (ds.args.size() == 1)) {
            Argument arg = (Argument) ds.args.get(0);
            if (arg.type.equals("Object")) {
              return true;
            } else {
              return false;
            }
          }
        }
      }
      return false;
    }
    
    protected boolean primitiveDataType(String type) {
      return ((type != null) && 
              ((type.equals("boolean")) ||
              (type.equals("char")) ||
              (type.equals("byte")) ||
              (type.equals("short")) ||
              (type.equals("int")) ||
              (type.equals("long")) ||
              (type.equals("float")) ||
              (type.equals("double"))));
    }
    
    protected class CollectionType {
      public String ctype;
      public String etype;
      public CollectionType(String ct, String et) {
        ctype = ct;
        etype = et;
      }
    }
    protected CollectionType parseCollectionType (String typestring) {
      int es = typestring.indexOf("<");
      int ee = typestring.indexOf(">");
      if (ee > es && es >= 0) {
        String ctype = typestring.substring(0, es).trim();
        String etype = typestring.substring(es+1, ee);
        return new CollectionType(ctype,etype);
      } else {
        return null;
      }
    }
    
    protected class ActiveSlot {
      public String rtype;
      public String name;
      public String[] arguments;
      public String[] types;
      public ActiveSlot(String rtype, String name, Object[] arguments, Object[] types) {
        this.rtype = rtype;
        this.name = name;
        int l = arguments.length;
        this.arguments = new String[l];
        this.types = new String[l];
        for (int i = 0; i <l; i++) {
          this.arguments[i] = (String) arguments[i];
          this.types[i] = (String) types[i];
        }
      }
      public String getterSpec() {
        String s = "get"+toClassName(name)+"(";
        s=s+typedarglist()+")";
        return s;
      }
      public String getterCall() {
        return "get"+toClassName(name)+"("+arglist()+")";
      }        
      public String getter(String var) {
        String s = getterSpec();
        s=s+" {\n"+
        "    if ("+var+"==null) throw new UndefinedValueException();\n"+
        "    return "+var+".get"+toClassName(name)+"(";
        s=s+arglist()+");\n  }";
        return s;
      }
      public String setterSpec() {
        String s = "set"+toClassName(name)+"("+rtype+" _value";
        if (arguments.length>0) s=s+", ";
        s=s+typedarglist()+")";
        return s;
      }
      public String setter(String var) {
        String s = setterSpec();
        s=s+" {\n"+
        "    if ("+var+"==null) throw new UndefinedValueException();\n"+
        "    "+var+".set"+toClassName(name)+"(_value";
        if (arguments.length>0)s=s+", ";
        s=s+arglist()+");\n  }";
        return s;
      }
      public String arglist() {
        String s="";
        for (int i = 0; i<arguments.length;i++) {
          if (i!= 0) s=s+", ";
          s=s+arguments[i];
        }
        return s;
      }
      public String typedarglist() {
        String s="";
        for (int i = 0; i<arguments.length;i++) {
          if (i!= 0) s=s+", ";
          s=s+types[i]+" "+arguments[i];
        }
        return s;
      }
      public String handlerName() {
        return toClassName(name)+"Handler";
      }
      public String handlerClassDef() {
        // BIZARRE - We apparently need the "public static"
        return 
        "  public static interface "+handlerName()+" {\n"+
        "    "+rtype+" "+getterSpec()+";\n"+
        "    void "+setterSpec()+";\n"+
        "  }";
      }
    }
    
    protected ActiveSlot parseActiveSlot (String slotd) {
      int sp = slotd.indexOf(" ");
      String rtype=slotd.substring(0,sp).trim();
      String slotname=slotd.substring(sp+1).trim();
      // search for foo(int x, int y)
      int es = slotname.indexOf("(");
      int ee = slotname.indexOf(")");
      if (!(ee > es && es >= 0)) return null; //  not an active slot?
      
      String name = slotname.substring(0,es);
      
      List<String> args = new ArrayList<String>();
      List<String> types = new ArrayList<String>();
      
      for (String arg : explode(slotname.substring(es+1, ee), ',')) {
        arg = arg.trim();
        sp = arg.indexOf(" ");
        if (sp < 0) throw new RuntimeException("Broken active slot specification: "+ slotname);
        String at = arg.substring(0,sp);
        types.add(at);
        String av = arg.substring(sp+1).trim();
        args.add(av);
      }
      return new ActiveSlot(rtype, name, args.toArray(), types.toArray());
    }
    
    
    class Argument {
      String type;
      String name;
      public Argument(String t, String n) { type=t; name=n; }
      public String toString() {
        return type+" "+name;
      }
    }
    
    class DelegateSpec {
      String type;
      String name;
      List<Argument> args;
      public DelegateSpec(String t, String n, List<Argument> a) {
        type=t; name=n; args=a;
      }
      public String toString() {
        return type+" "+name+"("+args+")";
      }
    }
    
    public Argument parseArgument(String s) {
      s = s.trim();
      int p = s.indexOf(" ");
      return new Argument(s.substring(0,p).trim(), 
                          s.substring(p+1).trim());
    }
    
    public String unparseArguments(List<Argument> args, boolean typesToo) {
      String s = "";
      for (Argument arg : args) {
        if (! s.equals("")) s=s+", ";
        if (typesToo) s=s+arg.type+" ";
        s=s+arg.name;
      }
      return s;
    }        
    
    public List<Argument> parseArguments(String s) {
      if (s == null) return new ArrayList<Argument>(0);
      List<String> argstrs = explode(s, ',');
      List<Argument> args = new ArrayList<Argument>(argstrs.size());
      for (String arg : argstrs) {
        args.add(parseArgument(arg));
      }
      return args;
    }
    
    public DelegateSpec parseDelegateSpec(String s) {
      s = s.trim();
      int p1 = s.indexOf(" ");
      int p2 = s.indexOf("(");
      int p3 = s.indexOf(")");
      String type = s.substring(0,p1).trim();
      String name = s.substring(p1+1,p2).trim();
      List<Argument> args = parseArguments(s.substring(p2+1,p3));
      return new DelegateSpec(type, name, args);
    }
    
    public List<DelegateSpec> parseDelegateSpecs(String s) {
      List<String> v = explode(s, ';');
      List<DelegateSpec> ds = new ArrayList<DelegateSpec>(v.size());
      for (String x : v) {
        x = x.trim();
        if (x.length()>0) {
          ds.add(parseDelegateSpec(x));
        }
      }
      return ds;
    }      
    
    public void writeAsset () throws IOException {
      String outname = "AssetSkeleton.java";
      if (cleanp) {
        (new File(outname)).delete();
        return ;
      }
      
      debug("Writing AssetSkeleton to \""+outname+"\"");
      noteFile(outname);
      FileOutputStream fos = new FileOutputStream(new File(getTargetDir(),outname));
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      PrintWriter out = new PrintWriter(osw);
      writeCR(out,deffilename);
      
      println(out,"/** Abstract Asset Skeleton implementation");
      println(out," * Implements default property getters, and additional property");
      println(out," * lists.");
      println(out," * Intended to be extended by org.cougaar.planning.ldm.asset.Asset");
      println(out," **/");
      println(out);
      doPackaging(out,"global");
      println(out,"import java.io.Serializable;");
      println(out,"import java.beans.PropertyDescriptor;" );
      println(out,"import java.beans.IndexedPropertyDescriptor;" );
      println(out);
      String baseclass = p.get("global","skeletonBase");
      if (baseclass == null) baseclass = "org.cougaar.planning.ldm.asset.Asset";
      println(out,"public abstract class AssetSkeleton extends "+baseclass+" {");
      println(out);
      // default constructor
      println(out,"  protected AssetSkeleton() {}");
      println(out);
      // copy constructor
      println(out,"  protected AssetSkeleton(AssetSkeleton prototype) {\n"+
              "    super(prototype);\n"+
      "  }");
      println(out);
      
      println(out,"  /**                 Default PG accessors               **/");
      println(out);
      
      for (String context : p.getContexts()) {
        if (!context.equals("global") &&
            isPrimary(context) &&
            (p.get(context, "abstract") == null)) {
          println(out,"  /** Search additional properties for a "+
                  context+
          " instance.");
          println(out,"   * @return instance of "+context+" or null.");
          println(out,"   **/");
          
          boolean timephased = isTimePhased(context);
          String timeVar = "";
          
          if (timephased) {
            timeVar = "long time";
          }
          println(out,"  public "+context+" get"+context+"("+timeVar+")");
          println(out,"  {");
          
          if (timephased) {
            timeVar = ", time";
          }
          println(out,"    "+context+" _tmp = ("+context+") resolvePG("+
                  context+".class"+timeVar+");");
          println(out,"    return (_tmp=="+context+".nullPG)?null:_tmp;");
          println(out,"  }");
          println(out);
          
          // For timephased - get default
          if (timephased) {
            println(out,"  public "+context+" get"+context+"()");
            println(out,"  {");
            println(out,"    PropertyGroupSchedule pgSchedule = get"+context+
            "Schedule();");
            println(out,"    if (pgSchedule != null) {");
            println(out,"      return ("+context+
            ") pgSchedule.getDefault();");
            println(out,"    } else {");
            println(out,"      return null;");
            println(out,"    }");
            println(out,"  }");
            println(out);
          }
          
          if (timephased) {
            println(out,"  /** Test for existence of a default "+context+"\n"+
            "   **/");
          } else {
            println(out,"  /** Test for existence of a "+context+"\n"+
            "   **/");
          }
          println(out,"  public boolean has"+context+"() {");
          println(out,"    return (get"+context+"() != null);\n"+
          "  }");
          println(out);
          
          
          if (timephased) {
            println(out,"  /** Test for existence of a "+context+
                    " at a specific time\n"+"   **/");
            println(out,"  public boolean has"+context+"(long time) {");
            println(out,"    return (get"+context+"(time) != null);\n"+
            "  }");
            println(out);
          }
          
          String vr = "a"+context;
          println(out,"  /** Set the "+context+" property.\n"+
                  "   * The default implementation will create a new "+context+"\n"+
                  "   * property and add it to the otherPropertyGroup list.\n"+
                  "   * Many subclasses override with local slots.\n"+
                  "   **/\n"+
                  "  public void set"+context+"(PropertyGroup "+vr+") {\n"+
                  "    if ("+vr+" == null) {\n"+
                  "      removeOtherPropertyGroup("+context+".class);\n"+
                  "    } else {\n"+
                  "      addOtherPropertyGroup(a"+context+");\n"+
                  "    }\n"+
          "  }");
          println(out);
          
          if (timephased) {
            println(out,"  public PropertyGroupSchedule get"+context+"Schedule()");
            println(out,"  {");
            println(out,"    return searchForPropertyGroupSchedule("+context+
            ".class);");
            println(out,"  }");
            println(out);
            
            println(out,"  public void set"+context+"Schedule(PropertyGroupSchedule schedule) {\n"+
                    "    removeOtherPropertyGroup("+context+".class);\n"+
                    "    if (schedule != null) {\n"+
                    "      addOtherPropertyGroupSchedule(schedule);\n"+
                    "    }\n"+
            "  }");
            println(out);
          }
        }
      }
      
      println(out,"}");
      out.close();
    }
    
    
    
    private void writeIndex() throws Exception {
      String outname = "Properties.index";
      if (cleanp) {
        (new File(outname)).delete();
        return ;
      }
      noteFile(outname);
      
      debug("Writing Properties Index to \""+outname+"\"");
      FileOutputStream fos = new FileOutputStream(new File(getTargetDir(),outname));
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      PrintWriter out = new PrintWriter(osw);
      
      println(out,"- - - - - List of machine generated PropertyGroups - - - - -");
      
      Map<String,List<String>> permuted = new HashMap<String,List<String>>();
      
      List<String> l =new ArrayList<String>(p.table.keySet());
      Collections.sort(l);
      for (String context : l) {
        println(out,"* "+context);
        String doc = p.get(context,"doc");
        if (doc != null) {
          println(out,doc);
        }
        
        for (String slotspec : getAllSlotSpecs(context)) {
          int s = slotspec.indexOf(" ");
          String type = slotspec.substring(0, s);
          String name = slotspec.substring(s+1);
          ActiveSlot as = parseActiveSlot(slotspec);
          if (as != null) {
            name = as.name;
          }
          print(out,"    "+type+" "+toClassName(name)+";");
          String slotdoc = (String)p.get(context, name+".doc");
          if (slotdoc != null) print(out,"\t//"+slotdoc);
          println(out);
          
          
          List<String> pent = permuted.get(name);
          if (pent == null) {
            pent = new ArrayList<String>();
            permuted.put(name, pent);
          } 
          pent.add(context);
        }
      }
      
      println(out,"- - - - - Permuted index of PropertyGroupSlots - - - - -");
      List<String> k = new ArrayList<String>(permuted.keySet());
      Collections.sort(k);
      for (String name : k) {
        println(out,"* "+toClassName(name));
        for (String context : permuted.get(name)) {
          println(out,"    "+context);
        }
      }
      
      out.close();
    }
    
    public void write() throws Exception {
      grokGlobal();
      for (String context : p.getContexts()) {
        if (! context.equals("global") &&
            (p.get(context, "abstract") == null) &&
            (isPrimary(context))) {
          writePropertyGroup(context);
        }
      }
      if (writeAbstractFactory)
        writeFactory();
      writeAsset();
      
      writeIndex();
    }
    
    protected boolean isPrimary(String context) {
      String source = p.get(context, PGParser.PG_SOURCE);
      return (source != null) && (source.equals(PGParser.PRIMARY));
    }
  }
  
  /** arguments to the writer **/
  private String arguments[];
  
  public String deffilename = null;
  
  boolean isVerbose = false;
  boolean cleanp = false;
  boolean writeAbstractFactory = true;
  boolean writeInterfaces = true;
  
  public void debug(String s) {
    if (isVerbose)
      System.err.println(s);
  }
  
  void processFile(String filename) {
    InputStream stream = null;
    try {
      setDirectories(filename);
      if (filename.equals("-")) {
        debug("Reading from standard input.");
        stream = new java.io.DataInputStream(System.in);
      } else if (new File(filename).exists()) {
        debug("Reading \""+filename+"\".");
        deffilename = filename;
        stream = new FileInputStream(filename);
      } else {
        deffilename = filename;
        debug("Using ClassLoader to read \""+filename+"\".");
        stream = 
          getClass().getClassLoader().getResourceAsStream(filename);
      }
      
      PGParser p = new PGParser(isVerbose);
      p.parse(stream);
      p.setModifiable(false);
      stream.close();
      
      Writer w = new Writer(p);
      w.write();
      w.done();
    } catch (Exception e) {
      System.err.println("Caught: "+e);
      e.printStackTrace();
    }
  }
  
  void usage(String s) {
    System.err.println(s);
    System.err.println("Usage: PGWriter [-v] [-f] [-i] [--] file [file ...]\n"+
                       "-v  toggle verbose mode (default off)\n"+
                       "-f  toggle AbstractFactory generation (on)\n"+
                       "-i  toggle Interface generation (on)\n"
    );
    System.exit(1);
  }
  
  public void start() {
    boolean ignoreDash = false;
    String propertiesFile = null;
    
    for (int i=0; i<arguments.length; i++) {
      String arg = arguments[i];
      if (!ignoreDash && arg.startsWith("-")) { // parse flags
        if (arg.equals("--")) {
          ignoreDash = true;
        } else if (arg.equals("-v")) {
          isVerbose = (!isVerbose);
        } else if (arg.equals("-f")) {
          writeAbstractFactory = (!writeAbstractFactory);
        } else if (arg.equals("-clean")) {
          cleanp = true;
        } else if (arg.equals("-properties")) {
          propertiesFile = arguments[++i];
        } else if (arg.equals("-d")) {
          targetDirName = arguments[++i];
        } else {
          usage("Unknown option \""+arg+"\"");
        }
      } else {                  // deal with files
        propertiesFile = arg;
      }
    }
    
    if (propertiesFile == null) {
      propertiesFile = PGParser.DEFAULT_FILENAME;
    }
    
    processFile(propertiesFile);
  }
  
  public PGWriter(String args[]) {
    arguments=args;
  }
  
  
  public static void main(String args[]) {
    PGWriter mw = new PGWriter(args);
    mw.start();
  }
}
