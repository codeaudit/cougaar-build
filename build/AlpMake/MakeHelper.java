
import java.awt.List;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


public class MakeHelper {

  /** */
  private static final String GEN_PREFIX = ";!generate:";

  /** Root dir of java sources. Default is .  */
  private String sourceDirectory  =  ".";

  /** Root dir for generated .class files. Default is .*/
  private String classDirectory   =  ".";

  /**  */
  private String buildDirectory   =  ".";

  /** -bootclasspath value for javac. Default is ''. */
  private String bootclasspath    =  null; 

  /** Use -deprecation flag with javac. Default is false. */
  public  boolean deprecation     =  false;

  /** Not supported. */
  public  boolean usingJikes      =  false;


  /**
   *
   */
  public MakeHelper() {

  } // MakeHelper()


  //---------------------------------------------------------------------
  // CACHE  STUFF
  //
  // Use explicit Cache model for internal data
  //
  private Object FilesCacheKey = new Object();
  private Object DirsCacheKey = new Object();
  private Hashtable cachedResult = new Hashtable();

  /**
   *
   */
  private void cacheResult( Object key, Vector v )
  {
     cachedResult.put(key, v);
  }

  /**
   *
   */
  private Vector getCachedResult(Object key)
  {
     return (Vector)cachedResult.get(key);
  }
  // END CACHE STUFF
  //---------------------------------------------------------------------


  /**
   * Helper method. Change value of sourcedir. 
   * @param  sourcedir Directory root of java sources for javac.
   * @return nothing.
   */
  public void setSourceDirectory(String sourcedir)
  {
      // if null, use default (current working directory) for source directory
      if(sourcedir != null ) sourceDirectory = sourcedir;
  } // setSourceDirectory()


  /**
   * Helper method. Change value of classdir. 
   * @param  classdir Directory of java .class files generated
   *                  by javac.
   * @return nothing.
   */
  public void setClassDirectory(String classdir)
  {
      // if null, use default (current working directory) for source directory
      if(classdir != null ) classDirectory = classdir;
  } // setClassDirectory()


  /**
   *
   */
  public void setBuildDirectory(String builddir) {
    buildDirectory = builddir;
  } // setBuildDirectory()


  /**
   * Helper Method. Set the bootclasscath var. 
   * @param  bootClassPath  Passed as -bootclasspath &lt value &gt to javac.
   * @return nothing.
   */
  public void setBootclasspath(String bootclasspath) {
       this.bootclasspath = bootclasspath;
  } // setBootclasspath()



  /**
   *
   */
  public void scan( PrintStream out )
  {
     out.println(">Starting scan, scanning directory: " + sourceDirectory);
     Vector filenames = new Vector();
     Vector dirnames = new Vector();
     scan(filenames,dirnames);
     cacheResult(FilesCacheKey, filenames);
     cacheResult(DirsCacheKey, dirnames);

     //     printFilesVector(filenames, out);
     out.println("<Completed scan.");
  }


  /**
   *
   */
  private void scan(Vector filesvec, Vector dirsvec)
  {
      File currentdir = new File(sourceDirectory);
      
      // Add the root dir to the directory vector
      // if it contains java files, and doesn't
      // contain a SKIP_COMPILE file
      // 980918 md
      if ( (dirHasSkipCompileFile(currentdir)==false) && 
	   (dirHasJavaFiles(currentdir)==true) ) {
	dirsvec.addElement(currentdir);
      } // if
      
      scanRecurse(currentdir, filesvec, dirsvec);
 
  } // scan()

// Depth-first recursive accumulation of java files

  /**
   * Recurse all directories from the current directory,
   * adding the name of new directories to the dirsvec,
   * and the name of .java files to the filesvec. <br>
   * The method will NOT recurse directories called 'CVS' and
   * will look for a file called 'SKIP_COMPILE' or '.SKIP_COMPILE',
   * wich will cause the directory, and its children to be skipped.
   * eg: not added tot he dir/file list.
   *
   * @param curdir   The current working directory. (File)
   * @param filesvec A vector of FQ file names ending with .java.
   * @param dirsvec  A vector of FQ directory names.
   */
private void scanRecurse(File curdir, Vector filesvec, Vector dirsvec)
{
  // File filter not used now --
  //FilenameFilter filter = new javaFileFilter();
  String[]  files = curdir.list();
  int i;
 
  // for all in the current dir...
  for(i=0; i< files.length; i++) {
    // if( files[i].endsWith(".class") == false )  {  // not a .class file...
      // be sure to create file reference obj with current directory
      File f = new File( curdir, files[i] );       // create a new 'File'
      if (f.getName().endsWith(".java") == true) { // A .java file??
	filesvec.addElement( f );                  // Add it to the file vector
	// System.out.println(f.getName());
      } // if

      else if( f.isDirectory() == true ) {         // Is it a directory??

	// Stop descending when SKIP_COMPILE file has been hit.
	// Added 980825 -md
	if ( dirHasSkipCompileFile(f) == true) {    // Does the dir the we're looking at contain a SKIP_COMPILE file?
	  continue;             // YES, ignore the directroy and it's children.
	} // if 
	
	if( f.getName().endsWith("CVS") == false ) {  // Not a CVS directory
	  dirsvec.addElement(f);                      // Add to directory vector
	} // if
	
	scanRecurse(f, filesvec, dirsvec);            // Continue descending.
      } // else if
      // } // if
  } // for
} // void scanRecurse(File,Vector,Vector)




