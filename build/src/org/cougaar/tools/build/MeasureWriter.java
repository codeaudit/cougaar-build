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
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.*;
import java.io.File;

class MeasureWriter {
  class Parser {
    InputStream s;
    public Parser(InputStream s) {
      this.s = s;
    }

    Hashtable table = new Hashtable();
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
    public void _put(String context, String key, Object value) {
      Hashtable ct = getContext(context);
      ct.put(key, value);
    }

    public String get(String context, String key) {
      Hashtable ct = getContext(context);
      return (String)ct.get(key);
    }

    public Object _get(String context, String key) {
      Hashtable ct = getContext(context);
      return ct.get(key);
    }      

    public Enumeration getContexts() {
      return table.keys();
    }

    public void parse() throws IOException {
      InputStreamReader isr = new InputStreamReader(s);
      BufferedReader br = new BufferedReader(isr);
      String context = "global";
      int i;

      getContext(context);
      for (String line = br.readLine(); line != null; line=br.readLine()) {
        line = line.trim();
        if (line.length() == 0 || line.startsWith(";")) {
          // empty or comment line
        } else {
          while (line.endsWith("\\")) {
            String more = br.readLine();
            if (more == null) throw new IOException("Unexpected EOF");
            line = line.substring(0,line.length()-1)+more.trim();
          }

          if (line.startsWith("[") && line.endsWith("]")) { // new context
            context = line.substring(1,line.length()-1);
            debug("Parsing Measure \""+context+"\"");
            getContext(context);  // setup the new context
          }
          else if ( (i = line.indexOf("=")) >= 0) { // key/value pair
            String key = line.substring(0, i);
            String value = line.substring(i+1, line.length());
            put(context, key, value);
          } 
          else {
            debug("Bad line \""+line+"\"");
          }
        }
      }
    }
  }

  class Writer {
    PrintWriter filelist = null;

    Parser p;
    public Writer(Parser p) {
      this.p = p;
      try {
        filelist = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(targetdir,"measures.gen"))));
        noteFile("measures.gen");
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

    String unpluralize(String s) {
      if (s.endsWith("s")) 
        return s.substring(0,s.length()-1);
      if (s.equals("feet"))
        return "foot";
      return s;
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
      out.println("/* @"+"generated */");
      out.println();
    }

    void writePrelude(PrintWriter out, String context, String className, String outname) {
      writeCR(out);
      out.println("/** Immutable implementation of "+className+".");
      out.println(" **/");
      out.println();
      out.println();
      out.println("package org.cougaar.domain.planning.ldm.measure;");
      out.println("import java.io.*;");
      out.println();
    }
    
    Vector explode(String s) {
     Vector r = new Vector();
     int last = 0;
     for (int i = 0; i < s.length(); i++) {
       char c = s.charAt(i);
       if (c == ' ') {
         if (i>last)
           r.addElement(s.substring(last,i));
         last = i+1;
       }
     }
     if (s.length()>last) r.addElement(s.substring(last));
     return r;
    }

    Vector explodeC(String s, char burst) {
     Vector r = new Vector();
     int last = 0;
     for (int i = 0; i < s.length(); i++) {
       char c = s.charAt(i);
       if (c == burst) {
         if (i>last)
           r.addElement(s.substring(last,i));
         last = i+1;
       }
     }
     if (s.length()>last) r.addElement(s.substring(last));
     return r;
    }

    /** return the value factor required to convert a value from the base unit of
     * the context to the requested unit.
     **/
    double computeToFactor(String context, String unit) {
      String base = getBaseUnit(context);
      if (base.equals(unit)) return 1.0;
      String tobase = p.get(context, "to_"+unit);
      if (tobase != null) {
        return Double.valueOf(tobase).doubleValue();
      } else {
        String frombase = p.get(context, "from_"+unit);
        if (frombase == null) {
          // no info here!  Try what we extend:
          return computeToFactor(p.get(context,"extends"), unit);
        }
        return 1.0/Double.valueOf(frombase).doubleValue();
      }
    }

