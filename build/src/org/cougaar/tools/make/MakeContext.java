/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.tools.make;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

public class MakeContext {
    private static final String SRC = "src";
    private static final String TMPDIR = "tmpdir";
    private static final String CLASSES = "classes";
    private static final String GENCODE = "gencode";
    private static final String BUILD_CLASSES = "build" + File.separator + TMPDIR + File.separator + "classes";
    private static final String BUILD_JAR = "build.jar";
    private static final String DEFRUNNER_CLASS = "org.cougaar.tools.build.DefRunner";
    private static final String JAR_CLASS = "sun.tools.jar.Main";
    private static final String JAVAC_CLASS = "sun.tools.javac.Main";

    public static final String PROP_PREFIX           = "org.cougaar.tools.make.";
    public static final String PROP_BASEDIR          = PROP_PREFIX + "basedir";
    public static final String PROP_DEBUG            = PROP_PREFIX + "debug";
    public static final String PROP_TEST             = PROP_PREFIX + "test";
    public static final String PROP_JIKES_CLASS_PATH = PROP_PREFIX + "jikes.class.path";
    public static final String PROP_JIKES            = PROP_PREFIX + "jikes";
    public static final String PROP_3RD_PARTY_JARS   = PROP_PREFIX + "3rd.party.jars";
    public static final String PROP_JDK_TOOLS        = PROP_PREFIX + "jdk.tools";
    public static final String PROP_DEFAULT_TARGET   = PROP_PREFIX + "default.target";
    public static final String PROP_NO_PREREQUISITES = PROP_PREFIX + "no.prerequisites";
    public static final String PROP_DEPRECATION      = PROP_PREFIX + "deprecation";
    public static final String PROP_OMIT             = PROP_PREFIX + "omit.module.";

    public static final String DEFAULT_TARGET = "compileDir";
    public static final String COLON = " -- "; // Redefine COLON to avoid emacs error match

    private String theModuleName;
    private File theCurrentDirectory;
    private File theProjectRoot;
    private File theProjectLib;
    private File theProjectBin;
    private File theModuleRoot;
    private File theSourceRoot;
    private File theClassesRoot;
    private File theModuleTemp;
    private File theCodeGeneratorJar;
    private File theCodeGeneratorClasses;
    private File theGenCodeRoot;
    private File the3rdPartyDirectory;
    private File theJDKToolsJar;
    private File[] theModuleRoots;
    private Targets theTargets = new Targets(this);
    private final String[] JAVA  = {"java"};
    private final String[] JAVAC = {"javac", "-g"};
    private final String[] JIKES = {"jikes", "+D", "-g"};
    private final String[] RMIC  = {"rmic"};
    private final String[] ETAGS = {"etags",
                                    "-r",
                                    "/.* interface +\\([a-zA-Z0-9_]+\\) ?/\\1/"
    };
    private final String[] JAR   = {"jar"};
    private Class[] targetMethodParameterTypes = new Class[0];
    private Object[] targetMethodParameters = new Class[0];
    private Properties theProperties;
    public boolean debug = false;
    public boolean test = false;
    public String jikesClassPath = null;
    public boolean jikes = false;
    private String[] theExtensionsToJar = {
        ".def",
        ".props",
        ".gif",
        ".jpg",
        ".png"
    };
    private Set madeTargets = new HashSet();

