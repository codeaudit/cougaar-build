/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
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


public class PGWriter extends WriterBase {
  private final static String NewTimeSpanText =
    "//NewTimeSpan implementation\n" +
    "  private long theStartTime = org.cougaar.util.TimeSpan.MIN_VALUE;\n" +
    "  public long getStartTime() {\n" +
    "    return theStartTime;\n" +
    "  }\n" +
    "\n" +
    "  private long theEndTime = org.cougaar.util.TimeSpan.MAX_VALUE;\n" +
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
      filelist.println(s);
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

    Vector explode(String s) { return explode(s, ' '); }
    Vector explode(String s, char x) {
      Vector r = new Vector();
      int last = 0;
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == x) {
          if (i>last)
            r.addElement(s.substring(last,i));
          last = i+1;
        }
      }
      if (s.length()>last) r.addElement(s.substring(last));
      return r;
    }

    String findPackage(String context) {
      String pkg = p.get(context, "package");
      if (pkg == null)
        pkg = p.get("global", "package");
      if (pkg == null)
        pkg = "org.cougaar.domain.planning.ldm.asset";
      return pkg;
    }

    void doPackaging(PrintWriter out, String context) {
      out.println("package "+findPackage(context)+";");
      out.println();
      out.println("import org.cougaar.domain.planning.ldm.measure.*;");
      out.println("import org.cougaar.domain.planning.ldm.asset.*;");
      out.println("import org.cougaar.domain.planning.ldm.plan.*;");
      out.println("import java.util.*;");
      out.println();
      doImports(out, "global");
      if (!context.equals("global")) 
        doImports(out, context);
    }
    void doImports(PrintWriter out, String context) {
      String importstr=(String)p.get(context,"import");
      if (importstr!=null) {
        Vector v = explode(importstr, ',');
        Enumeration ves = v.elements();
        while (ves.hasMoreElements()) {
          String ve=(String)ves.nextElement();
          out.println("import "+ve+";");
        }
      }      
      out.println();
    }

    /** return DQ specification of context, first looking at local context,
     * then global, then default (true).
     **/
    boolean hasDQ(String context) {
      String s = (String)p.get(context,"hasDataQuality");
      if (s == null) {
        s = (String)p.get("global","hasDataQuality");
      }
      if (s == null) {
        return true;            // default is true
      } else {
        return Boolean.valueOf(s).booleanValue();
      }
    }

    void writeGetterIfc(PrintWriter out, String context, String className) {
      out.println("/** Primary client interface for "+className+".");
      String doc = (String)p.get(context,"doc");
      if (doc != null)
        out.println(" * "+doc);
      out.println(" *  @see New"+className);
      out.println(" *  @see "+className+"Impl");
      out.println(" **/");
      out.println();
      doPackaging(out, context);

      // figure out what we're supposed to extend

      { 
        String dq = (hasDQ(context)?", org.cougaar.domain.planning.ldm.dq.HasDataQuality":"");

        if (p.get(context, PGParser.TIMEPHASED) == null) {
          out.println("public interface "+className+" extends PropertyGroup"+dq+" {");
        } else {
          out.println("public interface "+className+" extends TimePhasedPropertyGroup"+
                      dq+" {");
        }
      }

      // declare the slots
      Vector slotspecs = getAllSlotSpecs(context);

      Enumeration se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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
          out.println("  /** "+slotdoc+" **/");
        if (as!=null) {
          out.println("  "+type+" "+as.getterSpec()+";");
        } else {
          out.println("  "+type+" get"+clname+"();");
        }

        // extra collection type api
        if (etype != null) {
          out.println("  /** test to see if an element is a member of the "+name+" Collection **/");
          out.println("  boolean in"+clname+"("+etype+" element);");
          out.println();
          
          out.println("  /** array getter for beans **/");
          out.println("  "+etype+"[] get"+clname+"AsArray();");
          out.println();
          
          out.println("  /** indexed getter for beans **/");
          out.println("  "+etype+" getIndexed"+clname+"(int index);");
          out.println();
        }

        if (as != null) {
          out.println(as.handlerClassDef());
        }
      }      
      out.println();
      

      // delegation handling
      Vector v = getAllDelegateSpecs(context);
      if (v != null) {
        for (int i =0; i<v.size(); i++) {
          Argument dv = (Argument) v.elementAt(i);
          Vector dsv = parseDelegateSpecs(p.get(context, dv.name+".delegate"));
          for (int j=0; j<dsv.size(); j++) {
            DelegateSpec ds = (DelegateSpec) dsv.elementAt(j);

            String slotdoc = (String)p.get(context, ds.name+".doc");
            if (slotdoc!=null) 
              out.println("  /** "+slotdoc+" **/");

            out.println("  "+ds.type+" "+ds.name+
                        "("+unparseArguments(ds.args,true)+");");
          }
        }
      }

      out.println("  // introspection and construction");
      out.println("  /** the method of factoryClass that creates this type **/");
      out.println("  public static final String factoryMethod = \"new"+className+"\";");
      out.println("  /** the (mutable) class type returned by factoryMethod **/");
      out.println("  public static final String mutableClass = \""+
                  findPackage(context)+".New"+className+"\";");
      out.println("  /** the factory class **/");
      out.println("  public static final Class factoryClass = "+findPackage("global")+".PropertyGroupFactory.class;");

      out.println("  /** the (immutable) class type returned by domain factory **/");
      out.println("  public static final Class primaryClass = "+
                  findPackage(context)+"."+className+".class;");

      out.println("  public static final String assetSetter = \"set"+
                  className+"\";");
      out.println("  public static final String assetGetter = \"get"+
                  className+"\";");
      out.println("  /** The Null instance for indicating that the PG definitely has no value **/");
      out.println("  public static final "+className+" nullPG = new Null_"+className+"();");
      writeNullClass(out, context, className);
      writeFutureClass(out, context, className);
      out.println("}");
    }

    void writeNullClass(PrintWriter out, String context, String className) {
      out.println();
      // null class implementation
      out.println("/** Null_PG implementation for "+className+" **/");
      out.println("static final class Null_"+className+"\n"+
                  "  implements "+className+", Null_PG\n"+
                  "{");

      // declare the slots
      Vector slotspecs = getAllSlotSpecs(context);
      Enumeration se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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
          out.println("  /** "+slotdoc+" **/");
        if (as!=null) {
          out.println("  public "+type+" "+as.getterSpec()+"{ throw new UndefinedValueException(); }");
        } else {
          out.println("  public "+type+" get"+clname+"() { throw new UndefinedValueException(); }");
        }

        // extra collection type api
        if (etype != null) {
          out.println("  public boolean in"+clname+"("+etype+" element) { return false; }");
          out.println("  public "+etype+"[] get"+clname+"AsArray() { return null; }");
          out.println("  public "+etype+" getIndexed"+clname+"(int index) { throw new UndefinedValueException(); }");
        }
      }      

      Vector v = getAllDelegateSpecs(context);
      if (v != null) {
        for (int i =0; i<v.size(); i++) {
          Argument dv = (Argument) v.elementAt(i);

          // define delegate getter and setter if non-automatic
          String autop = (String)p.get(context, dv.name+".auto");
          // if it isn't automatic, define the setter and getter
          if (!(autop != null && Boolean.valueOf(autop).booleanValue())) {
            out.println("  public "+dv.type+" get"+toClassName(dv.name)+"() {\n"+
                        "    throw new UndefinedValueException();\n"+
                        "  }");
            out.println("  public void set"+toClassName(dv.name)+"("+dv.type+" _"+dv.name+") {\n"+
                        "    throw new UndefinedValueException();\n"+
                        "  }");
          }

          Vector dsv = parseDelegateSpecs(p.get(context, dv.name+".delegate"));
          for (int j=0; j<dsv.size(); j++) {
            DelegateSpec ds = (DelegateSpec) dsv.elementAt(j);
            out.println("  public "+ds.type+" "+ds.name+
                        "("+unparseArguments(ds.args,true)+") {"+
                        " throw new UndefinedValueException(); "+
                        "}");
          }
        }
      }


      // TimePhased getters
      if (p.get(context, PGParser.TIMEPHASED) != null) {
        out.println("  public long getStartTime() { throw new UndefinedValueException(); }");
        out.println("  public long getEndTime() { throw new UndefinedValueException(); }");
      }

      out.println("  public Object clone() throws CloneNotSupportedException {\n"+
                  "    throw new CloneNotSupportedException();\n"+
                  "  }");
      out.println("  public NewPropertyGroup unlock(Object key) { return null; }");
      out.println("  public PropertyGroup lock(Object key) { return null; }");
      out.println("  public PropertyGroup lock() { return null; }");
      out.println("  public PropertyGroup copy() { return null; }");
      out.println("  public Class getPrimaryClass(){return primaryClass;}");
      out.println("  public String getAssetGetMethod() {return assetGetter;}");
      out.println("  public String getAssetSetMethod() {return assetSetter;}");
      out.println("  public Class getIntrospectionClass() {\n"+
                  "    return "+className+"Impl.class;\n"+
                  "  }");

      // implement PG basic api - null never has DQ
      out.println();
      out.println("  public boolean hasDataQuality() { return false; }");

      if (hasDQ(context)) {     // if the class has it, we need to implement the getter
        out.println("  public org.cougaar.domain.planning.ldm.dq.DataQuality getDataQuality() { return null; }");
      }
      out.println("}");

    }

    void writeFutureClass(PrintWriter out, String context, String className) {
      out.println();
      // null class implementation
      out.println("/** Future PG implementation for "+className+" **/");
      out.println("public final static class Future\n"+
                  "  implements "+className+", Future_PG\n"+
                  "{");
      // declare the slots
      Vector slotspecs = getAllSlotSpecs(context);
      Enumeration se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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
          out.println("  /** "+slotdoc+" **/");
        if (as!=null) {
          out.println("  public "+type+" "+as.getterSpec()+"{\n"+
                      "    waitForFinalize();\n"+
                      "    return _real."+as.getterCall()+";\n"+
                      "  }");
        } else {
          out.println("  public "+type+" get"+clname+"() {\n"+
                      "    waitForFinalize();\n"+
                      "    return _real.get"+clname+"();\n"+
                      "  }");
        }


        // extra collection type api
        if (etype != null) {
          out.println("  public boolean in"+clname+"("+etype+" element) {\n"+
                      "    waitForFinalize();\n"+
                      "    return _real.in"+clname+"(element);\n"+
                      "  }");
          out.println("  public "+etype+"[] get"+clname+"AsArray() {\n"+
                      "    waitForFinalize();\n"+
                      "    return _real.get"+clname+"AsArray();\n"+
                      "  }");
          out.println("  public "+etype+" getIndexed"+clname+"(int index) {\n"+
                      "    waitForFinalize();\n"+
                      "    return _real.getIndexed"+clname+"(index);\n"+
                      "  }");
        }
      }      

      Vector v = getAllDelegateSpecs(context);
      if (v != null) {
        for (int i =0; i<v.size(); i++) {
          Argument dv = (Argument) v.elementAt(i);
          Vector dsv = parseDelegateSpecs(p.get(context, dv.name+".delegate"));
          for (int j=0; j<dsv.size(); j++) {
            DelegateSpec ds = (DelegateSpec) dsv.elementAt(j);
            out.println("  public "+ds.type+" "+ds.name+
                        "("+unparseArguments(ds.args,true)+") {\n"+
                        "    waitForFinalize();\n"+
                        "    "+ ("void".equals(ds.type)?"":"return ")+"_real."+ds.name+
                        "("+unparseArguments(ds.args,false)+");\n"+
                        "  }");
          }
        }
      }



      // TimePhased getters
      if (p.get(context, PGParser.TIMEPHASED) != null) {
        out.println("  public long getStartTime() {\n" + 
                    "    waitForFinalize();\n"+
                    "    return _real.getStartTime();\n"+
                    "  }");
        out.println("  public long getEndTime() {\n" + 
                    "    waitForFinalize();\n"+
                    "    return _real.getEndTime();\n"+
                    "  }");
      }
      
      out.println("  public Object clone() throws CloneNotSupportedException {\n"+
                  "    throw new CloneNotSupportedException();\n"+
                  "  }");
      out.println("  public NewPropertyGroup unlock(Object key) { return null; }");
      out.println("  public PropertyGroup lock(Object key) { return null; }");
      out.println("  public PropertyGroup lock() { return null; }");
      out.println("  public PropertyGroup copy() { return null; }");
      out.println("  public Class getPrimaryClass(){return primaryClass;}");
      out.println("  public String getAssetGetMethod() {return assetGetter;}");
      out.println("  public String getAssetSetMethod() {return assetSetter;}");
      out.println("  public Class getIntrospectionClass() {\n"+
                  "    return "+className+"Impl.class;\n"+
                  "  }");

      // Futures do not have data quality, though the replacement instance
      // may.
      out.println("  public synchronized boolean hasDataQuality() {\n"+
                  "    return (_real!=null) && _real.hasDataQuality();\n"+
                  "  }");
      if (hasDQ(context)) {     // if the class has it, we need to implement the getter
        out.println("  public synchronized org.cougaar.domain.planning.ldm.dq.DataQuality getDataQuality() {\n"+
                    "    return (_real==null)?null:(_real.getDataQuality());\n"+
                    "  }");
      }

      out.println();
      out.println("  // Finalization support");
      out.println("  private "+className+" _real = null;");
      out.println("  public synchronized void finalize(PropertyGroup real) {\n"+
                  "    if (real instanceof "+className+") {\n"+
                  "      _real=("+className+") real;\n"+
                  "      notifyAll();\n"+
                  "    } else {\n"+
                  "      throw new IllegalArgumentException(\"Finalization with wrong class: \"+real);\n"+
                  "    }\n"+
                  "  }");
      out.println("  private synchronized void waitForFinalize() {\n"+
                  "    while (_real == null) {\n"+
                  "      try {\n"+
                  "        wait();\n"+
                  "      } catch (InterruptedException _ie) {}\n"+
                  "    }\n"+
                  "  }");
      out.println("}");
    }


    void writeSetterIfc(PrintWriter out, String context, String className) {
      out.println("/** Additional methods for "+className);
      out.println(" * offering mutators (set methods) for the object's owner");
      out.println(" **/");
      out.println();
      doPackaging(out, context);

      String importstr=(String)p.get(context,"import");
      if (importstr!=null) {
        Vector v = explode(importstr, ',');
        Enumeration ves = v.elements();
        while (ves.hasMoreElements()) {
          String ve=(String)ves.nextElement();
          out.println("import "+ve+";");
        }
      }
      out.println();

      String newclassName = "New"+className;

      // figure out what we're supposed to extend
      String extendstring;
      if (p.get(context, PGParser.TIMEPHASED) == null) {
        extendstring = "extends "+className+ ", NewPropertyGroup";
      } else {
        extendstring = "extends "+className+ ", NewTimePhasedPropertyGroup";
      }
      
      if (hasDQ(context)) {     // if the class has it, we need to implement the getter
        extendstring = extendstring+ ", org.cougaar.domain.planning.ldm.dq.HasDataQuality";
      }

      out.println("public interface "+newclassName+" "+extendstring+" {");
      // declare the slots
      Vector slotspecs = getAllSlotSpecs(context);
      Enumeration se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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
          out.println("  /** @deprecated "+depp+" **/");
        if (as != null) {
          out.println("  void "+as.setterSpec()+";");
        } else {
          out.println("  void set"+toClassName(name)+"("+
                      type+" "+name+");");
        }
        // mutators for collection types
        if (etype != null) {
          out.println("  void clear"+toClassName(name)+"();");
          out.println("  boolean removeFrom"+toClassName(name)+"("+etype+" _element);");
          out.println("  boolean addTo"+toClassName(name)+"("+etype+" _element);");
        }

        if (as != null) {
          // handler installation method
          String htype = as.handlerName();
          out.println("  void set"+htype+"("+className+"."+htype+" handler);");
          out.println("  "+className+"."+htype+" get"+htype+"();");
        }

      }

      // delegation
      {
        Vector v = getAllDelegateSpecs(context);
        if (v != null) {
          for (int i =0; i<v.size(); i++) {
            Argument dv = (Argument) v.elementAt(i);
            
            // define delegate getter and setter if non-automatic
            String autop = (String)p.get(context, dv.name+".auto");
            // if it isn't automatic, define the setter and getter
            if (!(autop != null && Boolean.valueOf(autop).booleanValue())) {
              out.println("  public "+dv.type+" get"+toClassName(dv.name)+"();");
              out.println("  public void set"+toClassName(dv.name)+"("+dv.type+" _"+dv.name+");");
            }
            
          }
        }
      }

      out.println("}");
    }

    Vector getAllSlotSpecs(String context) {
      Vector slotspecs = getLocalSlotSpecs(context);
      String exts = p.get(context, "extends");
      
      if (exts != null && exts.length() > 0) {
        Vector superspecs = getAllSlotSpecs(exts);
        Enumeration sss = superspecs.elements();
        while (sss.hasMoreElements()) {
          slotspecs.addElement(sss.nextElement());
        }
      }

      return slotspecs;
    }        

    Vector getLocalSlotSpecs(String context) {
      return parseSlots(p.get(context, "slots"));
    }

    Vector getAllDelegateSpecs(String context) {
      Vector slotspecs = getLocalDelegateSpecs(context);
      String exts = p.get(context, "extends");
      if (exts != null && exts.length() > 0) {
        Vector superspecs = getAllDelegateSpecs(exts);
        Enumeration sss = superspecs.elements();
        while (sss.hasMoreElements()) {
          slotspecs.addElement(sss.nextElement());
        }
      }
      return slotspecs;
    }        
    
    Vector getLocalDelegateSpecs(String context) {
      return parseArguments(p.get(context, "delegates"));
    }

    Vector parseSlots(String slotstr) {
      if (slotstr == null) slotstr ="";
      int s=0;
      char[] chars = slotstr.toCharArray();
      int l = chars.length;
      int parens = 0;
      Vector slotds = new Vector();

      int p;
      for (p = 0; p<l; p++) {
        char c = chars[p];
        // need to parse out parens
        if (c == '(') {
          parens++;
        } else if (c == ')') {
          parens--;
        } else if (c == ',' && parens==0) {
          slotds.addElement(slotstr.substring(s,p).trim());
          s = p+1;
        } else {
          // just advance
        }
      }
      if (p > s) {
        slotds.addElement(slotstr.substring(s,p).trim());
      }
      return slotds;
    }


    void writeImpl(PrintWriter out, String context, String className) {
      out.println("/** Implementation of "+className+".");
      out.println(" *  @see "+className);
      out.println(" *  @see New"+className);
      out.println(" **/");
      out.println();
      doPackaging(out, context);
      out.println("import java.io.ObjectOutputStream;");
      out.println("import java.io.ObjectInputStream;");
      out.println("import java.io.IOException;");
      out.println("import java.beans.PropertyDescriptor;");
      out.println("import java.beans.IndexedPropertyDescriptor;");

      String importstr=(String)p.get(context,"import");
      if (importstr!=null) {
        Vector v = explode(importstr, ',');
        Enumeration ves = v.elements();
        while (ves.hasMoreElements()) {
          String ve=(String)ves.nextElement();
          out.println("import "+ve+";");
        }
      }
      out.println();
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
      out.println("public"+(hasDQ(context)?"":" final")+" class "+implclassName+" extends "+adapter+"\n"+
                  "  implements "+newclassName+", Cloneable\n{");

      // constructor
      out.println("  public "+implclassName+"() {");
      Vector v = getAllDelegateSpecs(context);
      if (v != null) {
        for (int i =0; i<v.size(); i++) {
          Argument dv = (Argument) v.elementAt(i);

          // define delegate getter and setter if non-automatic
          String autop = (String)p.get(context, dv.name+".auto");
          // if it is automatic, pre-initialize it
          if (autop != null && Boolean.valueOf(autop).booleanValue()) {
            out.println("    "+dv.name+" = new "+dv.type+"(this);");
          }
        }
      }
      out.println("  };");
      out.println();
      
      boolean timephased = (p.get(context, PGParser.TIMEPHASED) != null);
      if (timephased) {
        //handle time phased support separately because getters != setters
        out.println(NewTimeSpanText);
      }
        
      Vector slotspecs = getAllSlotSpecs(context);

      // declare the slots (including "inherited" ones)
      out.println("  // Slots\n");
      Enumeration se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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
          out.println("  private transient "+className+"."+htype+" "+var+" = null;");
          
          // handler installation
          out.println("  public void set"+htype+"("+className+"."+htype+" handler) {\n"+
                      "    "+var+" = handler;\n"+
                      "  }");
          out.println("  public "+className+"."+htype+" get"+htype+"() {\n"+
                      "    return "+var+";\n"+
                      "  }");

          // get dispatcher
          if (getter != null){
            out.println(getter);
          } else {
            out.println("  public "+type+" "+as.getter(var));
          }

          // set dispatcher
          if (setter != null){
            out.println(setter);
          } else {
            out.println("  public void "+as.setter(var));
          }
          
        } else {

          // storage
          if (var != null) {
            String init = (String)p.get(context,name+".init");
            out.print("  private "+type+" "+var);
            if (init != null) {
              out.print(" = new "+init+"()");
            }
            out.println(";");

            // Normal getter
            if (getter != null) {
              out.println(getter);
            } else {
              out.println( "  public "+type+" get"+toClassName(name)+"(){ "+
                           "return "+var+"; }");
            }

            // test for Collection types
            if (etype != null) {
              String clname = toClassName(name);
              out.println("  public boolean in"+clname+"("+etype+" _element) {\n"+
                          "    return "+var+".contains(_element);\n"+
                          "  }");

              out.println("  public "+etype+"[] get"+clname+"AsArray() {");
              out.println("    if ("+var+" == null) return new "+etype+"[0];");
              out.println("    int l = "+var+".size();");
              out.println("    "+etype+"[] v = new "+etype+"[l];");
              out.println("    int i=0;");
              out.println("    for (Iterator n="+var+".iterator(); n.hasNext(); ) {");
              out.println("      v[i]=("+etype+") n.next();");
              out.println("      i++;");
              out.println("    }");
              out.println("    return v;");
              out.println("  }");
          
              out.println("  public "+etype+" getIndexed"+clname+"(int _index) {");
              out.println("    if ("+var+" == null) return null;");
              out.println("    for (Iterator _i = "+var+".iterator(); _i.hasNext();) {");
              out.println("      "+etype+" _e = ("+etype+") _i.next();");
              out.println("      if (_index == 0) return _e;");
              out.println("      _index--;");
              out.println("    }");
              out.println("    return null;");
              out.println("  }");
            }

            // setter
            if (setter != null) {
              out.println(setter);
            } else {
              out.println("  public void set"+toClassName(name)+"("+type+" "+name+") {");
              // special handling for string setters
              String isUnique = p.get(context, name+".unique");
              if (type.equals("String") &&
                  (isUnique == null || isUnique.equals("false"))){
                out.println("    if ("+name+"!=null) "+name+"="+name+".intern();");
              }
              out.println("    "+var+"="+name+";");
              out.println("  }");
            }

            // mutators for collection types
            if (etype != null) {
              out.println("  public void clear"+toClassName(name)+"() {\n"+
                          "    "+var+".clear();\n"+
                          "  }");
              out.println("  public boolean removeFrom"+toClassName(name)+"("+etype+" _element) {\n"+
                          "    return "+var+".remove(_element);\n"+
                          "  }");
              out.println("  public boolean addTo"+toClassName(name)+"("+etype+" _element) {\n"+
                          "    return "+var+".add(_element);\n"+
                          "  }");
            }

          } else {                // var is specified as empty
            if (getter != null) out.println(getter);
            if (setter != null) out.println(setter);
          }
        }
      }

      // delgates
      {
        Vector vs = getAllDelegateSpecs(context);
        if (vs != null) {
          out.println();
          for (int i =0; i<vs.size(); i++) {
            Argument dv = (Argument) vs.elementAt(i);

            out.println("  private "+dv.type+" "+dv.name+" = null;");

            // define delegate getter and setter if non-automatic
            String autop = (String)p.get(context, dv.name+".auto");
            // if it isn't automatic, define the setter and getter
            if (!(autop != null && Boolean.valueOf(autop).booleanValue())) {
              out.println("  public "+dv.type+" get"+toClassName(dv.name)+"() {\n"+
                          "    return "+dv.name+";\n"+
                          "  }");
              out.println("  public void set"+toClassName(dv.name)+"("+dv.type+" _"+dv.name+") {\n"+
                          "    if ("+dv.name+" != null) throw new IllegalArgumentException(\""+
                          dv.name+" already set\");\n"+
                          "    "+dv.name+" = _"+dv.name+";\n"+
                          "  }");
            }


            Vector dsv = parseDelegateSpecs(p.get(context, dv.name+".delegate"));
            for (int j=0; j<dsv.size(); j++) {
              DelegateSpec ds = (DelegateSpec) dsv.elementAt(j);
              out.println("  public "+ds.type+" "+ds.name+
                          "("+unparseArguments(ds.args,true)+") {"+
                          " "+ ("void".equals(ds.type)?"":"return ")+dv.name+"."+ds.name+
                          "("+unparseArguments(ds.args,false)+");"+
                          "  }");
            }
          }
        }
      }

      // copy constructor
      out.println();
      out.println("  public "+implclassName+"("+className+" original) {");
      se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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

        if (as != null) {
          // do nothing - clones don't get the handler automatically.
        } else {
          String var = (String)p.get(context,name+".var");
          if (var == null) { var = "the"+toClassName(name); } // set the default
          if (var.equals("")) { var = null; } // unset if specified as empty
          if (var != null) {
            out.println("    "+var+" = original.get"+toClassName(name)+"();");
          }
        }
      }
      out.println("  }");
      out.println();

      if (hasDQ(context)) {
        out.println("  public boolean hasDataQuality() { return false; }");
        out.println("  public org.cougaar.domain.planning.ldm.dq.DataQuality getDataQuality() { return null; }");
        /*
          // classes without runtime support do not implement NewHasDataQuality!
        out.println("  public void setDataQuality(org.cougaar.domain.planning.ldm.dq.DataQuality dq) {\n"+
                    "    throw new IllegalArgumentException(\"This instance does not support setting of DataQuality.\");\n"+
                    "  }");
        */
        out.println();
        out.println("  // static inner extension class for real DataQuality Support");
        out.println("  public final static class DQ extends "+className+"Impl implements org.cougaar.domain.planning.ldm.dq.NewHasDataQuality {");
        out.println("   public DQ() {\n"+ // never copy data quality
                    "    super();\n"+
                    "   }");

        out.println("   public DQ("+className+" original) {\n"+ // never copy data quality
                    "    super(original);\n"+
                    "   }");
        out.println("   public Object clone() { return new DQ(this); }");
        out.println("   private transient org.cougaar.domain.planning.ldm.dq.DataQuality _dq = null;");
        out.println("   public boolean hasDataQuality() { return (_dq!=null); }");
        out.println("   public org.cougaar.domain.planning.ldm.dq.DataQuality getDataQuality() { return _dq; }");
        out.println("   public void setDataQuality(org.cougaar.domain.planning.ldm.dq.DataQuality dq) { _dq=dq; }");
        out.println("   private void writeObject(ObjectOutputStream out) throws IOException {\n"+
                    "    out.defaultWriteObject();\n"+
                    "    if (out instanceof org.cougaar.core.cluster.persist.PersistenceOutputStream) out.writeObject(_dq);\n"+
                    "   }");
        out.println("   private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {\n"+
                    "    in.defaultReadObject();\n"+
                    "    if (in instanceof org.cougaar.core.cluster.persist.PersistenceInputStream) _dq=(org.cougaar.domain.planning.ldm.dq.DataQuality)in.readObject();\n"+
                    "   }");
        out.println("    ");
        out.println("    private final static PropertyDescriptor properties[]=new PropertyDescriptor[1];\n"+
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
        out.println("  }");
        out.println();


      } else {
        out.println("  public final boolean hasDataQuality() { return false; }");
      }
      out.println();

      // lock and unlock methods
      out.println("  private transient "+className+" _locked = null;");
      out.println("  public PropertyGroup lock(Object key) {");
      out.println("    if (_locked == null)");
      out.println("      _locked = new _Locked(key);");
      out.println("    return _locked; }");
      out.println("  public PropertyGroup lock() "+
                  "{ return lock(null); }");
      out.println("  public NewPropertyGroup unlock(Object key) "+
                  "{ return this; }");
      out.println();

      // clone method
      out.println("  public Object clone() throws CloneNotSupportedException {\n"+
                  //"    "+implclassName+" o = ("+implclassName+") super.clone();\n"+
                  //"    return new "+implclassName+"(this);\n"+
                  "    "+implclassName+" _tmp = new "+implclassName+"(this);");
      {
        Vector vs = getAllDelegateSpecs(context);
        if (vs != null) {
          for (int i =0; i<vs.size(); i++) {
            Argument dv = (Argument) vs.elementAt(i);
            out.println("    _tmp."+dv.name+" = ("+dv.type+") "+dv.name+".copy(_tmp);");
          }
        }
      }
      out.println("    return _tmp;\n"+
                  "  }");



      out.println();

      out.println("  public PropertyGroup copy() {\n"+
                  "    try {\n"+
                  "      return (PropertyGroup) clone();\n"+
                  "    } catch (CloneNotSupportedException cnse) { return null;}\n"+
                  "  }");
      out.println();

      // introspection methods
      out.println("  public Class getPrimaryClass() {\n"+
                  "    return primaryClass;\n"+
                  "  }");
      out.println("  public String getAssetGetMethod() {\n"+
                  "    return assetGetter;\n"+
                  "  }");
      out.println("  public String getAssetSetMethod() {\n"+
                  "    return assetSetter;\n"+
                  "  }");
      out.println();
      
      // Serialization
      boolean needSerialization = false;
      se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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

        String isUnique = p.get(context, name+".unique");
        if (type.equals("String") &&
            (isUnique == null || isUnique.equals("false"))){
          needSerialization = true;
          break;
        }
      }

      if (needSerialization) {
        out.println("  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {");
        out.println("    in.defaultReadObject();");
        se = slotspecs.elements();
        while (se.hasMoreElements()) {
          String slotspec = ((String)se.nextElement()).trim();
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

          if (as != null) {
            // serialized copies can't get the active slot remotely.
            continue;           
          }
          String var = (String)p.get(context,name+".var");
          if (var == null) { var = "the"+toClassName(name); } // set the default
          if (var.equals("")) { var = null; } // unset if specified as empty
          if (var != null) {
            String isUnique = p.get(context, name+".unique");
            if (type.equals("String") &&
                (isUnique == null || isUnique.equals("false"))){
              out.println("    if ("+var+"!= null) "+var+"="+var+".intern();");
            }
          }
        }

        out.println("  }");
        out.println();
      }

      writeBeanInfoBody(out, context, className);

      // inner (locked) class
      out.println("  private final class _Locked extends java.beans.SimpleBeanInfo\n"+
                  "    implements "+className+", Cloneable, LockedPG\n"+
                  "  {");
      out.println("    private transient Object theKey = null;");
      out.println("    _Locked(Object key) { ");
      out.println("      if (this.theKey == null){  ");
      out.println("        this.theKey = key; ");
      out.println("      } ");
      out.println("    }  ");
      out.println();
      out.println("    /** public constructor for beaninfo - probably wont work**/");
      out.println("    public _Locked() {}");
      out.println();
      out.println("    public PropertyGroup lock() { return this; }");
      out.println("    public PropertyGroup lock(Object o) { return this; }");
      out.println();
      out.println("    public NewPropertyGroup unlock(Object key) "+
                  "throws IllegalAccessException {");
      out.println("       if( theKey.equals(key) )");
      out.println("         return "+implclassName+".this;");
      out.println("       else ");
      out.println("         throw new IllegalAccessException(\"unlock: mismatched internal and provided keys!\");");
      out.println("    }");
      out.println();
      out.println("    public PropertyGroup copy() {\n"+
                  "      return new "+implclassName+"("+implclassName+".this);\n"+
                  "    }");
      out.println();
      out.println("    public Object clone() throws CloneNotSupportedException {\n"+
                  "      return new "+implclassName+"("+implclassName+".this);\n"+
                  "    }");
      out.println();

      if (timephased) {
        out.println("    public long getStartTime() { return " + 
                    implclassName + ".this.getStartTime(); }");
        out.println("    public long getEndTime() { return " +
                    implclassName + ".this.getEndTime(); }");
      }

      // dispatch getters
      se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
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
          out.println("    public "+as.rtype+" "+as.getterSpec()+" {\n"+
                      "      return "+implclassName+".this."+as.getterCall()+";\n"+
                      "    }");
        } else {
          out.println("    public "+type+" "+getter+"() { "+
                      "return "+implclassName+".this."+getter+"(); }");
        }

        // test for Collection types
        if (etype != null) {
          String clname = toClassName(name);
          String ref = implclassName+".this.";
          out.println("  public boolean in"+clname+"("+etype+" _element) {\n"+
                      "    return "+ref+"in"+clname+"(_element);\n"+
                      "  }");
          out.println("  public "+etype+"[] get"+clname+"AsArray() {");
          out.println("    return "+ref+"get"+clname+"AsArray();");
          out.println("  }");
          
          out.println("  public "+etype+" getIndexed"+clname+"(int _index) {");
          out.println("    return "+ref+"getIndexed"+clname+"(_index);");
          out.println("  }");
        }

      }

      {
        Vector vs = getAllDelegateSpecs(context);
        if (vs != null) {
          for (int i =0; i<vs.size(); i++) {
            Argument dv = (Argument) vs.elementAt(i);
            Vector dsv = parseDelegateSpecs(p.get(context, dv.name+".delegate"));
            for (int j=0; j<dsv.size(); j++) {
              DelegateSpec ds = (DelegateSpec) dsv.elementAt(j);
              out.println("  public "+ds.type+" "+ds.name+
                          "("+unparseArguments(ds.args,true)+") {\n"+
                          "    "+("void".equals(ds.type)?"":"return ")+implclassName+".this."+ds.name+
                          "("+unparseArguments(ds.args,false)+");\n"+
                          "  }");
            }
          }
        }
      }

      if (hasDQ(context)) {
        out.println("  public final boolean hasDataQuality() { return "+
                    implclassName+".this.hasDataQuality(); }");
        out.println("  public final org.cougaar.domain.planning.ldm.dq.DataQuality getDataQuality() { return "+
                    implclassName+".this.getDataQuality(); }");
      } else {
        out.println("  public final boolean hasDataQuality() { return false; }");
      }

      // introspection method of locked
      out.println("    public Class getPrimaryClass() {\n"+
                  "      return primaryClass;\n"+
                  "    }");
      out.println("    public String getAssetGetMethod() {\n"+
                  "      return assetGetter;\n"+
                  "    }");
      out.println("    public String getAssetSetMethod() {\n"+
                  "      return assetSetter;\n"+
                  "    }");
      out.println();

      // BeanInfo dispatchers
      out.println("    public PropertyDescriptor[] getPropertyDescriptors() {");
      out.println("      return properties;");
      out.println("    }");
      out.println();
      out.println("    public Class getIntrospectionClass() {");
      out.println("      return "+className+"Impl.class;");
      out.println("    }");
      out.println();
      
      out.println("  }");
      out.println();

      out.println("}");
    }

    void writeBeanInfoBody( PrintWriter out, String context, String className ) {
      
      Vector slotspecs = getAllSlotSpecs(context);

      // Add in time phased slots
      if (p.get(context, PGParser.TIMEPHASED) != null) {
        slotspecs.addElement("long start_time");
        slotspecs.addElement("long end_time");
      }

      // figure out how many property slots we need
      int l = 0;                // props
      int asc = 0;              // methods
      int cc = 0;               // collections (not used)
      Enumeration se = slotspecs.elements();
      while (se.hasMoreElements()) {
        String slotspec = ((String)se.nextElement()).trim();
        int s = slotspec.indexOf(" ");
        String type = slotspec.substring(0,s);
        String name = slotspec.substring(s+1);
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
        
      out.println("  private final static PropertyDescriptor properties[] = new PropertyDescriptor["+(l+cc)+"];");
      if ((l+cc)>0) {
        out.println("  static {");
        out.println("    try {");
        int i = 0;
        se = slotspecs.elements();
        while (se.hasMoreElements()) {
          String slotspec = ((String)se.nextElement()).trim();
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

          String clname = toClassName(context);

          if (as == null) {
            // non-active slots
            if (ct == null) {
              // plain slots
              out.println("      properties["+i+"]= new PropertyDescriptor(\""+
                          name+"\", "+
                          clname+".class, "+
                          "\"get"+toClassName(name)+"\", null);");
            } else {
              // collection slots
              out.println("      properties["+i+"]= new IndexedPropertyDescriptor(\""+
                          name+"\", "+
                          clname+".class, "+
                          "\"get"+toClassName(name)+"AsArray\", null, "+
                          "\"getIndexed"+toClassName(name)+"\", null);");
            }
            i++;
          }
        }
        out.println("    } catch (Exception e) { System.err.println(\"Caught: \"+e); e.printStackTrace(); }");
        out.println("  };");
      }
      out.println();
      out.println("  public PropertyDescriptor[] getPropertyDescriptors() {");
      out.println("    return properties;");
      out.println("  }");
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
        writeCR(out);
        writeGetterIfc(out, context, className);
        out.close();

        outname = "New" + className.toString() + ".java";
        debug("Writing SetterIfc \""+context+"\" to \""+outname+"\"");
        noteFile(outname);
        fos = new FileOutputStream(new File(getTargetDir(),outname));
        osw = new OutputStreamWriter(fos);
        out = new PrintWriter(osw);
        writeCR(out);
        writeSetterIfc(out, context, className);
        out.close();
      }

      outname = className.toString() + "Impl.java";
      debug("Writing Impl \""+context+"\" to \""+outname+"\"");
      noteFile(outname);
      fos = new FileOutputStream(new File(getTargetDir(),outname));
      osw = new OutputStreamWriter(fos);
      out = new PrintWriter(osw);
      writeCR(out);
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
      writeCR(out);
      
      out.println("/** AbstractFactory implementation for Properties.");
      out.println(" * Prevents clients from needing to know the implementation");
      out.println(" * class(es) of any of the properties.");
      out.println(" **/");
      out.println();
      doPackaging(out,"global");

      out.println();
      String xclause = "";
      String exts = p.get("global", "factoryExtends");
      if (exts != null) {
        xclause = "extends "+exts+" ";
      }
      out.println("public class PropertyGroupFactory "+xclause+"{");

      Enumeration contexts = p.getContexts();
      while ( contexts.hasMoreElements()) {
        String context = (String) contexts.nextElement();
        if (! context.equals("global") && 
            (p.get(context, "abstract") == null)) {
          String newclassName = "New"+context;
          String icn = context+"Impl";
          out.println("  // brand-new instance factory");
          out.println("  public static "+newclassName+" new"+context+"() {");
          out.println("    return new "+icn+"();");
          out.println("  }");
          
          boolean timephased = (p.get(context, PGParser.TIMEPHASED) != null);
          if (timephased) {
            out.println("  // brand-new instance factory");
            out.println("  public static PropertyGroupSchedule new"+context+
                        "Schedule() {");
            out.println("    return new PropertyGroupSchedule(new"+context+"());");
            out.println("  }");
          }

          out.println("  // instance from prototype factory");
          out.println("  public static "+newclassName+" new"+context+"("+context+" prototype) {");
          out.println("    return new "+icn+"(prototype);");
          out.println("  }");
          out.println();

          if (timephased) {
            out.println("  // instance from prototype factory");
            out.println("  public static PropertyGroupSchedule new"+context+
                        "Schedule("+context+" prototype) {");
            out.println("    return new PropertyGroupSchedule(new"+context+"(prototype));");
            out.println("  }");
          }

        }
      }
      out.println("  /** Abstract introspection information.");
      out.println("   * Tuples are {<classname>, <factorymethodname>}");
      out.println("   * return value of <factorymethodname> is <classname>.");
      out.println("   * <factorymethodname> takes zero or one (prototype) argument.");
      out.println("   **/");
      out.println("  public static String properties[][]={");
      contexts = p.getContexts();
      while ( contexts.hasMoreElements()) {
        String context = (String) contexts.nextElement();
        if (! context.equals("global") && 
            (p.get(context, "abstract") == null)) {
          String newclassName = "New"+context;
          String pkg = findPackage(context);
          out.print("    {\""+pkg+"."+context+"\", \"new"+context+"\"}");
          if (p.get(context, PGParser.TIMEPHASED) != null) {
            out.print(",");
            out.println();
            out.print("    {\""+pkg+".PropertyGroupSchedule\", \"new"+context+"Schedule\"}");
          }
          if (contexts.hasMoreElements())
            out.print(",");
          out.println();
        }
      }
      out.println("  };");
      

      out.println("}");
      out.close();
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
      Vector arglist = explode(slotname.substring(es+1, ee), ',');
      
      Collection args = new ArrayList();
      Collection types = new ArrayList();
      
      for (Enumeration e = arglist.elements(); e.hasMoreElements();) {
        String arg = ((String)e.nextElement()).trim();
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
      Vector args;
      public DelegateSpec(String t, String n, Vector a) {
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
    
    public String unparseArguments(Vector args, boolean typesToo) {
      int l = args.size();
      String s = "";
      for (int i = 0; i<l; i++) {
        Argument arg = (Argument) args.elementAt(i);
        if (i!=0) s=s+", ";
        if (typesToo) s=s+arg.type+" ";
        s=s+arg.name;
      }
      return s;
    }        

    public Vector parseArguments(String s) {
      if (s == null)
        return new Vector(0);
      Vector argstrs = explode(s, ',');
      int l = argstrs.size();
      Vector args = new Vector(l);
      for (int i = 0; i<l; i++) {
        args.addElement(parseArgument((String)argstrs.elementAt(i)));
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
      Vector args = parseArguments(s.substring(p2+1,p3));
      return new DelegateSpec(type, name, args);
    }
    
    public Vector parseDelegateSpecs(String s) {
      Vector v = explode(s, ';');
      int l = v.size();
      Vector ds = new Vector(l);
      for (int i = 0; i<l; i++) {
        String x = ((String)v.elementAt(i)).trim();
        if (x.length()>0) {
          ds.addElement(parseDelegateSpec(x));
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
      writeCR(out);
      
      out.println("/** Abstract Asset Skeleton implementation");
      out.println(" * Implements default property getters, and additional property");
      out.println(" * lists.");
      out.println(" * Intended to be extended by org.cougaar.domain.planning.ldm.asset.Asset");
      out.println(" **/");
      out.println();
      doPackaging(out,"global");
      out.println("import java.io.Serializable;");
      out.println("import java.beans.PropertyDescriptor;" );
      out.println("import java.beans.IndexedPropertyDescriptor;" );
      out.println();
      String baseclass = p.get("global","skeletonBase");
      if (baseclass == null) baseclass = "org.cougaar.domain.planning.ldm.asset.Asset";
      out.println("public abstract class AssetSkeleton extends "+baseclass+" {");
      out.println();
      // default constructor
      out.println("  protected AssetSkeleton() {}");
      out.println();
      // copy constructor
      out.println("  protected AssetSkeleton(AssetSkeleton prototype) {\n"+
                  "    super(prototype);\n"+
                  "  }");
      out.println();

      out.println("  /**                 Default PG accessors               **/");
      out.println();

      Enumeration contexts = p.getContexts();
      while ( contexts.hasMoreElements()) {
        String context = (String) contexts.nextElement();
        if (! context.equals("global") && 
            (p.get(context, "abstract") == null)) {
          out.println("  /** Search additional properties for a "+
                      context+
                      " instance.");
          out.println("   * @return instance of "+context+" or null.");
          out.println("   **/");
          
          boolean timephased = (p.get(context, PGParser.TIMEPHASED) != null);
          String timeVar = "";

          if (timephased) {
            timeVar = "long time";
          }
          out.println("  public "+context+" get"+context+"("+timeVar+")");
          out.println("  {");

          if (timephased) {
            timeVar = ", time";
          }
          out.println("    "+context+" _tmp = ("+context+") resolvePG("+
                      context+".class"+timeVar+");");
          out.println("    return (_tmp=="+context+".nullPG)?null:_tmp;");
          out.println("  }");
          out.println();

          out.println("  /** Test for existence of a "+context+"\n"+
                      "   **/");
          if (timephased) {
            timeVar = "long time";
          }
          out.println("  public boolean has"+context+"("+timeVar+") {\n");

          if (timephased) {
            timeVar = "time";
          }
          out.println("    return (get"+context+"("+timeVar+") != null);\n"+
                      "  }");
          out.println();

          String vr = "a"+context;
          out.println("  /** Set the "+context+" property.\n"+
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
          out.println();

          if (timephased) {
            out.println("  public PropertyGroupSchedule get"+context+"Schedule()");
            out.println("  {");
            out.println("    return searchForPropertyGroupSchedule("+context+
                      ".class);");
            out.println("  }");
            out.println();

            out.println("  public void set"+context+"Schedule(PropertyGroupSchedule schedule) {\n"+
                      "    removeOtherPropertyGroup("+context+".class);\n"+
                      "    if (schedule != null) {\n"+
                      "      addOtherPropertyGroupSchedule(schedule);\n"+
                      "    }\n"+
                      "  }");
            out.println();
          }
        }
      }

      out.println("}");
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

      out.println("- - - - - List of machine generated PropertyGroups - - - - -");
      
      HashMap permuted = new HashMap();

      List l =new ArrayList(p.table.keySet());
      Collections.sort(l);
      for (Iterator i = l.iterator(); i.hasNext();) {
        String context = (String) i.next();
        out.println("* "+context);
        String doc = p.get(context,"doc");
        if (doc != null) {
          out.println(doc);
        }

        Vector slotspecs = getAllSlotSpecs(context);
        Enumeration se = slotspecs.elements();
        while (se.hasMoreElements()) {
          String slotspec = ((String)se.nextElement()).trim();

          int s = slotspec.indexOf(" ");
          String type = slotspec.substring(0, s);
          String name = slotspec.substring(s+1);
          ActiveSlot as = parseActiveSlot(slotspec);
          if (as != null) {
            name = as.name;
          }
          out.print("    "+type+" "+toClassName(name)+";");
          String slotdoc = (String)p.get(context, name+".doc");
          if (slotdoc != null) out.print("\t//"+slotdoc);
          out.println();


          List pent = (List) permuted.get(name);
          if (pent == null) {
            pent = new ArrayList();
            permuted.put(name, pent);
          } 
          pent.add(context);
        }
      }

      out.println("- - - - - Permuted index of PropertyGroupSlots - - - - -");
      List k = new ArrayList(permuted.keySet());
      Collections.sort(k);
      for (Iterator i = k.iterator(); i.hasNext();) {
        String name = (String) i.next();
        out.println("* "+toClassName(name));
        List cs = (List) permuted.get(name);
        for (Iterator j = cs.iterator(); j.hasNext();) {
          String context = (String) j.next();
          out.println("    "+context);
        }
      }

      out.close();
    }

    public void write() throws Exception {
      grokGlobal();
      Enumeration contexts = p.getContexts();
      while ( contexts.hasMoreElements()) {
        String context = (String) contexts.nextElement();
        if (! context.equals("global") &&
            (p.get(context, "abstract") == null)) {
          writePropertyGroup(context);
        }
      }
      if (writeAbstractFactory)
        writeFactory();
      writeAsset();
      
      writeIndex();
    }
  }

  /** arguments to the writer **/
  private String arguments[];
  
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
        stream = new FileInputStream(filename);
      } else {
        debug("Using ClassLoader to read \""+filename+"\".");
        stream = 
          getClass().getClassLoader().getResourceAsStream(filename);
      }

      PGParser p = new PGParser(isVerbose);
      p.parse(stream);
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

  void writeCR(PrintWriter out) {
    out.println("/*\n"+
                " * <copyright>\n"+
                " * Copyright 1997-2000 Defense Advanced Research Projects Agency (DARPA)\n"+
                " * and ALPINE (A BBN Technologies (BBN) and Raytheon Systems Company\n"+
                " * (RSC) Consortium). This software to be used in accordance with the\n"+
                " * COUGAAR license agreement.  The license agreement and other\n"+
                " * information on the Cognitive Agent Architecture (COUGAAR) Project can\n"+
                " * be found at http://www.cougaar.org or email: info@cougaar.org.\n"+
                " * </copyright>\n"+
                " */");
    out.println();
    out.println("// source machine generated at "+new java.util.Date()+" - Do not edit");
    out.print("/* @"+"generated */");
    out.println();
  }

  public PGWriter(String args[]) {
    arguments=args;
  }


  public static void main(String args[]) {
    PGWriter mw = new PGWriter(args);
    mw.start();
  }
}