    /** return the value factor required to convert a value from the requested unit
     * to the base unit of the context.
     **/
    double computeFromFactor(String context, String unit) {
      String base = getBaseUnit(context);
      if (base.equals(unit)) return 1.0;
      String frombase = p.get(context, "from_"+unit);
      if (frombase != null) {
        return Double.valueOf(frombase).doubleValue();
      } else {
        String tobase = p.get(context, "to_"+unit);
        if (tobase == null) {
          // no info here!  Try what we extend:
          return computeFromFactor(p.get(context,"extends"), unit);
        }
        return 1.0/Double.valueOf(tobase).doubleValue();
      }
    }

    Vector getUnitV(String context) {
      Vector v = (Vector) p._get(context,"_units");
      if (v != null) return v;
      String units = p.get(context, "units");
      if (units != null) {
        v = explode(units);
        p._put(context, "_units", v);
        return v;
      }
      String ext = p.get(context, "extends");
      if (ext != null) {
        v = getUnitV(ext);
        if (v != null) {
          p._put(context, "_units", v);
        }
        return v;
      }
      return null;
    }

    String getBaseUnit(String context) {
      String base = p.get(context, "base");
      if (base == null) {
        Vector units = getUnitV(context);
        return (String)units.elementAt(0);
      } else {
        return base;
      }
    }

    void writeClass(PrintWriter out, String context, String className) {
      String isDep = p.get(context,"deprecated");
      if (isDep!=null) {
        out.println("/** @deprecated "+isDep+" **/");
      }

      String ext = p.get(context,"extends");
      if (ext == null) ext = "AbstractMeasure";
      boolean isFinal = !("false".equals(p.get(context,"final")));
      out.println("public "+
                  (isFinal?"final ":"")+
                  "class "+className+" extends "+ext+" implements Externalizable {");
      // get units
      Vector units = getUnitV(context);
      String base = getBaseUnit(context);
      String baseabbrev = p.get(context, "baseabbrev");
      if (baseabbrev == null) baseabbrev=base;
      String baseC = toConstantName(base);
      
      // write static factors
      out.println("  // Conversion factor constants");
      Enumeration ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        if (! unit.equals(base)) {
          // f is the factor of (1 unit) = f * (1 baseunit);
          String fact1 = toConstantName(base+"_PER_"+unit);
          double fromf = computeFromFactor(context,unit);
          out.println("  public static final double "+fact1+" = "+fromf+";");
          String fact2 = toConstantName(unit+"_PER_"+base);
          double tof = computeToFactor(context,unit);
          out.println("  public static final double "+fact2+" = "+tof+";");
        }
      }
      out.println();

      // the storage
      out.println("  // the value is stored as "+base);
      out.println("  private double theValue;");
      out.println();

      out.println("  /** No-arg constructor is only for use by serialization **/");
      out.println("  public "+className+"() {}");
      out.println();

      // constructor
      out.println("  // private constructor");
      out.println("  private "+className+"(double v) {");
      out.println("    theValue = v;");
      out.println("  }");
      out.println();

