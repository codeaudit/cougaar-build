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
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

public class AssetWriter {
  public final static String DEFAULT_FILENAME = "assets.def";

  class ClassD {
    Vector imports = new Vector();
    String name;
    String doc;
    String base;
    Vector slotds = new Vector();
    public ClassD(String name) {
      this.name = name;
      doc = null;
      base = null;
    }
    void parseSlot(String t) {
      if (t == null && t.length() == 0)
        return;

      String v = t;
      // two arg case
      int j;
      if ((j = t.indexOf(' ')) != -1) {
        v = t.substring(j+1).trim();
        t = t.substring(0,j);
      } else if (!pgParser.hasContext(v)) {
        // Assuming that two arg case never applies to property groups
          System.err.println("Definition of PropertyGroup " + v + 
                             " has not been parsed.");
      }      

      SlotD sd = new SlotD(t, v);

      sd.setTimePhased(pgParser.get(v, PGParser.TIMEPHASED) != null);
      
      addSlotD(sd);
    }

    public void setSlots(String s) {
      int p = 0;
      int l = s.length();
      int i;
      while ((i = s.indexOf(',',p)) != -1) {
        parseSlot(s.substring(p,i).trim());
        p = i+1;
      }
      parseSlot(s.substring(p).trim());
    }
    public SlotD getSlotD(String slotdname) {
      for (Enumeration en = slotds.elements(); en.hasMoreElements(); ) {
        SlotD slotd = (SlotD) en.nextElement();
        if (slotdname.equals(slotd.getName())) 
          return slotd;
      }
      return null;
    }
    public void addSlotD(SlotD slotd) {
      slotds.addElement(slotd);
    }
    public void setDoc(String doc) {
      this.doc = doc;
    }
    public String getDoc() { return doc; }
    public void setBase(String base) {
      this.base = base;
    }
    public String getBase() { return base; }
    public Enumeration getSlotDs() {
      return slotds.elements();
    }
    public Vector getSlots() { return slotds; }
    public Vector getAllSlots() {
      if (base != null) {
        ClassD cd = session.findClassD(base);
        if (cd != null) {
          Vector v = cd.getAllSlots();
          v = new Vector(v);
          v.addAll(slotds);
          return v;
        }
      }
      return slotds;
    }

    public String getName() { return name; }
    public String toString() { return "ClassD "+name; }
    public Vector getImports() { return imports; }
    public void addImport(String imp) {
      imports.addElement(imp);
    }
  }

  class SlotD {
    String type;
    String name;
    String doc;
    String init;
    boolean trans = false;
    boolean exact = false;
    boolean timephased = false;
    public SlotD(String type, String name) {
      this.type = type;
      this.name = name;
      init = null;
      doc = null;
    }
    public void setDoc(String doc) {
      this.doc = doc;
    }
    public void setInit(String init) {
      this.init = init;
    }
    public void setTrans(String t) {
      this.trans = Boolean.valueOf(t).booleanValue();
    }
    public void setExact(String t) {
      exact = Boolean.valueOf(t).booleanValue();
    }
    public void setTimePhased(boolean tp) {
      timephased = tp;
    }
    public String getType() { return type; }
    public String getName() { return name; }
    public boolean hasInit() { return (init != null); }
    public String getInit() { 
      if (init != null) {
        return init;
      } else {
        if (timephased) {
          return "PropertyGroupFactory.new"+type+"Schedule()";
        } else {
          return "PropertyGroupFactory.new"+type+"()";
        }
      }
    }
    public String getDoc() { return doc; }
    public boolean getTrans() { return trans; }
    public boolean getExact() { return exact; }
    public boolean getTimePhased() {return timephased; }
    public String toString() { return "SlotD "+name; }
  }

  class Session {
    PrintWriter filelist = null;