    /**
     * Constructor initializes variables from a given set of
     * properties. Everything except per module information is set.
     **/
    public MakeContext(Properties someProperties) throws IOException {
	theProperties = someProperties;
	String basedir = theProperties.getProperty(PROP_BASEDIR);
	if (basedir == null) throw new IOException("basedir not specified");

	theProjectRoot = new File(basedir).getCanonicalFile();
	debug = theProperties.getProperty(PROP_DEBUG, "false").equalsIgnoreCase("true");
	test = theProperties.getProperty(PROP_TEST, "false").equalsIgnoreCase("true");
	jikes = theProperties.getProperty(PROP_JIKES, "false").equalsIgnoreCase("true");
	jikesClassPath = theProperties.getProperty(PROP_JIKES_CLASS_PATH);
	String third = theProperties.getProperty(PROP_3RD_PARTY_JARS);
	if (third == null) {
	    the3rdPartyDirectory = new File(theProjectRoot, "jars");
	} else {
	    the3rdPartyDirectory = new File(third).getCanonicalFile();
	}
        String jdkToolsJar = theProperties.getProperty(PROP_JDK_TOOLS);
        if (jdkToolsJar != null) {
            theJDKToolsJar = new File(jdkToolsJar);
        }
	theProjectLib = new File(theProjectRoot, "lib").getCanonicalFile();
	theProjectBin = new File(theProjectRoot, "bin").getCanonicalFile();
        theCurrentDirectory = new File(".").getCanonicalFile();
	theCodeGeneratorJar = new File(theProjectLib, BUILD_JAR);
	theCodeGeneratorJar = new File(theProjectRoot, BUILD_CLASSES);
        theModuleRoots = getProjectRoot().listFiles(new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory() && new File(f, "src").isDirectory()) {
                    return (theProperties.getProperty(PROP_OMIT + f.getName(), "false")
                            .equals("false"));
                }
                return false;
            }
        });
    }

    /**
     * Get all the module roots as an array of File.
     **/
    public File[] getAllModuleRoots() {
        return theModuleRoots;
    }

    /**
     * Infer what module is being made by searching up the directory
     * hierarchy until a subdirectory of the project root is found.
     * The name of that subdirectory is the name of the default module.
     **/
    private String inferModuleName() {
	File dir = getCurrentDirectory();
	File base = getProjectRoot();
	while (dir != null) {
	    File parent = dir.getParentFile();
	    if (parent == null) break;
	    if (parent.equals(base)) {
		return dir.getName();
	    }
	    dir = parent;
	}
	return null;
    }

    /**
     * Make a target in all modules. Loops through the module roots
     * and makes the given target for each module.
     **/
    private void forAllModules(String target) throws MakeException {
        File[] moduleRoots = getAllModuleRoots();
        System.out.println("all." + target + COLON + moduleRoots.length + " modules");
        for (int i = 0; i < moduleRoots.length; i++) {
            String moduleName = moduleRoots[i].getName();
            if (moduleName.equals("build")) continue;
            String tgt = moduleName + "." + target;
            makeTarget(tgt);
        }
    }

    /**
     * Make a particular targe. Targets are of the form
     * <module>.<target> If <module> is all, makes the target in all
     * modules. If the <module> is omitted, makes the target in the
     * current module or the default module if there is not current
     * module. Targets that have already been made, are skipped.
     **/
    public void makeTarget(String targetName) throws MakeException {
        if (debug) {
            System.out.println("makeTarget" + COLON + targetName);
        }
        int dotPos = targetName.indexOf('.');
        String newModuleName = null;
        if (dotPos < 0) {
            if (theModuleName == null) { // Must have a module
                newModuleName = inferModuleName();
                if (newModuleName == null) {
                    throw new MakeException("Cannot infer module from current directory");
                }
            } else {
                newModuleName = theModuleName;
            }
        } else {
            newModuleName = targetName.substring(0, dotPos);
            targetName = targetName.substring(dotPos + 1);
        }
        if (newModuleName != null && newModuleName.equals("all")) {
            try {
                String allTargetName = "all_" + targetName;
                Method targetMethod =
                    Targets.class.getMethod(allTargetName, targetMethodParameterTypes);
                if (debug) {
                    System.out.println("Target" + COLON + allTargetName);
                }
                targetMethod.invoke(theTargets, targetMethodParameters);
            } catch (NoSuchMethodException nsme) {
                forAllModules(targetName);
            } catch (InvocationTargetException ite) {
                Throwable targetException = ite.getTargetException();
                if (targetException instanceof MakeException) {
                    throw (MakeException) targetException;
                }
                targetException.printStackTrace();
                throw new MakeException(targetException.toString());
            } catch (Exception e) {
                e.printStackTrace();
                throw new MakeException("makeTarget exception: " + e);
            }
            return;
        }
        String key = newModuleName + "." + targetName;
        if (madeTargets.contains(key)) {
            return; // Already made
        }
        madeTargets.add(key);
        String savedModuleName = null;
        if (newModuleName != null && !newModuleName.equals(theModuleName)) {
            if (debug) {
                System.out.println("setModule" + COLON + newModuleName);
            }
            savedModuleName = theModuleName;
            setModule(newModuleName);
        }
        try {
            Method targetMethod =
                Targets.class.getMethod(targetName, targetMethodParameterTypes);
            if (debug) {
                System.out.println("Target" + COLON + targetName);
            }
            targetMethod.invoke(theTargets, targetMethodParameters);
        } catch (NoSuchMethodException nsme) {
            throw new MakeException("Unknown target: " + targetName);
        } catch (InvocationTargetException ite) {
            Throwable targetException = ite.getTargetException();
            if (targetException instanceof MakeException) {
                throw (MakeException) targetException;
            }
            targetException.printStackTrace();
            throw new MakeException(targetException.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new MakeException("makeTarget exception: " + e);
        } finally {
            if (savedModuleName != null) {
                if (debug) {
                    System.out.println("restoreModule" + COLON + savedModuleName);
                }
                setModule(savedModuleName);
            }
        }
    }

    /**
     * Recomputes the per-module variables for a new module name.
     **/
    private void setModule(String aModuleName) throws MakeException {
        try {
            if (theModuleName != null && theModuleName.equals(aModuleName)) return;
            theModuleName = aModuleName;
            theModuleRoot = getModuleRoot(theModuleName).getCanonicalFile();
            theSourceRoot = getSourceRoot(theModuleRoot);
            theModuleTemp = getModuleTemp(theModuleRoot);
            theClassesRoot = getClassesRoot(theModuleRoot);
            theGenCodeRoot = getGenCodeRoot(theModuleRoot);
            if (debug) {
                System.out.println("Module Name" + COLON + theModuleName);
                System.out.println("Module Root" + COLON + theModuleRoot);
                System.out.println("Source Root" + COLON + theSourceRoot);
                System.out.println("Module Temp" + COLON + theModuleTemp);
                System.out.println("Classes Root" + COLON + theClassesRoot);
            }
        } catch (IOException ioe) {
            throw new MakeException(ioe.toString());
        }
    }

    /**
     * Get the root of java source tree (in canonical form)
     **/
    public File getSourceRoot() {
        return theSourceRoot;
    }

    public File getSourceRoot(File aModuleRoot) throws MakeException {
        try {
            return new File(aModuleRoot, SRC).getCanonicalFile();
        } catch (IOException ioe) {
            throw new MakeException(ioe.toString());
        }
    }

    /**
     * Get the root of project tree (in canonical form)
     **/
    public File getProjectRoot() {
        return theProjectRoot;
    }

    /**
     * Get the project lib directory (has jars) (in canonical form)
     **/
    public File getProjectLib() {
        return theProjectLib;
    }

    /**
     * Get the project bin directory (has executable jars) (in canonical form)
     **/
    public File getProjectBin() {
        return theProjectBin;
    }

    /**
     * Get the root of java class tree (in canonical form)
     **/
    public File getClassesRoot() {
        return theClassesRoot;
    }

    public File getClassesRoot(File aModuleRoot) throws MakeException {
        try {
            return new File(getModuleTemp(aModuleRoot), CLASSES)
                .getCanonicalFile();
        } catch (IOException ioe) {
            throw new MakeException(ioe.toString());
        }
    }

    /**
     * Get the temp directory of a module (in canonical form)
     **/
    public File getModuleTemp() {
        return theModuleTemp;
    }

    /**
     * Get the temp directory of a module (in canonical form)
     **/
    public File getModuleTemp(File aModuleRoot) throws MakeException {
        try {
            return new File(aModuleRoot, TMPDIR).getCanonicalFile();
        } catch (IOException ioe) {
            throw new MakeException(ioe.toString());
        }
    }

    /**
     * Get the root of generated code tree (in canonical form)
     **/
    public File getGenCodeRoot() {
        return theGenCodeRoot;
    }

    public File getGenCodeRoot(File aModuleRoot) throws MakeException {
        try{
            return new File(getModuleTemp(aModuleRoot), GENCODE)
                .getCanonicalFile();
        } catch (IOException ioe) {
            throw new MakeException(ioe.toString());
        }
    }

    /**
     * Get the current directory (in canonical form)
     **/
    public File getCurrentDirectory() {
        return theCurrentDirectory;
    }

    /**
     * Get the module directory (in canonical form)
     **/
    public File getModuleRoot() {
        return theModuleRoot;
    }

    /**
     * Get the module directory for a particular module. Not
     * necessarily in canonical form.
     **/
    public File getModuleRoot(String aModuleName) {
        return new File(theProjectRoot, aModuleName);
    }

    /**
     * Get the module name
     **/
    public String getModuleName() {
        return theModuleName;
    }

    /**
     * Get the directory containing third party jars (in canonical form)
     **/
    public File get3rdPartyDirectory() {
        return the3rdPartyDirectory;
    }

    /**
     * Get the File of the jdk tools jar
     **/
    public File getJDKToolsJar() {
        return theJDKToolsJar;
    }

    public boolean haveJDKTools() {
        File jdkTools = getJDKToolsJar();
        return (jdkTools != null && jdkTools.exists());
    }

    /**
     * Get an array of the file extension that should extracted from
     * the src tree and included in the jar file.
     **/
    public String[] getExtensionsToJar() {
        return theExtensionsToJar;
    }

    /**
     * Get prerequisite modules for a given module.
     **/
    public String[] getPrerequisites(String aModuleName) {
        Set closure = new HashSet();
        getPrerequisites(aModuleName, closure);
        return (String[]) closure.toArray(new String[closure.size()]);
    }

    private void getPrerequisites(String aModuleName, Set closure) {
        StringTokenizer tokens =
            new StringTokenizer(theProperties.getProperty(PROP_PREFIX + aModuleName + ".prerequisites", ""));
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (closure.add(token)) {
                getPrerequisites(token, closure);
            }
        }
    }

    /**
     * Print a command to be executed represented as an array of strings.
     **/
    private void printCommand(String[] args) {
	for (int i = 0; i < args.length; i++) {
	    if (i > 0) System.out.print(" ");
	    System.out.print(args[i]);
	}
	System.out.println();
    }

    /**
     * Perform a java compilation using javac (or jikes if enabled)
     **/
    public void javac(File[] sources) throws MakeException {
        int offset = 0;
        MakeException e = null;
        while (offset < sources.length) {
            int nfiles = Math.min(sources.length - offset, 200);
            List command = new ArrayList();
            if (jikes && jikesClassPath != null) {
                command.addAll(Arrays.asList(JIKES));
            } else {
                command.addAll(Arrays.asList(JAVAC));
            }
            if (isDeprecation()) command.add("-deprecation");
            command.add("-classpath");
            command.add(getClassPath());
            command.add("-d");
            command.add(getClassesRoot().getPath());
            try {
                runExecutable(command, sources, offset, nfiles);
            } catch (MakeException me) {
                e = me;
            }
            offset += nfiles;
        }
        if (e != null) throw e;
    }

    /**
     * Run an executable program in a subprocess. The standard out and
     * error streams are copied.
     **/
    private void runExecutable(List command, Object[] xargs, int offset, int nargs)
        throws MakeException
    {
        List argList = new ArrayList(command);
        for (int i = 0; i < nargs; i++) {
            argList.add(xargs[i + offset].toString());
        }
        String[] args = (String[]) argList.toArray(new String[argList.size()]);
        if (debug) {
            printCommand(args);
        }
        try {
            Process process = Runtime.getRuntime().exec(args);
            Sucker outSucker =
                new Sucker(process.getInputStream(), System.out, "javac-stdout");
            Sucker errSucker =
                new Sucker(process.getErrorStream(), System.err, "javac-stderr");
            int exitValue = process.waitFor();
            outSucker.join();
            errSucker.join();
            if (exitValue != 0) {
                throw new MakeException("Compilation error: " + exitValue);
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            throw new MakeException("Interrupted in javac: " + ie);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new MakeException("IOException in javac: " + ioe);
        }
    }

    private void runJavaMain(String className, String[] args) throws MakeException {
        runJava(className, "main", new Class[] {String[].class}, new Object[] {args});
    }

    private void runJava(String className, String methodName, Class[] argTypes, Object[] args)
        throws MakeException
    {
        try {
            ClassLoader pcl = getClass().getClassLoader().getParent();
            URLClassLoader cl = new URLClassLoader(getClassPathURLs(), pcl);
            Class cls = cl.loadClass(className);
            Method method = cls.getMethod(methodName, argTypes);
            method.invoke(null, args);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MakeException(e.toString());
        }
    }

    /**
     * Run jar on a bunch of arguments.
     **/
    public void jar(File jarFile, File manifestFile, JarSet[] jarSets) throws MakeException {
        List args = new ArrayList();
        if (manifestFile != null) {
            args.add("-cmf");
            args.add(manifestFile.getPath());
        } else {
            args.add("-cf");
        }
        args.add(jarFile.getPath());
        for (int j = 0; j < jarSets.length; j++) {
            JarSet jarSet = jarSets[j];
            if (jarSet.files == null) {
                args.add("-C"); // Do all files
                args.add(jarSet.root.getPath());
                args.add(".");
            } else {
                String[] tails = getTails(jarSet.root, jarSet.files);
                for (int i = 0; i < tails.length; i++) {
                    args.add("-C");
                    args.add(jarSet.root.getPath());
                    args.add(tails[i]);
                }
            }
        }
        String[] argStrings = (String[]) args.toArray(new String[args.size()]);
        runExecutable(Arrays.asList(JAR), argStrings, 0, argStrings.length);
    }

    /**
     * Get the tails of a bunch of files relative to some ancestor
     * directory of all the files.
     **/
    public String[] getTails(File root, File[] files) throws MakeException {
        String[] result = new String[files.length];
        String head = root.getPath();
        if (!head.endsWith(File.separator)) head += File.separator;
        int headLength = head.length();
        for (int i = 0; i < files.length; i++) {
            String path = files[i].getPath();
            if (!path.startsWith(head)) {
                throw new MakeException("getTails: " + path + " doesn't start with " + head);
            }
            result[i] = path.substring(headLength);
        }
        return result;
    }


    /**
     * Change the root of a file. The resultant file has the same tail
     * relative to the root, but has the tgtRoot. The suffix
     * (extension) can also be changed.
     **/
    public File reroot(File src, File srcRoot, File tgtRoot, String newSuffix) throws MakeException {
	String srcRootPath = srcRoot.getPath();
	String tgtRootPath = tgtRoot.getPath();
	String srcPath = src.getPath();
        if (!srcPath.startsWith(srcRootPath)) {
            throw new IllegalArgumentException("Wrong root: " + srcRoot + " for " + src);
        }
	int xpos = -1;
	if (newSuffix != null) {
	    xpos = srcPath.lastIndexOf('.');
	} else {
	    newSuffix = "";
	}
	if (xpos < 0) xpos = srcPath.length();
	String tail = srcPath.substring(srcRootPath.length(), xpos) + newSuffix;
	return new File(tgtRoot, tail);
    }

    /**
     * Run a code generator on a .def file. The output is written to
     * the directory of a given .gen file.
     **/
    public void generateCode(File defFile, File genFile) throws MakeException {
        insureDirectory(genFile.getParentFile());
        String[] args = {
            "-options",
            "-d " + genFile.getParent(),
            defFile.getPath()
        };
        if (true) {
            runJavaMain(DEFRUNNER_CLASS, args);
        } else {
            List command = new ArrayList(Arrays.asList(JAVA));
            command.add("-classpath");
            command.add(getClassPath());
            command.add(DEFRUNNER_CLASS);
            runExecutable(command, args, 0, args.length);
        }
    }
	    
    /**
     * Delete some files
     **/
    public void delete(File[] files) throws MakeException {
        for (int i = 0; i < files.length; i++) {
	    if (files[i].delete()) {
		if (debug) {
		    System.out.println("Delete" + COLON + files[i] + " ok");
		}
	    } else {
		if (debug) {
		    System.out.println("Delete" + COLON + files[i] + " failed");
		}
	    }
	}
    }

    public void etags(File tagsFile, File[] sources, File[] tagFiles, boolean append)
	throws MakeException
    {
        int offset = 0;
        MakeException e = null;
        List writeCmd = new ArrayList(Arrays.asList(ETAGS));
        writeCmd.add("-o");
        writeCmd.add(tagsFile.getPath());
        List appendCmd = new ArrayList(writeCmd);
        appendCmd.add("-a");
        if (sources != null) {
            while (offset < sources.length) {
                int nfiles = Math.min(sources.length - offset, 20);
                List args = new ArrayList();
                for (int i = 0; i < nfiles; i++) {
                    args.add(sources[i + offset]);
                }
                try {
                    runExecutable(append ? appendCmd : writeCmd, args.toArray(), 0, args.size());
                } catch (MakeException me) {
                    e = me;
                }
                offset += nfiles;
                append = true;
            }
        }
        if (tagFiles != null) {
            String[] tagArgs = new String[tagFiles.length * 2];
            for (int i = 0; i < tagFiles.length; i++) {
                tagArgs[2*i+0] = "-i";
                tagArgs[2*i+1] = tagFiles[i].getPath();
            }
            try {
                runExecutable(append ? appendCmd : writeCmd, tagArgs, 0, tagArgs.length);
            } catch (MakeException me) {
                e = me;
            }
        }
        if (e != null) throw e;
    }

    /**
     * Find file under a given directory having a particular suffix
     * (extension).
     * @param base the directory beneath which the files are to be
     * found
     * @param suffix the suffix with which the files must end. Null
     * means to find all files.
     * @recurse descend into subdirectories recursively if true.
     **/
    public static File[] findFiles(File base, String suffix, boolean recurse, boolean includeDirectories)
	throws MakeException
    {
	if (!base.isDirectory()) {
            return new File[0];
	}
        List files = new ArrayList();
        findFiles(files, base, suffix, recurse, includeDirectories);
        return (File[]) files.toArray(new File[files.size()]);
    }

    /**
     * Find files and add them to a List.
     **/
    private static void findFiles(List files, File dir, final String suffix,
			   boolean recurse, boolean includeDirectories)
	throws MakeException
    {
	if (!dir.isDirectory()) {
	    throw new MakeException("base not directory: " + dir);
	}
        File[] x = dir.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isFile() && (suffix == null || f.getPath().endsWith(suffix));
            }
        });
        for (int i = 0; i < x.length; i++) {
            files.add(x[i]);
        }
        if (recurse) {
            x = dir.listFiles(new FileFilter() {
                public boolean accept(File f) {
		    return f.isDirectory() && !f.getName().startsWith(".");
                }
            });
	    for (int i = 0; i < x.length; i++) {
		findFiles(files, x[i], suffix, true, includeDirectories);
	    }
	}
	if (includeDirectories) {
	    files.add(dir);
	}
    }

    /**
     * Get an array of target Files corresponding to the supplied array
     * of source files.
     **/
    public File[] getTargets(File tgtRoot, File srcRoot, File[] srcs, String suffix)
  	throws MakeException
    {
        File[] result = new File[srcs.length];
        String srcRootPath = srcRoot.getPath() + srcRoot.separator;
        if (debug) {
            System.out.println("Rerooting " + srcRoot + " to " + tgtRoot);
        }
        for (int i = 0; i < result.length; i++) {
	    result[i] = reroot(srcs[i], srcRoot, tgtRoot, suffix);
        }
        return result;
    }

    /**
     * Get the subset of a given set of target files that are either
     * non-existent or older that their corresponding source file.
     **/
    public File[] getOutdatedTargets(File[] targets, File[] sources)
  	throws MakeException
    {
        List needed = new ArrayList();
        for (int i = 0; i < sources.length; i++) {
            File tgt = targets[i];
            File src = sources[i];
            if (!tgt.exists() || tgt.lastModified() < src.lastModified()) {
		if (debug) {
		    if (tgt.exists()) {
			System.out.println("Outdated "
                                           + tgt
                                           + COLON
                                           + new Date(tgt.lastModified())
                                           + "<"
                                           + new Date(src.lastModified()));
		    } else {
			System.out.println("Non-existent "
                                           + tgt);
		    }
		}
		needed.add(src);
            } else {
		if (debug) {
		    System.out.println("Uptodate " + tgt);
		}
	    }
        }
        return (File[]) needed.toArray(new File[needed.size()]);
    }

    /**
     * Get an array of all the third party jars.
     **/
    public File[] get3rdPartyJars() throws MakeException {
	File dir = get3rdPartyDirectory();
	if (dir == null || !dir.isDirectory()) return new File[0];
	return findFiles(dir, ".jar", false, false);
    }

    private URL[] getClassPathURLs() throws MakeException {
        try {
            File[] cpe = getClassPathElements();
            URL[] result = new URL[cpe.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = cpe[i].toURL();
            }
            return result;
        } catch (MalformedURLException mue) {
            throw new MakeException(mue.toString());
        }
    }

    /**
     * Get a classpath for a compilation or running code generators.
     **/

    private String getClassPath() throws MakeException {
        File[] cpe = getClassPathElements();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < cpe.length; i++) {
            if (i > 0) buf.append(";");
            buf.append(cpe[i].getPath());
        }
	if (jikesClassPath != null) {
	    buf.append(';').append(jikesClassPath);
	}
        return buf.substring(0);
    }

    private File[] getClassPathElements() throws MakeException {
        List elements = new ArrayList();
        elements.add(getSourceRoot());
        elements.add(getClassesRoot());
        File genCodeRoot = getGenCodeRoot();
        if (genCodeRoot.isDirectory()) {
            elements.add(genCodeRoot);
        }
        String[] prerequisiteModules = getPrerequisites(getModuleName());
        for (int i = 0; i < prerequisiteModules.length; i++) {
            elements.add(getSourceRoot(getModuleRoot(prerequisiteModules[i])));
            elements.add(getClassesRoot(getModuleRoot(prerequisiteModules[i])));
        }
        elements.addAll(Arrays.asList(findFiles(getProjectLib(), ".jar", false, false)));
        elements.addAll(Arrays.asList(get3rdPartyJars()));
        if (haveJDKTools()) {
            elements.add(getJDKToolsJar());
        }
        return (File[]) elements.toArray(new File[elements.size()]);
    }

    /**
     * Make a directory if necessary.
     **/
    public void insureDirectory(File dir) throws MakeException {
	if (dir.isDirectory()) return;
	if (!dir.mkdirs()) {
	    throw new MakeException("mkdirs failed: " + dir);
	}
	if (debug) {
	    System.out.println("mkdir" + COLON + dir);
	}
    }

    public boolean isNoPrerequisites() {
        return theProperties.getProperty(PROP_NO_PREREQUISITES, "false").equals("true");
    }

    public boolean isDeprecation() {
        return theProperties.getProperty(PROP_DEPRECATION, "false").equals("true");
    }

    /**
     * Inner class for sucking on the output streams of a subprocess.
     **/
    private static class Sucker extends Thread {
        private InputStream in;
        private OutputStream out;
        private byte[] buffer = new byte[1000];
        public Sucker(InputStream in, OutputStream out, String name) {
            super(name);
            this.in = in;
            this.out = out;
            start();
        }
        public void run() {
            while (true) {
                try {
                    int nb = in.read(buffer);
                    if (nb < 0) return;
                    out.write(buffer, 0, nb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public JarSet createJarSet(File root) {
        return new JarSet(root, null);
    }

    public JarSet createJarSet(File root, File dir, String suffix)
        throws MakeException
    {
        if (dir == null) dir = root;
        File[] files = findFiles(dir, suffix, true, false);
        if (debug)
            System.out.println(files.length + " " + suffix + " files in " + dir);
        return new JarSet(root, files);
    }

    public static class JarSet {
        public File root;
        public File[] files;
        private int fileCount = -1; // Haven't counted yet
        private long maxModificationTime = Long.MIN_VALUE;
        public JarSet(File root, File[] files) {
            this.root = root;
            this.files = files;
        }
        public long getMaxModificationTime() throws MakeException {
            if (fileCount < 0) processFiles();
            return maxModificationTime;
        }

        public int getFileCount() throws MakeException {
            if (fileCount < 0) processFiles();
            return fileCount;
        }

        private void processFiles() throws MakeException {
            File[] filesToCheck;
            if (files == null) {
                filesToCheck = findFiles(root, null, true, false);
            } else {
                filesToCheck = files;
            }
            fileCount = filesToCheck.length;
            maxModificationTime = Long.MIN_VALUE;
            for (int i = 0; i < filesToCheck.length; i++) {
                maxModificationTime =
                    Math.max(maxModificationTime,
                             filesToCheck[i].lastModified());
            }
        }
    }

    public void rmic(String[] classNames) throws MakeException {
        List command = new ArrayList();
        command.addAll(Arrays.asList(RMIC));
        command.add("-d");
        command.add(getClassesRoot().getPath());
        command.add("-classpath");
        command.add(getClassPath());
        runExecutable(command, classNames, 0, classNames.length);
    }
}