      // public constructor
      out.println("  /** parameterized constructor **/");
      out.println("  public "+className+"(double v, int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      theValue = v*convFactor[unit];");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      out.println("  /** takes strings of the form \"Number unit\" **/");
      out.println("  public "+className+"(String s) {");
      out.println("    int i = indexOfType(s);");
      out.println("    if (i < 0) throw new UnknownUnitException();");
      out.println("    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();");
      out.println("    String u = s.substring(i).trim().toLowerCase();");
      out.print("    ");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toClassName(unit);
        String fexpr="";
        if (!unit.equals(base)) {
          fexpr = "*"+toConstantName(base+"_PER_"+unit);
        }
        out.println("if (u.equals(\""+unitName.toLowerCase()+"\")) ");
        out.println("      theValue=n"+fexpr+";");
        out.print("    else ");
      }
      out.println("\n      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      // Named type factory methods
      out.println("  // TypeNamed factory methods");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toClassName(unit);
        String fexpr="";
        if (!unit.equals(base)) {
          fexpr = "*"+toConstantName(base+"_PER_"+unit);
        }
        out.println("  public static final "+className+" new"+unitName+"(double v) {");
        out.println("    return new "+className+"(v"+fexpr+");");
        out.println("  }");
        out.println("  public static final "+className+" new"+unitName+"(String s) {");
        out.println("    return new "+className+"((Double.valueOf(s).doubleValue())"+fexpr+");");
        out.println("  }");
      }
      out.println();

      // common unit support - mostly a bogon
      out.println();
      out.println("  public int getCommonUnit() {");
      String cus = p.get(context, "common");
      if (cus == null) { 
        out.println("    return 0;");
      } else {        
        out.println("    return "+cus.toUpperCase()+";");
      }
      out.println("  }");
      out.println();
      out.println("  public int getMaxUnit() { return MAXUNIT; }");
      out.println();

      out.println("  // unit names for getUnitName");
      out.println("  private static final String unitNames[]={");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        out.print("    \""+unit+"\"");
        if (ue.hasMoreElements())
          out.println(",");
        else
          out.println();
      }
      out.println("  };");
      out.println();
      out.println("  public String getUnitName(int unit) {");
      out.println("    return unitNames[unit];");
      out.println("  }");
      out.println();

      // index typed factory methods
      out.println("  // Index Typed factory methods");
      out.println("  static final double convFactor[]={");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toClassName(unit);
        String fexpr="1.0";
        if (!unit.equals(base)) {
          fexpr = toConstantName(base+"_PER_"+unit);
        }
        out.print("    "+fexpr);
        if (ue.hasMoreElements())
          out.println(",");
        else
          out.println();
      }
      out.println("  };");
      out.println("  // indexes into factor array");
      ue = units.elements();
      int i = 0;
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toConstantName(unit);
        out.println("  public static final int "+unitName+" = "+i+";");
        i++;
      }
      out.println("  static final int MAXUNIT = "+(i-1)+";");
      out.println();
      out.println("  // Index Typed factory methods");
      out.println("  public static final "+className+" new"+className+"(double v, int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      return new "+className+"(v*convFactor[unit]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();
      out.println("  public static final "+className+" new"+className+"(String s, int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      return new "+className+"((Double.valueOf(s).doubleValue())*convFactor[unit]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      // abstractmeasure-level concretefactory
      out.println("  // Support for AbstractMeasure-level constructor");
      out.println("  public static final AbstractMeasure newMeasure(String s, int unit) {");
      out.println("    return new"+className+"(s, unit);");
      out.println("  }");
      out.println("  public static final AbstractMeasure newMeasure(double v, int unit) {");
      out.println("    return new"+className+"(v, unit);");
      out.println("  }");


      // getters
      out.println("  // Unit-based Reader methods");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toClassName(unit);
        String fexpr="";
        if (!unit.equals(base)) {
          fexpr = "*"+toConstantName(unit+"_PER_"+base);
        }
        out.println("  public double get"+unitName+"() {");
        out.println("    return (theValue"+fexpr+");");
        out.println("  }");
      }
      out.println();

      // unit-as-argument getter
      out.println("  public double getValue(int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      return (theValue/convFactor[unit]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      // equals et al
      out.println("  public boolean equals(Object o) {");
      out.println("    return ( o instanceof "+className+" &&");
      out.println("             theValue == (("+className+") o).theValue);");
      out.println("  }");
      out.println("  public String toString() {");
      out.println("    return Double.toString(theValue) + \""+
                  baseabbrev+"\";");
      out.println("  }");
      out.println("  public int hashCode() {");
      out.println("    return (new Double(theValue)).hashCode();");
      out.println("  }");

      out.println();
      out.println("  // serialization");
      out.println("  public void writeExternal(ObjectOutput out) throws IOException {\n"+
                  "    out.writeDouble(theValue);\n"+
                  "  }");
      out.println("  public void readExternal(ObjectInput in) throws IOException {\n"+
                  "    theValue = in.readDouble();\n"+
                  "  }");

      // that's all, folks
      out.println("}");

    }

    void writeClassDt(PrintWriter out, String context, String className) {
      Vector mV = explodeC(p.get(context,"derivative"), '/');
      final String dC = (String) mV.elementAt(0);
      final String dtC = (String) mV.elementAt(1);

      String dB = getBaseUnit(dC);
      String dtB = getBaseUnit(dtC);

      String baseabbrev = p.get(context, "baseabbrev");
      if (baseabbrev == null) {
        String ab1 = p.get(dC,"baseabbrev");
        if (ab1 == null) ab1 = dB;
        String ab2 = p.get(dtC,"baseabbrev");
        if (ab2 == null) ab2 = dtB;
        baseabbrev=ab1+"/"+ab2;
      }

      class UnitTuple {
        String num;
        String den;
        String sden;
        double factor;
        UnitTuple(String n, String d) {
          num = n;
          den = d;
          sden = unpluralize(den);
          double nF = computeToFactor(dC,n);
          double dF = computeToFactor(dtC, d);
          factor = nF/dF;
        }
      }
      
      ArrayList tuples = new ArrayList();
      Vector numV = getUnitV(dC);
      Vector denV = getUnitV(dtC);
      for (Enumeration nE = numV.elements(); nE.hasMoreElements();) {
        String nU = (String) nE.nextElement();
        for (Enumeration dE = denV.elements(); dE.hasMoreElements();) {
          String dU = (String) dE.nextElement();
          tuples.add(new UnitTuple(nU,dU));
        }
      }

      String isDep = p.get(context,"deprecated");
      if (isDep!=null) {
        out.println("/** @deprecated "+isDep+" **/");
      }

      // the class def
      String ext = p.get(context,"extends");
      if (ext == null) ext = "AbstractMeasure";
      out.println("public final class "+className+" extends "+ext);
      out.print("  implements Externalizable, Derivative");
      String denC = p.get(dtC,"denominator_class");
      if (denC != null) {
        out.print(", "+denC);
      }
      out.println(" {");
      
      // the storage
      out.println("  // the value is stored as "+dB+"/"+unpluralize(dtB));
      out.println("  private double theValue;");
      out.println();

      out.println("  /** No-arg constructor is only for use by serialization **/");
      out.println("  public "+className+"() {}");
      out.println();

      // constructor
      out.println("  // private constructor");
      out.println("  private "+className+"(double v) {");
      out.println("    theValue = v;");
      out.println("  }");
      out.println();

      // public constructor
      out.println("  /** @param unit One of the constant units of "+className+" **/");
      out.println("  public "+className+"(double v, int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      theValue = v/convFactor[unit];");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();
      out.println("  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      out.println("  public "+className+"(double v, int unit1, int unit2) {");
      out.println("    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      out.println("      theValue = v*"+dC+".convFactor[unit1]/"+dtC+".convFactor[unit2];");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      out.println("  /** @param num An instance of "+dC+" to use as numerator\n"+
                  "   *  @param den An instance of "+dtC+"to use as denominator\n"+
                  "   **/");
      out.println("  public "+className+"("+dC+" num, "+dtC+" den) {");
      out.println("    theValue = num.getValue(0)/den.getValue(0);");
      out.println("  }");
      out.println();

      out.println("  /** takes strings of the form \"Number unit\" **/");
      out.println("  public "+className+"(String s) {");
      out.println("    int i = indexOfType(s);");
      out.println("    if (i < 0) throw new UnknownUnitException();");
      out.println("    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();");
      out.println("    String u = s.substring(i).trim().toLowerCase();");
      out.print("    ");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple) ti.next();
        out.println("if (u.equals(\""+
                    toClassName(ut.num+"per"+ut.sden).toLowerCase()+
                    "\")) ");
        out.println("      theValue=n/"+ut.factor+";");
        out.print("    else ");
      }
      out.println("\n      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      // Named type factory methods
      out.println("  // TypeNamed factory methods");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple) ti.next();

        String unit = ut.num+"_per_"+ut.sden;
        String unitName = toClassName(unit);
        String fexpr="*"+(1.0/ut.factor)+"";

        out.println("  public static final "+className+" new"+unitName+"(double v) {");
        out.println("    return new "+className+"(v"+fexpr+");");
        out.println("  }");
        out.println("  public static final "+className+" new"+unitName+"(String s) {");
        out.println("    return new "+className+"((Double.valueOf(s).doubleValue())"+fexpr+");");
        out.println("  }");
      }
      out.println();

      // common unit support - mostly a bogon
      out.println();
      out.println("  public int getCommonUnit() {");
      String cus = p.get(context, "common");
      if (cus == null) { 
        out.println("    return 0;");
      } else {        
        Vector v = explodeC(cus,'/');
        String n=(String)v.elementAt(0);
        String d=(String)v.elementAt(1);
        int i = 0;
        for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
          UnitTuple ut = (UnitTuple)ti.next();
          if (n.equals(ut.num) && d.equals(ut.sden)) {
            out.println("    return "+i+";");
            break;
          }
          i++;
        }
        if (i==tuples.size()){
          System.err.println("Couldn't find a matching tuple for \""+cus+"\".");
          out.println("     return 0;");
        }
      }
      out.println("  }");
      out.println();

      out.println("  public int getMaxUnit() { return MAXUNIT; }");
      out.println();

      out.println("  // unit names for getUnitName");
      out.println("  private static final String unitNames[]={");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        String unit = ut.num+"/"+ut.sden;
        out.print("    \""+unit+"\"");
        if (ti.hasNext())
          out.println(",");
        else
          out.println();
      }
      out.println("  };");
      out.println();

      out.println("  /** @param unit One of the constant units of "+className+" **/");
      out.println("  public final String getUnitName(int unit) {");
      out.println("    return unitNames[unit];");
      out.println("  }");
      out.println();

      // index typed factory methods
      out.println("  // Index Typed factory methods");
      out.println("  static final double convFactor[]={");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        out.print("    "+ut.factor+"");
        if (ti.hasNext())
          out.println(",");
        else
          out.println();
      }
      out.println("  };");

      out.println("  // indexes into factor array");
      int i = 0;
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        String unit = ut.num+"_per_"+ut.sden;
        String unitName = toConstantName(unit);
        out.println("  public static final int "+unitName+" = "+i+";");
        i++;
      }

      out.println("  static final int MAXUNIT = "+(i-1)+";");
      out.println();
      out.println("  // Index Typed factory methods");
      out.println("  /** @param unit One of the constant units of "+className+" **/");
      out.println("  public static final "+className+" new"+className+"(double v, int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      return new "+className+"(v*convFactor[unit]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();
      out.println("  /** @param unit One of the constant units of "+className+" **/");
      out.println("  public static final "+className+" new"+className+"(String s, int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      return new "+className+"((Double.valueOf(s).doubleValue())*convFactor[unit]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      out.println("  // Index Typed factory methods");
      out.println("  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      out.println("  public static final "+className+" new"+className+"(double v, int unit1, int unit2) {");
      out.println("    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      out.println("      return new "+className+"(v*"+dC+".convFactor[unit1]/"+dtC+".convFactor[unit2]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      out.println("  /** @param num An instance of "+dC+" to use as numerator\n"+
                  "   *  @param den An instance of "+dtC+"to use as denominator\n"+
                  "   **/");
      out.println("  public static final "+className+" new"+className+"("+dC+" num, "+dtC+" den) {");
      out.println("    return new "+className+"(num.getValue(0)/den.getValue(0));");
      out.println("  }");
      out.println();


      out.println("  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      out.println("  public static final "+className+" new"+className+"(String s, int unit1, int unit2) {");
      out.println("    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      out.println("      return new "+className+"((Double.valueOf(s).doubleValue())*"+dC+".convFactor[unit1]/"+dtC+".convFactor[unit2]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      // abstractmeasure-level concretefactory
      out.println("  // Support for AbstractMeasure-level constructor");
      out.println("  public static final AbstractMeasure newMeasure(String s, int unit) {");
      out.println("    return new"+className+"(s, unit);");
      out.println("  }");
      out.println("  public static final AbstractMeasure newMeasure(double v, int unit) {");
      out.println("    return new"+className+"(v, unit);");
      out.println("  }");


      // getters
      out.println("  // Unit-based Reader methods");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        String unit = ut.num+"_per_"+ut.sden;
        String unitName = toClassName(unit);
        out.println("  public double get"+unitName+"() {");
        out.println("    return (theValue*"+ut.factor+");");
        out.println("  }");
      }
      out.println();

      // unit-as-argument getter
      out.println("  /** @param unit One of the constant units of "+className+" **/");
      out.println("  public double getValue(int unit) {");
      out.println("    if (unit >= 0 && unit <= MAXUNIT)");
      out.println("      return (theValue*convFactor[unit]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      out.println("  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      out.println("  public double getValue(int unit1, int unit2) {");
      out.println("    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      out.println("      return (theValue*"+dtC+".convFactor[unit2]/"+dC+".convFactor[unit1]);");
      out.println("    else");
      out.println("      throw new UnknownUnitException();");
      out.println("  }");
      out.println();

      // equals et al
      out.println("  public boolean equals(Object o) {");
      out.println("    return ( o instanceof "+className+" &&");
      out.println("             theValue == (("+className+") o).theValue);");
      out.println("  }");
      out.println("  public String toString() {");
      out.println("    return Double.toString(theValue) + \""+
                  baseabbrev+"\";");
      out.println("  }");
      out.println("  public int hashCode() {");
      out.println("    return (new Double(theValue)).hashCode();");
      out.println("  }");
      out.println();

      // derivative implementation
      out.println("  // Derivative");
      out.println("  public final Class getNumeratorClass() { return "+dC+".class; }");
      out.println("  public final Class getDenominatorClass() { return "+dtC+".class; }");
      out.println();

      out.println("  private final static "+dC+" can_num = new "+dC+"(0.0,0);");
      out.println("  public final Measure getCanonicalNumerator() { return can_num; }");
      out.println("  private final static "+dtC+" can_den = new "+dtC+"(0.0,0);");
      out.println("  public final Measure getCanonicalDenominator() { return can_den; }");      
      out.println("  public final Measure computeNumerator(Measure den) {\n"+
                  "    if (!(den instanceof "+dtC+")) throw new IllegalArgumentException();\n"+
                  "    return new "+dC+"(theValue*den.getValue(0),0);\n"+
                  "  }");
      out.println("  public final Measure computeDenominator(Measure num) {\n"+
                  "    if (!(num instanceof "+dC+")) throw new IllegalArgumentException();\n"+
                  "    return new "+dtC+"(num.getValue(0)/theValue,0);\n"+
                  "  }");
      out.println();

      out.println("  // serialization");
      out.println("  public void writeExternal(ObjectOutput out) throws IOException {\n"+
                  "    out.writeDouble(theValue);\n"+
                  "  }");
      out.println("  public void readExternal(ObjectInput in) throws IOException {\n"+
                  "    theValue = in.readDouble();\n"+
                  "  }");

      // that's all, folks
      out.println("}");

    }

    void writeClassDef(PrintWriter out, String context, String className) {
      String alias = p.get(context, "alias");
      if (alias != null) {
        writeClassDef(out, alias, className); // alias class
      } else if (p.get(context,"derivative")!=null) {
        writeClassDt(out, context, className); // derivative class
      } else {
        writeClass(out, context, className); // "regular" class
      }
    }

    void writeMeasure(String context) throws Exception {

      String className = toClassName(context);
      String outname = className.toString() + ".java";
      noteFile(outname);
      if (cleanp) {
        (new File(outname)).delete();
      } else {
        debug("Writing Measure \""+context+"\" to \""+outname+"\"");
        FileOutputStream fos = new FileOutputStream(new File(targetdir,outname));
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        PrintWriter out = new PrintWriter(osw);
      
        writePrelude(out, context, className, outname);
        writeClassDef(out, context, className);
        out.close();
      }

      // check to see if we need to write a denominator class
      String denC = p.get(context,"denominator_class");
      if (denC != null) {
        writeDenominatorClass(context, denC);
      }
    }

    void writeDenominatorClass(String context, String className) throws Exception {
      String outname = className + ".java";
      noteFile(outname);
      if (cleanp) {
        (new File(outname)).delete();
        return;
      }
      debug("Writing Derivative Measure Denominator Class \""+className+"\" for \""+
            context+"\" to \""+outname+"\"");
      FileOutputStream fos = new FileOutputStream(new File(targetdir,outname));
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      PrintWriter out = new PrintWriter(osw);
      
      writePrelude(out, context, className, outname);
      out.println("/** Implemented by Measures which represent derivatives with\n"+
                  " * respect to "+toClassName(context)+".\n"+
                  " *\n"+
                  " * Derivative.getDenominatorClass() will always\n"+
                  " * return "+toClassName(context)+"\n"+
                  " **/");
      out.println("public interface "+className+" extends Derivative {");
      out.println("}");
      out.close();
    }


    public void write() throws Exception {
      grokGlobal();
      Enumeration contexts = p.getContexts();
      while ( contexts.hasMoreElements()) {
        String cname = (String) contexts.nextElement();
        if (! cname.equals("global")) {
          try {
            writeMeasure(cname);
          } catch (Exception e) {
            System.err.println("Caught while processing context \""+cname+"\":");
            e.printStackTrace();
          }
        }
      }
    }
  }

  /** arguments to the writer **/
  private String arguments[];
  
  boolean isVerbose = false;
  boolean cleanp = false;

  File targetdir = null;

  public void debug(String s) {
    if (isVerbose)
      System.err.println(s);
  }

  void processFile(String filename) {
    InputStream stream = null;
    try {
      targetdir = new File(System.getProperty("user.dir"));
      if (filename.equals("-")) {
        debug("Reading from standard input.");
        stream = new java.io.DataInputStream(System.in);
      } else {
        debug("Reading \""+filename+"\".");
        stream = new FileInputStream(filename);

        int p;
        if ((p=filename.lastIndexOf(File.separatorChar))!=-1) {
          targetdir = new File(filename.substring(0,p));
        }
      }

      Parser p = new Parser(stream);
      p.parse();
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
    System.err.println("Usage: MeasureWriter [-v] [--] file [file ...]");
    System.exit(1);
  }

  public void start() {
    boolean ignoreDash = false;
    boolean did = false;
    for (int i=0; i<arguments.length; i++) {
      String arg = arguments[i];
      if (!ignoreDash && arg.startsWith("-")) { // parse flags
        if (arg.equals("--")) {
          ignoreDash = true;
        } else if (arg.equals("-v")) {
          isVerbose = (!isVerbose);
        } else if (arg.equals("-clean")) {
          cleanp = true;
        } else {
          usage("Unknown option \""+arg+"\"");
        }
      } else {                  // deal with files
        processFile(arg);
        did = true;
      }
    }
    if (!did) {
      processFile("measures.def");
    }
  }


  public MeasureWriter(String args[]) {
    arguments=args;
  }


  public static void main(String args[]) {
    MeasureWriter mw = new MeasureWriter(args);
    mw.start();
  }
}
