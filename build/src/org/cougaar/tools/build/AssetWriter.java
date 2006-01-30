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
import java.util.Collection;
import java.util.List;

public class AssetWriter extends WriterBase {
  public final static String DEFAULT_FILENAME = "assets.def";

  class ClassD {
    Collection<String> imports = new ArrayList<String>();
    String name;
    String doc;
    String base;
    boolean hasRelationships = false;
    List<SlotD> slotds = new ArrayList<SlotD>();
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

      sd.setTimePhased(pgParser.getValueFlag(v, PGParser.TIMEPHASED, true, false));


      if (pgParser.getValueFlag(v, PGParser.HAS_RELATIONSHIPS, true, false)) {
        if (!hasRelationships) {
          hasRelationships = true;
          sd.setHasRelationships(true);
        } else {
          System.err.println("AssetWriter.parseSlot(): " + name + 
                             " has multiple PGs which implement HasRelationships");
          sd.setHasRelationships(false);
        }
      } else {
        sd.setHasRelationships(false);
      }      
      addSlotD(sd);

    }

    public void setSlots(String s) {
      int p = 0;

      int i;
      while ((i = s.indexOf(',',p)) != -1) {
        parseSlot(s.substring(p,i).trim());
        p = i+1;
      }
      parseSlot(s.substring(p).trim());
    }
    public SlotD getSlotD(String slotdname) {
    	for (SlotD slotd: slotds) {
        if (slotdname.equals(slotd.getName())) 
          return slotd;
      }
      return null;
    }
    public void addSlotD(SlotD slotd) {
      slotds.add(slotd);
    }
    public void setDoc(String doc) {
      this.doc = doc;
    }
    public String getDoc() { return doc; }
    public void setBase(String base) {
      this.base = base;
    }
    public String getBase() { return base; }
    public List<SlotD> getSlotDs() {
      return slotds;
    }
 
    public List<SlotD> getAllSlots() {
      if (base != null) {
        ClassD cd = session.findClassD(base);
        if (cd != null) {
          List<SlotD> v = cd.getAllSlots();
          v = new ArrayList<SlotD>(v);
          v.addAll(slotds);
          return v;
        }
      }
      return slotds;
    }