  /**
   * 20000322 Returns true if contain .java or .def files.
   *
   * See if the current dir contains any .java files.
   *
   * @param curdir The current directory (File)
   * @return  true if there are any .java files in the directory. 
   *
   */
  boolean dirHasJavaFiles(File curdir)
  {
    String[] files = curdir.list();
    for (int i=0; i<files.length; i++) {
      if ( (files[i].endsWith(".java")==true) || (files[i].endsWith(".def")==true)) {
        return true; 
      } // if
    } // for
    return false;
  } // boolean dirHasJavaFiles()



  /**
   * See if the source tree contains any '.def' file.
   * <p>
   * Traverse the vector of directories, 'dirsvec', and
   * accumulate all the .def files found in those directories.
   * <b> Note: </b> 'dirsvec' only contains directories
   * which contain '.java' files. It is assumed that there
   * is a .java file in a directory containing a .def file.
   * <p>
   * This could also be done when during the initial directory
   * scan.
   *
   * @param   dirsvec  A vector files.  This is a list of every
   *          directory containing .java files..
   * @return  Enumeration of .def files found
   */ 
  Enumeration getDefFilesFromSourceTree(Vector dirs) {
    Vector defFiles = new Vector();
    FilenameFilter defFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".def");
      }
    };
    for (Enumeration e = dirs.elements(); e.hasMoreElements(); ) {
      File dir = (File) e.nextElement();
      defFiles.addAll(Arrays.asList(dir.listFiles(defFilter)));
    }
    if (defFiles.size() > 0) {
      System.out.println("\nFirst .def file found is " + defFiles.elementAt(0) + "\n");
    } else {
      System.out.println("\nNo .def files found. \n");
    }
    return defFiles.elements();
  }


  /**
   * See if the current dir contains a file called
   * 'SKIP_COMPILE' or '.SKIP_COMPILE'. If this file is found, the directory
   * will not be compiled.
   *
   * @param curdir The current directory (File)
   * @return  true if there is a file called 'SKIP_COMPILE'
   *          or '.SKIP_COMPILE'
   *          in the directory. 
   *
   */
  boolean dirHasSkipCompileFile(File curdir)
  {
    String[] files = curdir.list();
    for (int i=0; i<files.length; i++) {
      if (files[i].equals("SKIP_COMPILE") == true) {
        return true; 
      } // if
      if (files[i].equals(".SKIP_COMPILE") == true) {
        return true; 
      } // if
    } // for
    return false;
  } // boolean dirHasSkipCompileFile()


  /**
   * For every cached directory: <\br> 
   *  - Construct a string to compile all .java files in the directory. <\br>
   *  - Compile the files, sending the output to stdout.
   *
   * @param out Output directed here (stdout).
   */
  public void makeDirs( PrintStream out )
  {

      // Create a list of directories.
      Vector dirs = getCachedResult(DirsCacheKey);
      if( dirs == null ) { throw new RuntimeException("No Directory Names Cached"); }

      // Create a list of files.
      Vector files = getCachedResult(FilesCacheKey);
      if( files == null ) { throw new RuntimeException("No File Names Cached"); }

      // Print statistics on the number of files and directories.
      out.println();
      out.println("found " + files.size() + " files in " + dirs.size() + " directories.");
      out.println();
     
      String path_sep = System.getProperty("path.separator"); // We're generating stuff for UNIX and NT.

      // Build a classpath string from the environment var CLASSPATH, and
      // add the source and destination directories to the front.
      String classpath = sourceDirectory + 
			 path_sep + 
			 classDirectory + 
			 path_sep + 
			 System.getProperty("java.class.path"); 

      // Set the compiler name and flags.    
      String compilerName  = new String();  // Name of the compiler.
      String compilerFlags = new String();  // Compiler options, including classpath.
      String cmd           = new String();  // Full command to be executed.
  
      if (usingJikes==true) {        // Using Jikes?? (Not supported)
	compilerName  = "jikes";
	compilerFlags = " +D -g  -classpath \"" + classpath + "\" -d " + classDirectory;
      } // if
      else {                         // Using javac??
	compilerName  = "javac";
	compilerFlags = " ";
	compilerFlags = compilerFlags + " -J-mx64m -g -nowarn -classpath \"" + classpath + "\" -d " + classDirectory;
        if (deprecation==true) {     // Deprecation specified from command line??
	  compilerFlags = compilerFlags + " -deprecation "; 
	} // if
        if (bootclasspath != null) {
          compilerFlags = compilerFlags + " -bootclasspath \"" + bootclasspath + "\"  ";
	} // if
      } // else


      // Write compile commands to a .sh and .bat file, which will be executed later.
      try {
        // Create the .bat and .sh files that will call javac.
	DataOutputStream bat = new DataOutputStream(new FileOutputStream("alp_compile.bat"));
	DataOutputStream sh  = new DataOutputStream(new FileOutputStream("alp_compile.sh"));

        // 1st line of generated file.
	sh.writeBytes("#!/bin/sh" + "\n\n");
	bat.writeBytes("@echo off" + "\n\n");
      
        // Read the .def files, and generate the apporpriate commands. 
	for (Enumeration defs = getDefFilesFromSourceTree(dirs); defs.hasMoreElements(); ) {
	  File defFile = (File) defs.nextElement();
          try {
            BufferedReader in = new BufferedReader(new FileReader(defFile));
            try {
              String firstLine = in.readLine();
              if (firstLine.startsWith(GEN_PREFIX)) {
                String gen = firstLine.substring(GEN_PREFIX.length());
                sh.writeBytes("\n");
                sh.writeBytes("echo   " + "\n");
		sh.writeBytes("echo \"=> .def file: " +  defFile + "\"" + "\n");
                sh.writeBytes("cd " + defFile.getParent() + "\n");
                sh.writeBytes("java -classpath \"" + classpath + "\" " + gen + "\n");
		bat.writeBytes("\n");
                bat.writeBytes("echo   " + "\n");
		bat.writeBytes("echo \"=> .def file: " +  defFile + "\"" + "\n");
                bat.writeBytes("cd " + defFile.getParent() + "\n");
                bat.writeBytes("java -classpath \"" + classpath + "\" " + gen + "\n");
              } else {
                throw new IOException("Bad generate prefix in " + defFile);
              }
            }
            finally {
              in.close();
            }
          }
          catch (IOException ioe) {
            System.err.println(ioe);
          }
	} // for


        // Generate a line for building every dir that contains a .java file...
	int i;
	Enumeration e = dirs.elements();
       	for(i=0; e.hasMoreElements(); i++) {
	  File f = (File) e.nextElement();
	
	  // for (i=dirs.size()-1; i>=0; i--) {
	  //   File f = (File) dirs.elementAt(i);
	  String dname = f.getAbsolutePath();

	  // No .java files in the current directory.
	  if (dirHasJavaFiles(f) == false) {
	    continue;  // Next dir...
	  } // if

	  // Now pickup up when scanning directories.
	  //      	  if (dirHasSkipCompileFile(f) == true) {
	  //       	    continue; // Next dir...
	  //       	  } // if

	  // Build the command.
	  cmd = compilerName + " " + compilerFlags + " " + dname +  f.separator + "*.java";
	  
	  // Print the command about to be executed.
	  //	  out.println("COMPILING: <" + i + "> " + f.getAbsolutePath());
	  //	  out.println(cmd);
           
	  // Write the command to the .bat file:
	  bat.writeBytes("echo  ." + "\n");
          bat.writeBytes("echo COMPILING: " + dname  + "\n");
	  bat.writeBytes(cmd + "\n\n");

	  // Write the command to the .sh file:
	  sh.writeBytes("echo   " + "\n");
	  sh.writeBytes("echo COMPILING: " + dname  + "\n");
	  sh.writeBytes(cmd + "\n\n");

	} // for

      } catch (Exception x) {  // If the file can't be written, dump stach, and abort.
	x.printStackTrace();
	System.exit(1);
      }

  } // makeDirs( PrintStream)
  


  /**
   *  Print helper
   */
  private void printStringVector( Vector strings, List list )
  {
      Enumeration e = strings.elements();
      for(;e.hasMoreElements();) {
         String s = (String)e.nextElement();
         list.add(s);
         // force select at end of list --
         list.select( list.getItemCount()-1);
      }
  } // printStringVector (Vector, List)

  /**
   *  Print helper
   */
  private void printFilesVector(Vector files, List list)
  {
      Enumeration e = files.elements();
      for(;e.hasMoreElements();) {
         // String s = (String)e.nextElement();
         File f = (File)e.nextElement();
         list.add(f.getAbsolutePath());
         // force select at end of list --
         list.select( list.getItemCount()-1);
      } // for
  } // printFilesVector(Vector, List)

  /**
   *  Print helper.
   */
  private void printFilesVector(Vector strings, PrintStream out)
  {
      Enumeration e = strings.elements();
      for(;e.hasMoreElements();) {
         //String s = (String)e.nextElement();
         File f = (File)e.nextElement();
         out.println(f.getAbsolutePath());
      } // for
  } // printFilesVector(Vector, PrintStream)

} // Class MakeHelper

