/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
package org.cougaar.tools.make;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class Make {
    public static final String USER_HOME = "user.home";
    private static Properties properties = new Properties();

    private static void usage(String msg) {
	System.err.println(msg);
	System.exit(-1);
    }

    private static void addProperties(File file, boolean notFoundOk) {
	try {
	    InputStream is = new FileInputStream(file);
	    properties.load(is);
	    is.close();
	} catch (FileNotFoundException fnfe) {
	    if (notFoundOk) return; // Quietly ignore non-existent files
	    usage("File not found: " + file);
	} catch (IOException ioe) {
	    usage("IO Error reading" + file);
	}
    }

    private static void addProperty(String name, String value) {
	properties.setProperty(name, value);
    }

    public static void main(String[] args) {
	List targets = new ArrayList();
	String home = System.getProperty(USER_HOME);
	File dir = new File(System.getProperty("user.dir"));
        File projectProperties = null;
	while (dir != null) {
	    File propFile = new File(dir, "make.properties");
	    if (propFile.exists()) {
		addProperties(propFile, true);
                projectProperties = propFile;
		break;
	    }
	    dir = dir.getParentFile();
	}
	if (home != null) addProperties(new File(new File(home), ".make.properties"), true);
	for (int i = 0; i < args.length; i++) {
	    String arg = args[i];
	    if (arg.equals("-basedir")) {
		addProperty(MakeContext.PROP_BASEDIR, args[++i]);
		continue;
	    }
	    if (arg.equals("-properties")) {
		addProperties(new File(args[++i]), false);
		continue;
	    }
	    if (arg.equals("-debug")) {
		addProperty(MakeContext.PROP_DEBUG, "true");
		continue;
	    }
	    if (arg.equals("-test")) {
		addProperty(MakeContext.PROP_TEST, "true");
		continue;
	    }
	    if (arg.equals("-noprerequisites")) {
		addProperty(MakeContext.PROP_NO_PREREQUISITES, "true");
		continue;
	    }
	    if (arg.equals("-deprecation")) {
		addProperty(MakeContext.PROP_DEPRECATION, "true");
		continue;
	    }
	    if (arg.equals("-pedantic")) {
		addProperty(MakeContext.PROP_PEDANTIC, "true");
		continue;
	    }
	    if (arg.equals("-jikesClassPath")) {
		addProperty(MakeContext.PROP_JIKES_CLASS_PATH, "true");
		continue;
	    }
	    if (arg.equals("-jikes")) {
		addProperty(MakeContext.PROP_JIKES, "true");
		continue;
	    }
	    if (arg.startsWith("-")) {
		usage("Unknown option: " + arg);
		continue;
	    }
	    targets.add(arg);
	}
        if (!properties.contains(MakeContext.PROP_BASEDIR)) {
            addProperty(MakeContext.PROP_BASEDIR, projectProperties.getParent());
        }
	try {
	    MakeContext context = new MakeContext(properties);
            if (targets.size() == 0) {
                targets.add(properties.getProperty(MakeContext.PROP_DEFAULT_TARGET,
                                                   MakeContext.DEFAULT_TARGET));
            }
	    for (Iterator i = targets.iterator(); i.hasNext(); ) {
		String tgt = (String) i.next();
		context.makeTarget(tgt);
	    }
  	} catch (MakeException me) {
  	    System.err.println(me);
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	} finally {
        }
    }
}