    InputStream s;
    public Session(InputStream s) {
      this.s = s;
      try {
        filelist = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(targetdir,"assets.gen"))));
        noteFile("assets.gen");

      } catch (IOException ioe) { throw new RuntimeException(); }
    }

    public void noteFile(String s) {
      filelist.println(s);
    }

    public void done() {
      filelist.close();
    }
    public Vector classds = new Vector();

    public ClassD findClassD(String name) {
      for (Enumeration e = classds.elements(); e.hasMoreElements(); ) {
        ClassD cd = (ClassD) e.nextElement();
        if (cd.getName().equals(name)) return cd;
      }
      return null;
    }

    public void parse() {
      InputStreamReader isr = new InputStreamReader(s);
      BufferedReader br = new BufferedReader(isr);
      
      ClassD cd = null;

      String line;
      int ln = 0;
      try {
      for (line = br.readLine(); line != null ; line=br.readLine() ){
        int i, j;

        ln++;
        // ignore comments
        if ((i = line.indexOf(';')) >= 0) 
          line = line.substring(0,i);

        // zap extra whitespace
        line = line.trim();

        // ignore empty lines
        if (line.length() <= 0)
          continue;

        int l;
        while (line.charAt((l=line.length())-1) == '\\') {
          line = line.substring(0,l-1) +
            br.readLine().trim();
          ln++;
        }

        if (line.charAt(0) == '[') {
          // classd line

          //Assume default property defs file if not yet initialized
          if (pgParser == null) {
            initProperties(PGParser.DEFAULT_FILENAME);
          }
            
          j = line.indexOf(']');
          String name = line.substring(1, j).trim();
          String ext = null;

          if ((j = name.indexOf(' ')) > -1 ) {
            ext = name.substring(j+1).trim();
            name = name.substring(0,j).trim();
          }
          cd = new ClassD(name);
          if (ext != null) {
            cd.setBase(ext);
          }
          classds.addElement(cd);
        } else {
          // a param line
          j = line.indexOf('=');
          String before = line.substring(0, j).trim();
          String after = line.substring(j+1).trim();

          if (before.equals("package") && cd==null) {
            asset_package=after;
          } else if (before.equals("propertydefs")) {
            System.out.println("propertydefs " + after);
            initProperties(after);
          } else if (before.equals("extends")) {
            cd.setBase(after);
          } else if (before.equals("slots")) {
            cd.setSlots(after);
          } else if (before.equals("doc")) {
            cd.setDoc(after);
          } else if (before.equals("import")) {
            cd.addImport(after);
          } else {
            j = before.indexOf('.');
            if (j >= 0) {
              // slot property
              String slot = before.substring(0,j);
              String param = before.substring(j+1);
              
              SlotD slotd = cd.getSlotD(slot);
              if (slotd == null) {
                System.err.println("Unknown slot in "+slot+"."+param);
              } else if (param.equals("init")) {
                slotd.setInit(after);
              } else if (param.equals("transient")) {
                slotd.setTrans(after);
              } else if (param.equals("exact")) {
                slotd.setExact(after);
              } else {
                System.err.println("Unknown slot parameter in "+slot+"."+param);
              }
            } else {
              System.err.println("Bogus line '"+line+"'");
            }

          }
        }

      }
      } catch (Exception e) {
        System.err.println("Exception parsing " + s + " at line "+ln);
        // e.printStackTrace();
      }
    }


    public void writeFactory(String path) {
      debug("Writing AssetFactory to "+path);
      if (cleanp) {
        (new File(path)).delete();
        return;
      }
      try {
        noteFile(path);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(targetdir,path))));
        writeCR(out);
      
        out.println("package " + asset_package + ";");
        out.println("import org.cougaar.domain.planning.ldm.asset.EssentialAssetFactory;");
        out.println();
        out.println("public class AssetFactory extends EssentialAssetFactory {");
        out.println("  public static String[] assets = {");

        for (Enumeration cds = classds.elements(); cds.hasMoreElements(); ) {
          ClassD cd = (ClassD) cds.nextElement();
          String name = cd.getName();
          out.print("    \""+asset_package+"."+name+"\"");
          if (cds.hasMoreElements()) out.print(",");
          out.println();
        }


        out.println("  };");
        out.println("}");

        out.close();
      } catch (Exception e) {
        System.err.println("Caught Exception while writing "+path);
        e.printStackTrace();
      }
    }

    public void write() {
      for (Enumeration cds = classds.elements();
           cds.hasMoreElements(); ) {
        ClassD cd = (ClassD) cds.nextElement();

        // write the class
        String path = cd.getName() + ".java";
        debug("Writing "+cd+" to "+path);

        if (cleanp) {
          (new File(path)).delete();
          continue;
        }

        try {
          noteFile(path);
          PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(targetdir,path))));
          writeCR(out);

	  out.println("package " + asset_package + ";");
          out.println("import org.cougaar.domain.planning.ldm.asset.*;");
          out.println("import java.io.ObjectOutputStream;");
          out.println("import java.io.ObjectInputStream;");
          out.println("import java.io.IOException;");
          out.println("import java.util.Vector;");
          out.println("import java.beans.PropertyDescriptor;" );
          out.println("import java.beans.IndexedPropertyDescriptor;" );
          out.println("import java.beans.IntrospectionException;" );

          for (Enumeration imps = cd.getImports().elements();
               imps.hasMoreElements(); ) {
            out.println("import "+imps.nextElement()+";");
          }

          String doc = cd.getDoc();
          if (doc != null) {
            out.println("/** "+doc+" **/");
            out.println();
          }

          String name = cd.getName();
          out.print("public class "+name);

          String ext = cd.getBase();
          if (ext != null && ! ext.equals("")) {
            out.print(" extends "+ext);
          }
          out.println(" {");
          out.println();
          
          // constructor
          // is public so that anyone can construct assets.
          // may be a problem if we reintroduce internal links to cluster
          // state.
          out.println("  public "+name+"() {");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();

            //String stype = sd.getType();
            String init = sd.getInit();

            // for time phased, want snameSchedule
            if (!sd.getTimePhased()) {
              out.println("    my"+sname+" = null;"); 
            } else {
              out.println("    my"+sname+"Schedule = null;"); 
            }
          }
          
          out.println("  }");
          out.println();

          // Prototype constructor
          out.println("  public "+name+"("+name+" prototype) {");
          out.println("    super(prototype);");

          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();
            String stype = sd.getType();

            if (sd.getTimePhased()) {
              sname = sname+"Schedule";
            }

            if (! sd.getTrans()) {
              // values default to prototype
              out.println("    my"+sname+"=null;");
            } else {
              out.println("    my"+sname+"="+sd.getInit()+";  //non-property");
            }
          }
          out.println("  }");
          out.println();

          // clone - used by copy
          out.println("  /** For infrastructure only - use org.cougaar.domain.planning.ldm.Factory.copyInstance instead. **/");
          out.println("  public Object clone() throws CloneNotSupportedException {\n"+
                      "    "+name+" _thing = ("+name+") super.clone();");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();
            String stype = sd.getType();
            boolean stimephased = sd.getTimePhased();

            String vname = "my"+sname;
            String vv = vname;

            if (!stimephased) {
              if (!sd.getExact()) { // if it's a property, we'll need to lock it
                vv = vv+".lock()";
              }
              out.println("    if ("+vname+"!=null) _thing.set"+sname+"("+vv+");");
            } else {
              vname = vname+"Schedule";
              out.println("    if (_thing."+vname+"!=null) _thing."+vname+".lockPGs();");
            }
          }
          out.println("    return _thing;\n"+
                      "  }\n");

          // create the base instance for copies.
          out.println("  /** create an instance of the right class for copy operations **/\n"+
                      "  public Asset instanceForCopy() {\n"+
                      "    return new "+name+"();\n"+
                      "  }\n");


          // create the base instance for copies.
          out.println("  /** create an instance of this prototype **/\n"+
                      "  public Asset createInstance() {\n"+
                      "    return new "+name+"(this);\n"+
                      "  }\n");


          // property filler
          out.println("  protected void fillAllPropertyGroups(Vector v) {");
          out.println("    super.fillAllPropertyGroups(v);");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) { 
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();
            if (sd.getTimePhased()) {
              sname = sname+"Schedule";
            }

            String stype = sd.getType();
            if (!sd.hasInit()) {
              out.println("    { Object _tmp = get"+sname+"();\n"+
                          // LATE?
                          "    if (_tmp != null && !(_tmp instanceof Null_PG)) {\n"+
                          "      v.addElement(_tmp);\n"+
                          "    } }");
            }
          }
          out.println("  }");
          out.println();

          // slot hackery
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) { 
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();
            String stype = sd.getType();
            boolean stimephased = sd.getTimePhased();


            String sdoc = sd.getDoc();
            boolean exact = sd.getExact();

            String var = "my"+sname;
            if (stimephased) {
              var = var+"Schedule";
              out.println("  private transient PropertyGroupSchedule "+var+";");
            } else {
              out.println("  private transient "+stype+" "+var+";");
            }
            out.println();
            
            if (sdoc != null) {
              out.println("  /** "+sdoc+" **/");
            }

            String argType = "";
            String argName = "";
            String argStr = "";
            if (stimephased) {
              argType = "long";
              argName = "time";
              argStr = argType+" "+argName;
            }
              
            out.println("  public "+stype+" get"+sname+"("+argStr+") {");
            if (exact) {
              out.println("    if ("+var+" != null) return "+var+";");
              out.println("    if (myPrototype instanceof "+name+")\n"+
                          "      return (("+name+")myPrototype).get"+sname+"("+argName+");");
              out.println("    return null;");
            } else {
              out.println("    "+sname+" _tmp = ("+var+" != null) ?");
              if (stimephased) {
                out.println("      ("+sname+")"+var+".intersects(time) :");
                out.println("      ("+sname+")resolvePG("+sname+".class, time);");
              } else {
                out.println("      "+var+" : ("+sname+")resolvePG("+sname+".class);");
              }
              out.println("    return (_tmp == "+sname+".nullPG)?null:_tmp;");
            }
            out.println("  }");

            if (stimephased) {
              String fname = "get"+sname+"Schedule";

              out.println("  public PropertyGroupSchedule "+fname+"() {");

              out.println("    if ("+var+"==null) {\n"+
                          "      if (myPrototype != null) {");

              if (exact) {
                out.println("         // exact slots must delegate to same class proto");
                out.println("         if (myPrototype instanceof "+name+")");
                out.println("           return (("+name+")myPrototype)."+fname+"();");
                out.println("         else");
                out.println("           return null;");
              } else {
                out.println("         if (myPrototype instanceof "+name+") {\n"+
                            "           return (("+name+")myPrototype)."+fname+"();\n"+
                            "         } else {\n"+
                            "           return myPrototype.searchForPropertyGroupSchedule("+stype+".class);\n"+
                            "         }");
              }
              out.println("      } else {\n"+
                          "         "+var+" = "+sd.getInit()+";\n"+
                          "      }\n"+
                          "    }");
              out.println("    return ("+var+" instanceof Null_PG)?null:"+var+";");
              out.println("  }");
              out.println();
            }

            String arg = "arg_"+sname;

            if (exact) {
              // exact slots aren't PropertyGroups
              out.println("  public void set"+sname+"("+stype+" "+arg+") {");
              out.println("    my"+sname+"= "+arg+";");
              out.println("  }");
            } else {
              // non-exact slots have setters which just take PropertyGroup
              out.println("  public void set"+sname+"(PropertyGroup "+arg+") {");
              out.println("    if (!("+arg+" instanceof "+stype+"))\n"+
                          "      throw new IllegalArgumentException(\"set"+
                          sname+" requires a "+stype+" argument.\");");
              if (stimephased) {
                out.println("    if ("+var+" == null) {");
                out.println("      "+var+" = get"+sname+"Schedule();");
                out.println("    }");
                out.println();
                out.println("    "+var+".add("+arg+");");
              } else {
                out.println("    "+var+" = ("+stype+") "+arg+";");
              }
              out.println("  }");
              out.println();
            }
            
            if (stimephased) {
              arg = "arg_"+name+"Schedule";
              // non-exact slots have setters which just take PropertyGroup
              out.println("  public void set"+sname+"Schedule(PropertyGroupSchedule "+arg+") {");
              out.println("    if (!("+arg+".getPGClass() != "+stype+".class))\n"+
                          "      throw new IllegalArgumentException(\"set"+
                          sname+"Schedule requires a PropertyGroupSchedule of"+sname+"s.\");");
              out.println();
              out.println("    "+var+" = "+arg+";");
              out.println("  }");
              out.println();
            }
          }

          out.println("  // generic search methods");

          out.println("  public PropertyGroupSchedule searchForPropertyGroupSchedule(Class c) {");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();
            String init = sd.getInit();
            // for time phased, want snameSchedule
            if (!sd.getExact() && sd.getTimePhased()) {
              out.println("    if ("+sname+".class.equals(c)) return get"+sname+
                          "Schedule();");
            }
          }
          out.println("    return super.searchForPropertyGroupSchedule(c);");
          out.println("  }");
          out.println();


          out.println("  public PropertyGroup getLocalPG(Class c, long t) {");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            if (!sd.getExact()) {
              String sname = sd.getName();
              // for time phased, want snameSchedule
              String var = "my"+sname;
              out.println("    if ("+sname+".class.equals(c)) {");
              if (!sd.getTimePhased()) {
                out.println("      return ("+var+"=="+sname+".nullPG)?null:"+var+";");
              } else {
                var = var+"Schedule";
                out.println("      if ("+var+"==null) {\n"+
                            "        return null;\n"+
                            "      } else {\n"+
                            "        if (t == UNSPECIFIED_TIME) {\n"+
                            "          return ("+sname+")"+var+".getDefault();\n"+
                            "        } else {\n"+
                            "          return ("+sname+")"+var+".intersects(t);\n"+
                            "        }\n"+
                            "      }");
              }
              out.println("    }");
            }
          }
          out.println("    return super.getLocalPG(c,t);");
          out.println("  }");
          out.println();


          out.println("  public void setLocalPG(Class c, PropertyGroup pg) {");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            if (!sd.getExact()) {
              String sname = sd.getName();
              // for time phased, want snameSchedule
              String var = "my"+sname;
              out.println("    if ("+sname+".class.equals(c)) {");
              if (!sd.getTimePhased()) {
                out.println("      "+var+"=("+sname+")pg;");
              } else {
                var = var+"Schedule";
                out.println("      if ("+var+"==null) "+var+" = "+sd.getInit()+";\n"+
                            "      "+var+".add(pg);");
              }
              out.println("    } else");
            }
          }
          out.println("      super.setLocalPG(c,pg);");
          out.println("  }");
          out.println(); 

          out.println("  public PropertyGroup generateDefaultPG(Class c) {");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            if (!sd.getExact()) {
              String sname = sd.getName();
              // for time phased, want snameSchedule
              String var = "my"+sname;
              out.println("    if ("+sname+".class.equals(c)) {");
              if (!sd.getTimePhased()) {
                out.println("      return ("+var+"= new "+sname+"Impl());");
              } else {
                out.println("      return null;");
              }
              out.println("    } else");
            }
          }
          out.println("      return super.generateDefaultPG(c);");
          out.println("  }");
          out.println(); 


          out.println("  // dumb serialization methods");
          out.println();
          
          // writer
          out.println("  private void writeObject(ObjectOutputStream out) throws IOException {");
          out.println("    out.defaultWriteObject();");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();

            if (! sd.getTrans()) {
              String sname = sd.getName();
              String stype = sd.getType();
              
              String var = "my"+sname;
              if (sd.getTimePhased()) {
                var = var+"Schedule";
              }
              out.println("      if ("+var+" instanceof Null_PG || "+var+" instanceof Future_PG) {\n"+
                          "        out.writeObject(null);\n"+
                          "      } else {\n"+
                          "        out.writeObject("+var+");\n"+
                          "      }");
            }
          }
          out.println("  }");
          out.println();

          // reader
          out.println("  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {");
          out.println("    in.defaultReadObject();");
          for (Enumeration sls = cd.getSlotDs(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();
            String stype = sd.getType();

            String var = "my"+sname;
            if (sd.getTimePhased()) {
              var = var+"Schedule";
              stype = "PropertyGroupSchedule";
            }

            if (! sd.getTrans()) {
              out.println("      "+var+"=("+stype+")in.readObject();");
            } else {
              out.println("      "+var+"="+sd.getInit()+";");
            }
          }
          out.println("  }");

          out.println("  // beaninfo support");
          writeBeanInfoBody(out,cd);

          out.println("}");
          
          out.close();
        } catch (Exception e) {
          System.err.println("Caught Exception while writing "+path);
          e.printStackTrace();
        } 
      }
    }

    public void writeBeanInfoBody(PrintWriter out, ClassD cd) {
      String name = cd.getName();

      try {
        out.println("  private static PropertyDescriptor properties[];\n"+
                    "  static {");
        Vector v = cd.getSlots(); //cd.getAllSlots();
        int l = v.size();
        if (l == 0) {
          out.println("    properties = new PropertyDescriptor["+l+"];");
        } else {
          out.println("    try {");
          out.println("      properties = new PropertyDescriptor["+l+"];");

          int i = 0;
          for (Enumeration sls = v.elements(); sls.hasMoreElements(); ) {  
            SlotD sd = (SlotD) sls.nextElement();
            String sname = sd.getName();
            String stype = sd.getType();
            
            if (sd.getTimePhased()) {
              out.println("      properties["+i+"] = new PropertyDescriptor(\""+
                          sname+"Schedule\", PropertyGroupSchedule.class, "+
                          "\"get"+sname+"Schedule\", "+
                          "null);");
            } else {
              out.println("      properties["+i+"] = new PropertyDescriptor(\""+
                          sname+"\", "+
                          name+".class, "+
                          "\"get"+sname+"\", "+
                          "null);");

            }
            i++;
          }
          out.println("    } catch (IntrospectionException ie) {}");
        }
        out.println("  }");
        out.println();
        out.println("  public PropertyDescriptor[] getPropertyDescriptors() {\n"+
                    "    PropertyDescriptor[] pds = super.getPropertyDescriptors();\n"+
                    "    PropertyDescriptor[] ps = new PropertyDescriptor[pds.length+"+
                    l+"];\n"+
                    "    System.arraycopy(pds, 0, ps, 0, pds.length);\n"+
                    "    System.arraycopy(properties, 0, ps, pds.length, "+l+");\n"+
                    "    return ps;\n"+
                    "  }");
      } catch (Exception e) {
        System.err.println("Caught "+e);
      }
    }

    public void writeBeanInfo() {
      for (Enumeration cds = classds.elements();
           cds.hasMoreElements(); ) {
        ClassD cd = (ClassD) cds.nextElement();

        // write the class
        String name = cd.getName();
        String path = name + "BeanInfo.java";
        debug("Writing "+cd+" to "+path);

        if (cleanp) {
          (new File(path)).delete();
          continue;
        }

        String ext = cd.getBase();
        if (ext != null && ! ext.equals("")) {
          ext = " extends "+ext+"BeanInfo";
        } else {
          ext = " extends SimpleBeanInfo";
        }

        try {
          noteFile(path);
          PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(targetdir,path))));
          writeCR(out);

          out.println();
	  out.println("package " + asset_package + ";");
          out.println("import java.beans.*;");
          out.println();
          out.println("/** BeanInfo for "+name+" **/");
          out.println();
          
        
          for (Enumeration imps = cd.getImports().elements();
               imps.hasMoreElements(); ) {
            out.println("import "+imps.nextElement()+";");
          }


          out.println("public class "+name+"BeanInfo"+ext+" {");
          writeBeanInfoBody(out, cd);
          out.println("}");
          out.close();
        } catch (Exception e) {
          System.err.println("Caught "+e);
        }
      }
    }

  }


  public void debug(String s) {
    if (isVerbose)
      System.err.println(s);
  }

  public Session session = null;
  
  protected PGParser pgParser = null;

  File targetdir = null;

  protected void initProperties(String propertyFilenames) {
    pgParser = new PGParser(isVerbose);

    int i;
    int parsePos = 0;
    String filename;
    while (parsePos < propertyFilenames.length()) {
      if ((i = propertyFilenames.indexOf(',', parsePos)) != -1) {
        filename = propertyFilenames.substring(parsePos, i).trim();
        parsePos = ++i;
      } else {
        filename = propertyFilenames.substring(parsePos).trim();
        parsePos = propertyFilenames.length();
      }

      InputStream stream = null;

      try {
        if (filename.equals("-")) {
          debug("Reading properties from standard input.");
          stream = new java.io.DataInputStream(System.in);
        } else if (filename.indexOf('/') == -1) {
          debug("Reading \""+filename+"\".");
          File pf;
          int p;
          // must be a '/', NOT filesystem dependent!
          if ((p=filename.lastIndexOf('/'))!=-1) {
            pf = new File(filename);
          } else {
            pf = new File(targetdir, filename);
          }
          stream = new FileInputStream(pf);
        } else {
          debug("Using ClassLoader to read \""+filename+"\".");
          stream = getClass().getClassLoader().getResourceAsStream(filename);
        }
        if (stream != null) {
          pgParser.parse(stream);
          stream.close();
        } else {
          System.err.println("Could not find \""+filename+"\" with "+
                             ((filename.indexOf('/')==-1)?"file":"Resource")+
                             " IO");
        }
      } catch (Exception e) {
        System.err.println("targetdir="+targetdir);
        System.err.println("Unable to parse " + filename);
        System.err.println("Caught: "+e);
        e.printStackTrace();
      }
    }
  }
      
  protected void processFile(String assetFilename) {
    InputStream stream = null;

    try {
      targetdir = new File(System.getProperty("user.dir"));
      if (assetFilename.equals("-")) {
        debug("Reading from standard input.");
        stream = new java.io.DataInputStream(System.in);
      } else {
        debug("Reading \""+assetFilename+"\".");
        stream = new FileInputStream(assetFilename);

        int p;
        if ((p=assetFilename.lastIndexOf(File.separatorChar))!=-1) {
          targetdir = new File(assetFilename.substring(0,p));
        }

      }

      /*
      if (assetFilename.indexOf('/') == -1) {
      } else {
        debug("Using ClassLoader to read \""+assetFilename+"\".");
        stream = 
          getClass().getClassLoader().getResourceAsStream(assetFilename);
      }
      */
      

      Session p = new Session(stream);
      session = p;
      p.parse();
      p.write();
      //p.writeBeanInfo();
      p.writeFactory("AssetFactory.java");
      p.done();
      stream.close();

    } catch (Exception e) {
      System.err.println("Caught: "+e);
      e.printStackTrace();
    }
  }

  private boolean isVerbose = false;
  private boolean cleanp = false;

  private String arguments[];

  private String asset_package = "org.cougaar.domain.planning.ldm.asset";
  
  protected void usage(String s) {
    System.err.println(s);
    System.err.println("Usage: AssetWriter [-v] [--] file [file ...]\n"+
                       "-v  toggle verbose mode (default off)\n"
                       );
    System.exit(1);
  }

  public void start() {
    boolean ignoreDash = false;
    String assetsFile = null;

    for (int i=0; i<arguments.length; i++) {
      String arg = arguments[i];
      if (!ignoreDash && arg.startsWith("-")) { // parse flags
        if (arg.equals("--")) {
          ignoreDash = true;
        } else if (arg.equals("-v")) {
          isVerbose = (!isVerbose);
	} else if (arg.charAt(1) == 'P') {
	  // handle -Ppackage.name argument
	  asset_package = arg.substring(2);
	  if (isVerbose) {
	    System.err.println("Setting asset_package to "  + asset_package);
	  }
        } else if (arg.equals("-clean")) {
          cleanp = true;
        } else if (arg.equals("-assets")) {
          assetsFile = arguments[++i];
        } else {
          usage("Unknown option \""+arg+"\"");
        }
      } else {
        assetsFile = arg;
      }
    }

    if (assetsFile == null) {
      assetsFile = DEFAULT_FILENAME;
    }

    processFile(assetsFile);
  }
  
  public AssetWriter(String args[]) {
    arguments = args;
  }

  protected void writeCR(PrintWriter out) {
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

  public static void main(String argv[]) {
    AssetWriter aw = new AssetWriter(argv);
    aw.start();
  }

}
