/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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