    public String getName() { return name; }
    public String toString() { return "ClassD "+name; }
    public Collection<String> getImports() { return imports; }
    public void addImport(String imp) {
      imports.add(imp);
    }
    public boolean getHasRelationships() { return hasRelationships; }
  }

  class SlotD {
    String type;
    String name;
    String doc;
    String init;
    boolean trans = false;
    boolean exact = false;
    boolean timephased = false;
    boolean hasRelationships = false;

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
    public void setHasRelationships(boolean hr) {
      hasRelationships = hr;
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
    public boolean getHasRelationships() {return hasRelationships; }
    public String toString() { return "SlotD "+name; }
  }

  class Session {
    PrintWriter filelist = null;

    InputStream s;
    public Session(InputStream s) {
      this.s = s;
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
    
    public Collection<ClassD> classds = new ArrayList<ClassD>();

    public ClassD findClassD(String name) {
      for (ClassD cd : classds) {
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
          classds.add(cd);
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
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getTargetDir(),path))));
        writeCR(out,deffilename);
      
        println(out,"package " + asset_package + ";");
        println(out,"import org.cougaar.planning.ldm.asset.EssentialAssetFactory;");
        println(out);
        println(out,"public class AssetFactory extends EssentialAssetFactory {");
        println(out,"  public static String[] assets = {");

        for (ClassD cd : classds) {
          String name = cd.getName();
          print(out,"    \""+asset_package+"."+name+"\",");
          println(out);
        }

        println(out,"  };");
        println(out,"}");

        out.close();
      } catch (Exception e) {
        System.err.println("Caught Exception while writing "+path);
        e.printStackTrace();
      }
    }

    public void write() {
      for (ClassD cd : classds) {
        // write the class
        String path = cd.getName() + ".java";
        debug("Writing "+cd+" to "+path);

        if (cleanp) {
          (new File(path)).delete();
          continue;
        }

        try {
          noteFile(path);
          PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getTargetDir(),path))));
          writeCR(out,deffilename);

	  println(out,"package " + asset_package + ";");
          println(out,"import org.cougaar.planning.ldm.asset.*;");
          println(out,"import java.io.ObjectOutputStream;");
          println(out,"import java.io.ObjectInputStream;");
          println(out,"import java.io.IOException;");
          println(out,"import java.util.Vector;");
          println(out,"import java.beans.PropertyDescriptor;" );
          println(out,"import java.beans.IndexedPropertyDescriptor;" );
          println(out,"import java.beans.IntrospectionException;" );

          for (String imp: cd.getImports()) {
            println(out,"import "+imp+";");
          }

          if (cd.getHasRelationships()) {
            println(out,"import org.cougaar.planning.ldm.plan.HasRelationships;");
            println(out,"import org.cougaar.planning.ldm.plan.RelationshipSchedule;");
            println(out,"import org.cougaar.planning.ldm.plan.RelationshipScheduleImpl;");
          }

          String doc = cd.getDoc();
          if (doc != null) {
            println(out,"/** "+doc+" **/");
            println(out);
          }

          String name = cd.getName();
          print(out,"public class "+name);

          String ext = cd.getBase();
          if (ext != null && ! ext.equals("")) {
            print(out," extends "+ext);
          }
          if (cd.getHasRelationships()) {
            print(out, " implements HasRelationships");
          }
          println(out," {");
          println(out);
          
          // constructor
          // is public so that anyone can construct assets.
          // may be a problem if we reintroduce internal links to cluster
          // state.
          println(out,"  public "+name+"() {");
          for (SlotD sd: cd.getSlotDs()) {
            String sname = sd.getName();

            // for time phased, want snameSchedule
            if (!sd.getTimePhased()) {
              println(out,"    my"+sname+" = null;"); 
            } else {
              println(out,"    my"+sname+"Schedule = null;"); 
            }
          }
          
          println(out,"  }");
          println(out);

          // Prototype constructor
          println(out,"  public "+name+"("+name+" prototype) {");
          println(out,"    super(prototype);");

          for (SlotD sd: cd.getSlotDs()) {
            String sname = sd.getName();

            if (sd.getTimePhased()) {
              sname = sname+"Schedule";
            }

            if (! sd.getTrans()) {
              // values default to prototype
              println(out,"    my"+sname+"=null;");
            } else {
              println(out,"    my"+sname+"="+sd.getInit()+";  //non-property");
            }
          }
          println(out,"  }");
          println(out);

          // clone - used by copy
          println(out,"  /** For infrastructure only - use org.cougaar.core.domain.Factory.copyInstance instead. **/");
          println(out,"  public Object clone() throws CloneNotSupportedException {\n"+
                      "    "+name+" _thing = ("+name+") super.clone();");
          for (SlotD sd: cd.getSlotDs()) {
            // Don't clone RelationshipSchedules
            // Hack until we figure out how to do this right
            if (!sd.getHasRelationships()) {
              String sname = sd.getName();
              boolean stimephased = sd.getTimePhased();
              
              String vname = "my"+sname;
              String vv = vname;
              
              if (!stimephased) {
                if (!sd.getExact()) { // if it's a property, we'll need to lock it
                  vv = vv+".lock()";
                }
                println(out,"    if ("+vname+"!=null) _thing.set"+sname+"("+vv+");");

              } else {
                vname = vname+"Schedule";
                println(out,"    if ("+vname+"!=null) _thing.set"+sname+"Schedule((PropertyGroupSchedule) "+vname+".lock());");
              }
            }
          }
          println(out,"    return _thing;\n"+
                      "  }\n");

          // create the base instance for copies.
          println(out,"  /** create an instance of the right class for copy operations **/\n"+
                      "  public Asset instanceForCopy() {\n"+
                      "    return new "+name+"();\n"+
                      "  }\n");


          // create the base instance for copies.
          println(out,"  /** create an instance of this prototype **/\n"+
                      "  public Asset createInstance() {\n"+
                      "    return new "+name+"(this);\n"+
                      "  }\n");


          // property filler
          println(out,"  protected void fillAllPropertyGroups(Vector v) {");
          println(out,"    super.fillAllPropertyGroups(v);");
          for (SlotD sd: cd.getSlotDs()) {
            String sname = sd.getName();
            if (sd.getTimePhased()) {
              sname = sname+"Schedule";
            }

            if (!sd.hasInit()) {
              println(out,"    { Object _tmp = get"+sname+"();\n"+
                          // LATE?
                          "    if (_tmp != null && !(_tmp instanceof Null_PG)) {\n"+
                          "      v.addElement(_tmp);\n"+
                          "    } }");
            }
          }
          println(out,"  }");
          println(out);

          // slot hackery
          for (SlotD sd: cd.getSlotDs()) {
            String sname = sd.getName();
            String stype = sd.getType();
            boolean stimephased = sd.getTimePhased();


            String sdoc = sd.getDoc();
            boolean exact = sd.getExact();

            String var = "my"+sname;
            if (stimephased) {
              var = var+"Schedule";
              println(out,"  private transient PropertyGroupSchedule "+var+";");
            } else {
              println(out,"  private transient "+stype+" "+var+";");
            }
            println(out);
            
            if (sdoc != null) {
              println(out,"  /** "+sdoc+" **/");
            }

            String argType = "";
            String argName = "";
            String argStr = "";
            if (stimephased) {
              argType = "long";
              argName = "time";
              argStr = argType+" "+argName;
            }

            if (sd.getHasRelationships()) {
              addHasRelationshipsImpl(out, sname);
            }
              
            println(out,"  public "+stype+" get"+sname+"("+argStr+") {");
            if (exact) {
              println(out,"    if ("+var+" != null) return "+var+";");
              println(out,"    if (myPrototype instanceof "+name+")\n"+
                          "      return (("+name+")myPrototype).get"+sname+"("+argName+");");
              println(out,"    return null;");
            } else {
              println(out,"    "+sname+" _tmp = ("+var+" != null) ?");
              if (stimephased) {
                println(out,"      ("+sname+")"+var+".intersects(time) :");
                println(out,"      ("+sname+")resolvePG("+sname+".class, time);");
              } else {
                println(out,"      "+var+" : ("+sname+")resolvePG("+sname+".class);");
              }

              println(out,"    return (_tmp == "+sname+".nullPG)?null:_tmp;");
            }
            println(out,"  }");


            if (stimephased) {
              String fname = "get"+sname+"Schedule";

              println(out,"  public PropertyGroupSchedule "+fname+"() {");
              if (exact) {
                println(out,"    if ("+var+" != null) return "+var+";");

                println(out,"    // exact slots must delegate to same class proto");
                println(out,"    if (myPrototype instanceof "+name+")");
                println(out,"      return (("+name+")myPrototype)."+fname+"();");
                println(out,"    return null;");
              } else {
                println(out,"    PropertyGroupSchedule _tmp = ("+var+" != null) ?");
                println(out,"         "+var+" : "+"resolvePGSchedule("+stype+".class);");
                println(out,"    return _tmp;");
                println(out,"  }");
                println(out);
              }
            }

            String arg = "arg_"+sname;

            // ADD methods
            if (sd.getHasRelationships()) {
            }


            if (exact) {
              // exact slots aren't PropertyGroups
              println(out,"  public void set"+sname+"("+stype+" "+arg+") {");
              println(out,"    my"+sname+"= "+arg+";");
              println(out,"  }");
            } else {
              // non-exact slots have setters which just take PropertyGroup
              println(out,"  public void set"+sname+"(PropertyGroup "+arg+") {");
              println(out,"    if (!("+arg+" instanceof "+stype+"))\n"+
                          "      throw new IllegalArgumentException(\"set"+
                          sname+" requires a "+stype+" argument.\");");
              if (stimephased) {
                println(out,"    if ("+var+" == null) {");
                println(out,"      "+var+" = "+sd.getInit()+";");
                println(out,"    }");
                println(out);
                println(out,"    "+var+".add("+arg+");");
              } else {
                println(out,"    "+var+" = ("+stype+") "+arg+";");
              }
              println(out,"  }");
              println(out);
            }
            
            if (stimephased) {
              arg = "arg_"+name+"Schedule";
              // non-exact slots have setters which just take PropertyGroup
              println(out,"  public void set"+sname+"Schedule(PropertyGroupSchedule "+arg+") {");
              println(out,"    if (!("+stype+".class.equals("+arg+".getPGClass())))\n"+
                          "      throw new IllegalArgumentException(\"set"+
                          sname+"Schedule requires a PropertyGroupSchedule of"+sname+"s.\");");
              println(out);
              println(out,"    "+var+" = "+arg+";");
              println(out,"  }");
              println(out);
            }
          }

          if (!cd.getSlotDs().isEmpty()) {  // don't write the methods unless we need to
            println(out,"  // generic search methods");

            println(out,"  public PropertyGroup getLocalPG(Class c, long t) {");
            for (SlotD sd: cd.getSlotDs()) {
              if (!sd.getExact()) {
                String sname = sd.getName();
                // for time phased, want snameSchedule
                String var = "my"+sname;
                println(out,"    if ("+sname+".class.equals(c)) {");
                if (!sd.getTimePhased()) {
                  println(out,"      return ("+var+"=="+sname+".nullPG)?null:"+var+";");
                } else {
                  var = var+"Schedule";
                  println(out,"      if ("+var+"==null) {\n"+
                          "        return null;\n"+
                          "      } else {\n"+
                          "        if (t == UNSPECIFIED_TIME) {\n"+
                          "          return ("+sname+")"+var+".getDefault();\n"+
                          "        } else {\n"+
                          "          return ("+sname+")"+var+".intersects(t);\n"+
                          "        }\n"+
                          "      }");
                }
                println(out,"    }");
              }
            }
            println(out,"    return super.getLocalPG(c,t);");
            println(out,"  }");
            println(out);

            println(out,"  public PropertyGroupSchedule getLocalPGSchedule(Class c) {");
            for (SlotD sd: cd.getSlotDs()) {
              String sname = sd.getName();
              // for time phased, want snameSchedule
              if (!sd.getExact() && sd.getTimePhased()) {
                println(out,"    if ("+sname+".class.equals(c)) {");
                println(out,"      return my"+sname+"Schedule;");
                println(out,"    }");
              }
            }
            println(out,"    return super.getLocalPGSchedule(c);");
            println(out,"  }");
            println(out);


            println(out,"  public void setLocalPG(Class c, PropertyGroup pg) {");
            for (SlotD sd: cd.getSlotDs()) {
              if (!sd.getExact()) {
                String sname = sd.getName();
                String var = "my"+sname;
                println(out,"    if ("+sname+".class.equals(c)) {");
                if (!sd.getTimePhased()) {
                  println(out,"      "+var+"=("+sname+")pg;");
                } else {
                  var = var+"Schedule";
                  println(out,"      if ("+var+"==null) {");
                  println(out,"        "+var+"="+sd.getInit()+";");
                  println(out,"      } else {");
                  println(out,"        "+var+".removeAll("+var+".intersectingSet((TimePhasedPropertyGroup) pg));");
                  println(out,"      }");
                  println(out,"      "+var+".add(pg);");
                }
                println(out,"    } else");
              }
            }
            println(out,"      super.setLocalPG(c,pg);");
            println(out,"  }");
            println(out); 

            println(out,"  public void setLocalPGSchedule(PropertyGroupSchedule pgSchedule) {");
            for (SlotD sd: cd.getSlotDs()) {
              if ((!sd.getExact()) && (sd.getTimePhased())) {
                String sname = sd.getName();
                // for time phased, want snameSchedule
                String var = "my"+sname+"Schedule";
                println(out,"    if ("+sname+".class.equals(pgSchedule.getPGClass())) {");
                println(out,"      "+var+"=pgSchedule;");
                println(out,"    } else");
              }
            }
            println(out,"      super.setLocalPGSchedule(pgSchedule);");
            println(out,"  }");
            println(out); 

            println(out,"  public PropertyGroup removeLocalPG(Class c) {");
            println(out,"    PropertyGroup removed = null;");
            print  (out,"    ");
            for (SlotD sd: cd.getSlotDs()) {
              if (!sd.getExact()) {
                String sname = sd.getName();
                // for time phased, want snameSchedule
                String var = "my"+sname;
                println(out,    "if ("+sname+".class.equals(c)) {");
                if (!sd.getTimePhased()) {
                  println(out,"      removed="+var+";");
                  println(out,"      "+var+"=null;");
                } else {
                  var = var+"Schedule";
                  println(out,"      if ("+var+"!=null) {");
                  println(out,"        if ("+var+".getDefault()!=null) {");
                  println(out,"          removed="+var+".getDefault();");
                  println(out,"        } else if ("+var+".size() > 0) {");
                  println(out,"          removed=(PropertyGroup) "+var+".get(0);");
                  println(out,"        }");
                  println(out,"        "+var+"=null;");
                  println(out,"      }");
                }
                print  (out,"    } else ");
              }
            }
            println(out,    "{");
            println(out,"      removed=super.removeLocalPG(c);");
            println(out,"    }");
            println(out,"    return removed;");
            println(out,"  }");
            println(out); 

            println(out,"  public PropertyGroup removeLocalPG(PropertyGroup pg) {");
            println(out,"    Class pgc = pg.getPrimaryClass();");
            print  (out,"    ");
            for (SlotD sd: cd.getSlotDs()) {
              if (!sd.getExact()) {
                String sname = sd.getName();
                // for time phased, want snameSchedule
                String var = "my"+sname;
                println(out,    "if ("+sname+".class.equals(pgc)) {");
                if (!sd.getTimePhased()) {
                  println(out,"      PropertyGroup removed="+var+";");
                  println(out,"      "+var+"=null;");
                  println(out,"      return removed;");
                } else {
                  var = var+"Schedule";
                  println(out,    "if (("+var+"!=null) && ");
                  println(out,"          ("+var+".remove(pg))) {");
                  println(out,"        return pg;");
                  println(out,"      }");
                }
                print  (out,"    } else ");
              }
            }
            println(out, "{}");
            println(out,"    return super.removeLocalPG(pg);");
            println(out,"  }");
            println(out); 

            println(out,"  public PropertyGroupSchedule removeLocalPGSchedule(Class c) {");
            for (SlotD sd: cd.getSlotDs()) {
              if ((!sd.getExact()) && (sd.getTimePhased())) {
                String sname = sd.getName();
                // for time phased, want snameSchedule
                String var = "my"+sname+"Schedule";
                println(out,"    if ("+sname+".class.equals(c)) {");
                println(out,"      PropertyGroupSchedule removed="+var+";");
                println(out,"      "+var+"=null;");
                println(out,"      return removed;");
                println(out,"    } else ");
              }
            }
            println(out, "   {");
            println(out,"      return super.removeLocalPGSchedule(c);");
            println(out,"    }");
            println(out,"  }");
            println(out); 

            println(out,"  public PropertyGroup generateDefaultPG(Class c) {");
            for (SlotD sd: cd.getSlotDs()) {
              if (!sd.getExact()) {
                String sname = sd.getName();
                // for time phased, want snameSchedule
                String var = "my"+sname;
                println(out,"    if ("+sname+".class.equals(c)) {");
                if (!sd.getTimePhased()) {
                  println(out,"      return ("+var+"= new "+sname+"Impl());");
                } else {
                  println(out,"      return null;");
                }
                println(out,"    } else");
              }
            }
            println(out,"      return super.generateDefaultPG(c);");
            println(out,"  }");
            println(out); 

            println(out,"  // dumb serialization methods");
            println(out);
          
            // writer
            println(out,"  private void writeObject(ObjectOutputStream out) throws IOException {");
            println(out,"    out.defaultWriteObject();");
            for (SlotD sd: cd.getSlotDs()) {
              if (! sd.getTrans()) {
                String sname = sd.getName();
              
                String var = "my"+sname;
                if (sd.getTimePhased()) {
                  var = var+"Schedule";
                }
                println(out,"      if ("+var+" instanceof Null_PG || "+var+" instanceof Future_PG) {\n"+
                        "        out.writeObject(null);\n"+
                        "      } else {\n"+
                        "        out.writeObject("+var+");\n"+
                        "      }");
              }
            }
            println(out,"  }");
            println(out);

            // reader
            println(out,"  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {");
            println(out,"    in.defaultReadObject();");
            for (SlotD sd: cd.getSlotDs()) {
              String sname = sd.getName();
              String stype = sd.getType();

              String var = "my"+sname;
              if (sd.getTimePhased()) {
                var = var+"Schedule";
                stype = "PropertyGroupSchedule";
              }

              if (! sd.getTrans()) {
                println(out,"      "+var+"=("+stype+")in.readObject();");
              } else {
                println(out,"      "+var+"="+sd.getInit()+";");
              }
            }
            println(out,"  }");

          }

          println(out,"  // beaninfo support");
          writeBeanInfoBody(out,cd);

          println(out,"}");
          
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
        println(out,"  private static PropertyDescriptor properties[];\n"+
                    "  static {");
        List<SlotD> v = cd.getSlotDs();
        int l = v.size();
        if (l == 0) {
          println(out,"    properties = new PropertyDescriptor["+l+"];");
        } else {
          println(out,"    try {");
          println(out,"      properties = new PropertyDescriptor["+l+"];");

          int i = 0;
          for (SlotD sd : v) {
            String sname = sd.getName();
            
            if (sd.getTimePhased()) {
              println(out,"      properties["+i+"] = new PropertyDescriptor(\""+
                          sname+"Schedule\", "+
                          name+".class, "+
                          "\"get"+sname+"Schedule\", "+
                          "null);");
            } else {
              println(out,"      properties["+i+"] = new PropertyDescriptor(\""+
                          sname+"\", "+
                          name+".class, "+
                          "\"get"+sname+"\", "+
                          "null);");

            }
            i++;
          }
          println(out,"    } catch (IntrospectionException ie) {}");
        }
        println(out,"  }");
        println(out);
        println(out,"  public PropertyDescriptor[] getPropertyDescriptors() {\n"+
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
      for (ClassD cd : classds) {
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
          PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getTargetDir(),path))));
          writeCR(out,deffilename);

          println(out);
	  println(out,"package " + asset_package + ";");
          println(out,"import java.beans.*;");
          println(out);
          println(out,"/** BeanInfo for "+name+" **/");
          println(out);
          
          for (String imp : cd.getImports()) {
            println(out,"import "+imp+";");
          }


          println(out,"public class "+name+"BeanInfo"+ext+" {");
          writeBeanInfoBody(out, cd);
          println(out,"}");
          out.close();
        } catch (Exception e) {
          System.err.println("Caught "+e);
        }
      }
    }

    public void addHasRelationshipsImpl(PrintWriter out, String slotName) {
      String arg = "_arg" + slotName;
      
      println(out,"  public RelationshipSchedule getRelationshipSchedule() {");
      println(out,"    return get" + slotName + 
              "().getRelationshipSchedule();");
      println(out,"  }");
      
      
      println(out,"  public void setRelationshipSchedule(RelationshipSchedule schedule) {");
      println(out,"    New" + slotName + " " + arg + " = (New" + 
              slotName + ") get" + slotName + "().copy();");
      println(out,"    " + arg + ".setRelationshipSchedule(schedule);");
      println(out,"    set" + slotName + "(" + arg + ");");
      println(out,"  }");
      
      println(out, "");
      println(out,"  public boolean isLocal() {");
      println(out,"    return get" + slotName + 
              "().getLocal();");
      println(out,"  }");
      
      println(out,"  public void setLocal(boolean localFlag) {");
      println(out,"    New" + slotName + " " + arg + " = (New" + 
              slotName + ") get" + slotName + "().copy();");
      println(out,"    " + arg + ".setLocal(localFlag);");
      println(out,"    set" + slotName + "(" + arg + ");");
      println(out,"  }");
      
      println(out, "");
      println(out,"  public boolean isSelf() {");
      println(out,"    return get" + slotName + 
              "().getLocal();");
      println(out,"  }");
      
      println(out, "");
    }

  }


  public void debug(String s) {
    if (isVerbose)
      System.err.println(s);
  }

  public Session session = null;
  
  protected PGParser pgParser = null;

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
          // must be a '/', NOT filesystem dependent!
          if (filename.lastIndexOf('/')!=-1) {
            pf = new File(filename);
          } else {
            pf = new File(getSourceDir(), filename);
          }
          stream = new FileInputStream(pf);
        } else {
          System.out.println("using class loader: " + filename);
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
        System.err.println("targetdir="+getSourceDir());
        System.err.println("Unable to parse " + filename);
        System.err.println("Caught: "+e);
        e.printStackTrace();
      }
    }
  }
      
  protected void processFile(String assetFilename) {
    InputStream stream = null;

    try {
	setDirectories(assetFilename);
      if (assetFilename.equals("-")) {
        debug("Reading from standard input.");
        stream = new java.io.DataInputStream(System.in);
      } else {
        debug("Reading \""+assetFilename+"\".");
        deffilename=assetFilename;
        stream = new FileInputStream(assetFilename);
      }

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

  private String asset_package = "org.cougaar.planning.ldm.asset";
  
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
        } else if (arg.equals("-d")) {
          targetDirName = arguments[++i];
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

  public String deffilename=null;
  
  public AssetWriter(String args[]) {
    arguments = args;
  }

  public static void main(String argv[]) {
    AssetWriter aw = new AssetWriter(argv);
    aw.start();
  }

}
