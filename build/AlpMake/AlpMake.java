
/**
  *
  *
  *
  */
public class AlpMake {

  static MakeHelper makehelper = null;


  // KEEP commandLineParams UP TO DATE -- used  when user asks for help
  public static String commandLineParams = new String(" -s <sourcedir>  -d <destdir>  [-bootclasspath <bootclasspath>]  [-deprication]");


  /**
   **
   **
   **/
  public static void main(String[] args) {

     makehelper = new MakeHelper();

    // Parse the arguments
    if(args.length > 0) {   
       int i;
       for(i=0; i<args.length; i++) {
          if ( args[i].equals("-s") == true ) {
             makehelper.setSourceDirectory(args[(i+1)]);
          }
          else if( args[i].equals("-d") == true ) {
             makehelper.setClassDirectory(args[(i+1)]);
          }
          else if( args[i].equals("-bootclasspath") == true ) {
             makehelper.setBootclasspath(args[(i+1)]);
          }
          else if( args[i].equals("-deprecation") == true ) {
 	    makehelper.deprecation = true;
          }
          else if( (args[i].equals("-h")==true) || (args[i].equals("-help")==true) ) {
	     System.out.println("Usage: java AlpMake "+  commandLineParams);
	     System.exit(0);
          }
          else if( args[i].equals("-jikes") == true ) {
             makehelper.usingJikes=true;
          }
 
       } // for
    }
    else {
        System.out.println("Usage: java AlpMake "+  commandLineParams);
        System.exit(0);
    }

     makehelper.scan(System.out);
     makehelper.makeDirs(System.out);

  } // main()


} // class AlpMake


