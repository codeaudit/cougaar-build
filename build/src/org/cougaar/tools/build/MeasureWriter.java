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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class MeasureWriter extends WriterBase {
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

    String unpluralize(String s) {
      if (s.endsWith("s")) 
        return s.substring(0,s.length()-1);
      if (s.equals("feet"))
        return "foot";
      return s;
    }

    void writePrelude(PrintWriter out, String context, String className, String outname) {
      writeCR(out,deffilename);
      println(out,"/** Immutable implementation of "+className+".");
      println(out," **/");
      println(out);
      println(out);
      println(out,"package org.cougaar.planning.ldm.measure;");
      println(out,"import java.io.*;");
      println(out);
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
    String computeToFactor(String context, String unit) {
      String base = getBaseUnit(context);
      if (base.equals(unit)) return "1.0";
      String tobase = p.get(context, "to_"+unit);
      if (tobase != null) {
        return tobase;
      } else {
        String frombase = p.get(context, "from_"+unit);
        if (frombase == null) {
          // no info here!  Try what we extend:
          return computeToFactor(p.get(context,"extends"), unit);
        }
        return "(1.0/"+frombase+")";
      }
    }

    /** return the value factor required to convert a value from the requested unit
     * to the base unit of the context.
     **/
    String computeFromFactor(String context, String unit) {
      String base = getBaseUnit(context);
      if (base.equals(unit)) return "1.0";
      String frombase = p.get(context, "from_"+unit);
      if (frombase != null) {
        return frombase;
      } else {
        String tobase = p.get(context, "to_"+unit);
        if (tobase == null) {
          // no info here!  Try what we extend:
          return computeFromFactor(p.get(context,"extends"), unit);
        }
        return "(1.0/"+tobase+")";
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
        println(out,"/** @deprecated "+isDep+" **/");
      }

      String ext = p.get(context,"extends");
      if (ext == null) ext = "AbstractMeasure";
      boolean isFinal = !("false".equals(p.get(context,"final")));
      println(out,"public "+
                  (isFinal?"final ":"")+
                  "class "+className+" extends "+ext+" implements Externalizable {");
      // get units
      Vector units = getUnitV(context);
      String base = getBaseUnit(context);
      String baseabbrev = p.get(context, "baseabbrev");
      if (baseabbrev == null) baseabbrev=base;
      
      // write static factors
      println(out,"  // Conversion factor constants");
      Enumeration ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        if (! unit.equals(base)) {
          // f is the factor of (1 unit) = f * (1 baseunit);
          String fact1 = toConstantName(base+"_PER_"+unit);
          String fromf = computeFromFactor(context,unit);
          println(out,"  public static final double "+fact1+" = "+fromf+";");
          String fact2 = toConstantName(unit+"_PER_"+base);
          String tof = computeToFactor(context,unit);
          println(out,"  public static final double "+fact2+" = "+tof+";");
        }
      }
      println(out);

      // the storage
      println(out,"  // the value is stored as "+base);
      println(out,"  private double theValue;");
      println(out);

      println(out,"  /** No-arg constructor is only for use by serialization **/");
      println(out,"  public "+className+"() {}");
      println(out);

      // constructor
      println(out,"  // private constructor");
      println(out,"  private "+className+"(double v) {");
      println(out,"    theValue = v;");
      println(out,"  }");
      println(out);

      // public constructor
      println(out,"  /** parameterized constructor **/");
      println(out,"  public "+className+"(double v, int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      theValue = v*convFactor[unit];");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      println(out,"  /** takes strings of the form \"Number unit\" **/");
      println(out,"  public "+className+"(String s) {");
      println(out,"    int i = indexOfType(s);");
      println(out,"    if (i < 0) throw new UnknownUnitException();");
      println(out,"    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();");
      println(out,"    String u = s.substring(i).trim().toLowerCase();");
      print(out,"    ");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toClassName(unit);
        String fexpr="";
        if (!unit.equals(base)) {
          fexpr = "*"+toConstantName(base+"_PER_"+unit);
        }
        println(out,"if (u.equals(\""+unitName.toLowerCase()+"\")) ");
        println(out,"      theValue=n"+fexpr+";");
        print(out,"    else ");
      }
      println(out,"\n      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      // Named type factory methods
      println(out,"  // TypeNamed factory methods");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toClassName(unit);
        String fexpr="";
        if (!unit.equals(base)) {
          fexpr = "*"+toConstantName(base+"_PER_"+unit);
        }
        println(out,"  public static final "+className+" new"+unitName+"(double v) {");
        println(out,"    return new "+className+"(v"+fexpr+");");
        println(out,"  }");
        println(out,"  public static final "+className+" new"+unitName+"(String s) {");
        println(out,"    return new "+className+"((Double.valueOf(s).doubleValue())"+fexpr+");");
        println(out,"  }");
      }
      println(out);

      // common unit support - mostly a bogon
      println(out);
      println(out,"  public int getCommonUnit() {");
      String cus = p.get(context, "common");
      if (cus == null) { 
        println(out,"    return 0;");
      } else {        
        println(out,"    return "+cus.toUpperCase()+";");
      }
      println(out,"  }");
      println(out);
      println(out,"  public int getMaxUnit() { return MAXUNIT; }");
      println(out);

      println(out,"  // unit names for getUnitName");
      println(out,"  private static final String unitNames[]={");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        print(out,"    \""+unit+"\"");
        if (ue.hasMoreElements())
          println(out,",");
        else
          println(out);
      }
      println(out,"  };");
      println(out);
      println(out,"  public String getUnitName(int unit) {");
      println(out,"    return unitNames[unit];");
      println(out,"  }");
      println(out);

      // index typed factory methods
      println(out,"  // Index Typed factory methods");
      println(out,"  static final double convFactor[]={");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String fexpr="1.0";
        if (!unit.equals(base)) {
          fexpr = toConstantName(base+"_PER_"+unit);
        }
        print(out,"    "+fexpr);
        if (ue.hasMoreElements())
          println(out,",");
        else
          println(out);
      }
      println(out,"  };");
      println(out,"  // indexes into factor array");
      ue = units.elements();
      int i = 0;
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toConstantName(unit);
        println(out,"  public static final int "+unitName+" = "+i+";");
        i++;
      }
      println(out,"  public static final int MAXUNIT = "+(i-1)+";");
      println(out);
      println(out,"  // Index Typed factory methods");
      println(out,"  public static final "+className+" new"+className+"(double v, int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      return new "+className+"(v*convFactor[unit]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);
      println(out,"  public static final "+className+" new"+className+"(String s, int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      return new "+className+"((Double.valueOf(s).doubleValue())*convFactor[unit]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      // abstractmeasure-level concretefactory
      println(out,"  // Support for AbstractMeasure-level constructor");
      println(out,"  public static final AbstractMeasure newMeasure(String s, int unit) {");
      println(out,"    return new"+className+"(s, unit);");
      println(out,"  }");
      println(out,"  public static final AbstractMeasure newMeasure(double v, int unit) {");
      println(out,"    return new"+className+"(v, unit);");
      println(out,"  }");


      // getters
      println(out,"  // Unit-based Reader methods");
      ue = units.elements();
      while (ue.hasMoreElements()) {
        String unit = (String)ue.nextElement();
        String unitName = toClassName(unit);
        String fexpr="";
        if (!unit.equals(base)) {
          //fexpr = "*"+toConstantName(unit+"_PER_"+base);
          fexpr = "/"+toConstantName(base+"_PER_"+unit);
        }
        println(out,"  public double get"+unitName+"() {");
        println(out,"    return (theValue"+fexpr+");");
        println(out,"  }");
      }
      println(out);

      // unit-as-argument getter
      println(out,"  public double getValue(int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      return (theValue/convFactor[unit]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      // equals et al
      println(out,"  public boolean equals(Object o) {");
      println(out,"    return ( o instanceof "+className+" &&");
      println(out,"             theValue == (("+className+") o).theValue);");
      println(out,"  }");
      println(out,"  public String toString() {");
      println(out,"    return Double.toString(theValue) + \""+
                  baseabbrev+"\";");
      println(out,"  }");
      println(out,"  public int hashCode() {");
      println(out,"    return (new Double(theValue)).hashCode();");
      println(out,"  }");

      println(out);
      println(out,"  // serialization");
      println(out,"  public void writeExternal(ObjectOutput out) throws IOException {\n"+
                  "    out.writeDouble(theValue);\n"+
                  "  }");
      println(out,"  public void readExternal(ObjectInput in) throws IOException {\n"+
                  "    theValue = in.readDouble();\n"+
                  "  }");

      // that's all, folks
      println(out,"}");

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
        String factor;
        UnitTuple(String n, String d) {
          num = n;
          den = d;
          sden = unpluralize(den);
          String nF = computeToFactor(dC,n);
          String dF = computeToFactor(dtC, d);
          factor = "("+nF+"/"+dF+")";
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
        println(out,"/** @deprecated "+isDep+" **/");
      }

      // the class def
      String ext = p.get(context,"extends");
      if (ext == null) ext = "AbstractMeasure";
      println(out,"public final class "+className+" extends "+ext);
      print(out,"  implements Externalizable, Derivative");
      String denC = p.get(dtC,"denominator_class");
      if (denC != null) {
        print(out,", "+denC);
      }
      println(out," {");
      
      // the storage
      println(out,"  // the value is stored as "+dB+"/"+unpluralize(dtB));
      println(out,"  private double theValue;");
      println(out);

      println(out,"  /** No-arg constructor is only for use by serialization **/");
      println(out,"  public "+className+"() {}");
      println(out);

      // constructor
      println(out,"  // private constructor");
      println(out,"  private "+className+"(double v) {");
      println(out,"    theValue = v;");
      println(out,"  }");
      println(out);

      // public constructor
      println(out,"  /** @param unit One of the constant units of "+className+" **/");
      println(out,"  public "+className+"(double v, int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      theValue = v/convFactor[unit];");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);
      println(out,"  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      println(out,"  public "+className+"(double v, int unit1, int unit2) {");
      println(out,"    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      println(out,"      theValue = v*"+dC+".convFactor[unit1]/"+dtC+".convFactor[unit2];");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      println(out,"  /** @param num An instance of "+dC+" to use as numerator\n"+
                  "   *  @param den An instance of "+dtC+"to use as denominator\n"+
                  "   **/");
      println(out,"  public "+className+"("+dC+" num, "+dtC+" den) {");
      println(out,"    theValue = num.getValue(0)/den.getValue(0);");
      println(out,"  }");
      println(out);

      println(out,"  /** takes strings of the form \"Number unit\" **/");
      println(out,"  public "+className+"(String s) {");
      println(out,"    int i = indexOfType(s);");
      println(out,"    if (i < 0) throw new UnknownUnitException();");
      println(out,"    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();");
      println(out,"    String u = s.substring(i).trim().toLowerCase();");
      print(out,"    ");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple) ti.next();
        println(out,"if (u.equals(\""+
                    toClassName(ut.num+"per"+ut.sden).toLowerCase()+
                    "\")) ");
        println(out,"      theValue=n/"+ut.factor+";");
        print(out,"    else ");
      }
      println(out,"\n      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      // Named type factory methods
      println(out,"  // TypeNamed factory methods");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple) ti.next();

        String unit = ut.num+"_per_"+ut.sden;
        String unitName = toClassName(unit);
        String fexpr="*(1.0/"+ut.factor+")";

        println(out,"  public static final "+className+" new"+unitName+"(double v) {");
        println(out,"    return new "+className+"(v"+fexpr+");");
        println(out,"  }");
        println(out,"  public static final "+className+" new"+unitName+"(String s) {");
        println(out,"    return new "+className+"((Double.valueOf(s).doubleValue())"+fexpr+");");
        println(out,"  }");
      }
      println(out);

      // common unit support - mostly a bogon
      println(out);
      println(out,"  public int getCommonUnit() {");
      String cus = p.get(context, "common");
      if (cus == null) { 
        println(out,"    return 0;");
      } else {        
        Vector v = explodeC(cus,'/');
        String n=(String)v.elementAt(0);
        String d=(String)v.elementAt(1);
        int i = 0;
        for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
          UnitTuple ut = (UnitTuple)ti.next();
          if (n.equals(ut.num) && d.equals(ut.sden)) {
            println(out,"    return "+i+";");
            break;
          }
          i++;
        }
        if (i==tuples.size()){
          System.err.println("Couldn't find a matching tuple for \""+cus+"\".");
          println(out,"     return 0;");
        }
      }
      println(out,"  }");
      println(out);

      println(out,"  public int getMaxUnit() { return MAXUNIT; }");
      println(out);

      println(out,"  // unit names for getUnitName");
      println(out,"  private static final String unitNames[]={");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        String unit = ut.num+"/"+ut.sden;
        print(out,"    \""+unit+"\"");
        if (ti.hasNext())
          println(out,",");
        else
          println(out);
      }
      println(out,"  };");
      println(out);

      println(out,"  /** @param unit One of the constant units of "+className+" **/");
      println(out,"  public final String getUnitName(int unit) {");
      println(out,"    return unitNames[unit];");
      println(out,"  }");
      println(out);

      // index typed factory methods
      println(out,"  // Index Typed factory methods");
      println(out,"  static final double convFactor[]={");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        print(out,"    "+ut.factor+"");
        if (ti.hasNext())
          println(out,",");
        else
          println(out);
      }
      println(out,"  };");

      println(out,"  // indexes into factor array");
      int i = 0;
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        String unit = ut.num+"_per_"+ut.sden;
        String unitName = toConstantName(unit);
        println(out,"  public static final int "+unitName+" = "+i+";");
        i++;
      }

      println(out,"  static final int MAXUNIT = "+(i-1)+";");
      println(out);
      println(out,"  // Index Typed factory methods");
      println(out,"  /** @param unit One of the constant units of "+className+" **/");
      println(out,"  public static final "+className+" new"+className+"(double v, int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      return new "+className+"(v*convFactor[unit]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);
      println(out,"  /** @param unit One of the constant units of "+className+" **/");
      println(out,"  public static final "+className+" new"+className+"(String s, int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      return new "+className+"((Double.valueOf(s).doubleValue())*convFactor[unit]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      println(out,"  // Index Typed factory methods");
      println(out,"  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      println(out,"  public static final "+className+" new"+className+"(double v, int unit1, int unit2) {");
      println(out,"    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      println(out,"      return new "+className+"(v*"+dC+".convFactor[unit1]/"+dtC+".convFactor[unit2]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      println(out,"  /** @param num An instance of "+dC+" to use as numerator\n"+
                  "   *  @param den An instance of "+dtC+"to use as denominator\n"+
                  "   **/");
      println(out,"  public static final "+className+" new"+className+"("+dC+" num, "+dtC+" den) {");
      println(out,"    return new "+className+"(num.getValue(0)/den.getValue(0));");
      println(out,"  }");
      println(out);


      println(out,"  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      println(out,"  public static final "+className+" new"+className+"(String s, int unit1, int unit2) {");
      println(out,"    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      println(out,"      return new "+className+"((Double.valueOf(s).doubleValue())*"+dC+".convFactor[unit1]/"+dtC+".convFactor[unit2]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      // abstractmeasure-level concretefactory
      println(out,"  // Support for AbstractMeasure-level constructor");
      println(out,"  public static final AbstractMeasure newMeasure(String s, int unit) {");
      println(out,"    return new"+className+"(s, unit);");
      println(out,"  }");
      println(out,"  public static final AbstractMeasure newMeasure(double v, int unit) {");
      println(out,"    return new"+className+"(v, unit);");
      println(out,"  }");


      // getters
      println(out,"  // Unit-based Reader methods");
      for (Iterator ti = tuples.iterator(); ti.hasNext(); ) {
        UnitTuple ut = (UnitTuple)ti.next();
        String unit = ut.num+"_per_"+ut.sden;
        String unitName = toClassName(unit);
        println(out,"  public double get"+unitName+"() {");
        println(out,"    return (theValue*"+ut.factor+");");
        println(out,"  }");
      }
      println(out);

      // unit-as-argument getter
      println(out,"  /** @param unit One of the constant units of "+className+" **/");
      println(out,"  public double getValue(int unit) {");
      println(out,"    if (unit >= 0 && unit <= MAXUNIT)");
      println(out,"      return (theValue*convFactor[unit]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      println(out,"  /** @param unit1 One of the constant units of "+dC+"\n"+
                  "   *  @param unit2 One of the constant units of "+dtC+"\n"+
                  "   **/");
      println(out,"  public double getValue(int unit1, int unit2) {");
      println(out,"    if (unit1 >= 0 && unit1 <= "+dC+".MAXUNIT &&\n"+
                  "        unit2 >= 0 && unit2 <= "+dtC+".MAXUNIT)");
      println(out,"      return (theValue*"+dtC+".convFactor[unit2]/"+dC+".convFactor[unit1]);");
      println(out,"    else");
      println(out,"      throw new UnknownUnitException();");
      println(out,"  }");
      println(out);

      // equals et al
      println(out,"  public boolean equals(Object o) {");
      println(out,"    return ( o instanceof "+className+" &&");
      println(out,"             theValue == (("+className+") o).theValue);");
      println(out,"  }");
      println(out,"  public String toString() {");
      println(out,"    return Double.toString(theValue) + \""+
                  baseabbrev+"\";");
      println(out,"  }");
      println(out,"  public int hashCode() {");
      println(out,"    return (new Double(theValue)).hashCode();");
      println(out,"  }");
      println(out);

      // derivative implementation
      println(out,"  // Derivative");
      println(out,"  public final Class getNumeratorClass() { return "+dC+".class; }");
      println(out,"  public final Class getDenominatorClass() { return "+dtC+".class; }");
      println(out);

      println(out,"  private final static "+dC+" can_num = new "+dC+"(0.0,0);");
      println(out,"  public final Measure getCanonicalNumerator() { return can_num; }");
      println(out,"  private final static "+dtC+" can_den = new "+dtC+"(0.0,0);");
      println(out,"  public final Measure getCanonicalDenominator() { return can_den; }");      
      println(out,"  public final Measure computeNumerator(Measure den) {\n"+
                  "    if (!(den instanceof "+dtC+")) throw new IllegalArgumentException();\n"+
                  "    return new "+dC+"(theValue*den.getValue(0),0);\n"+
                  "  }");
      println(out,"  public final Measure computeDenominator(Measure num) {\n"+
                  "    if (!(num instanceof "+dC+")) throw new IllegalArgumentException();\n"+
                  "    return new "+dtC+"(num.getValue(0)/theValue,0);\n"+
                  "  }");
      println(out);

      println(out,"  // serialization");
      println(out,"  public void writeExternal(ObjectOutput out) throws IOException {\n"+
                  "    out.writeDouble(theValue);\n"+
                  "  }");
      println(out,"  public void readExternal(ObjectInput in) throws IOException {\n"+
                  "    theValue = in.readDouble();\n"+
                  "  }");

      // that's all, folks
      println(out,"}");

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
        FileOutputStream fos = new FileOutputStream(new File(getTargetDir(),outname));
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
      FileOutputStream fos = new FileOutputStream(new File(getTargetDir(),outname));
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      PrintWriter out = new PrintWriter(osw);
      
      writePrelude(out, context, className, outname);
      println(out,"/** Implemented by Measures which represent derivatives with\n"+
                  " * respect to "+toClassName(context)+".\n"+
                  " *\n"+
                  " * Derivative.getDenominatorClass() will always\n"+
                  " * return "+toClassName(context)+"\n"+
                  " **/");
      println(out,"public interface "+className+" extends Derivative {");
      println(out,"}");
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
      } else {
        debug("Reading \""+filename+"\".");
        deffilename = filename;
        stream = new FileInputStream(filename);
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
        } else if (arg.equals("-d")) {
          targetDirName = arguments[++i];
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

  public String deffilename = null;

  public MeasureWriter(String args[]) {
    arguments=args;
  }

  public static void main(String args[]) {
    MeasureWriter mw = new MeasureWriter(args);
    mw.start();
  }
}
