package org.cougaar.tools.make;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Defines the standard targets. Each public method is a target.
 **/
public class Targets {
    private MakeContext theContext;

    public Targets(MakeContext aContext) {
	theContext = aContext;
    }

    public void projectLib() throws MakeException {
	theContext.insureDirectory(theContext.getProjectLib());
    }

    public void moduleClasses() throws MakeException {
	theContext.insureDirectory(theContext.getClassesRoot());

    }

    public void moduleGenCode() throws MakeException {
	theContext.insureDirectory(theContext.getGenCodeRoot());
    }

    /**
     * Compile the sources in the current directory that are
     * out-of-date w.r.t. their class file.
     **/
    private void compilePrerequisites() throws MakeException {
        String[] prerequisiteModules = theContext.getPrerequisites(theContext.getModuleName());
        for (int i = 0; i < prerequisiteModules.length; i++) {
            String moduleName = prerequisiteModules[i];
            if (theContext.getModuleRoot(moduleName).isDirectory()) {
                String tgt = moduleName + ".jar";
                theContext.makeTarget(tgt);
            }
        }
    }

    public void compileDir() throws MakeException {
        compileSome(theContext.getCurrentDirectory(), false);
    }
    public void compileAll() throws MakeException {
        compileSome(theContext.getCurrentDirectory(), true);
    }
    public void compile() throws MakeException {
        compileSome(theContext.getSourceRoot(), true);
    }
    private void compileSome(File srcDirectory, boolean recurse) throws MakeException {
	theContext.makeTarget("projectLib");
	theContext.makeTarget("moduleClasses");
        theContext.makeTarget("compileGenCode");
        compilePrerequisites();
        File[] sources = theContext.findFiles(srcDirectory, ".java", recurse);
        compileCommon(theContext.getSourceRoot(), sources);
    }

    public void compileGenCode() throws MakeException {
        theContext.makeTarget("generateCode");
        File srcRoot = theContext.getGenCodeRoot();
        if (srcRoot.isDirectory()) {
            File[] sources = theContext.findFiles(srcRoot, ".java", true);
            compileCommon(srcRoot, sources);
        }
    }

    private void compileCommon(File srcRoot, File[] sources) throws MakeException {
        File[] targets =
	    theContext.getTargets(theContext.getClassesRoot(),
				  srcRoot,
				  sources,
				  ".class");
	File[] needed = theContext.getOutdatedTargets(targets, sources);
        if (needed.length == 0) return;
        System.out.println("javac: Compiling " + needed.length + " files");
	theContext.javac(needed);
    }
    private void cleanRoot(File root, String suffix, boolean recurse) throws MakeException {
        if (root.isDirectory()) {
            File[] targets = theContext.findFiles(root, suffix, recurse);
            System.out.println("delete " + root + ": " + targets.length + " files");
            theContext.delete(targets);
        }
    }
    public void clean() throws MakeException {
        cleanRoot(theContext.getClassesRoot(), null, true);
        cleanRoot(theContext.getGenCodeRoot(), null, true);
    }
        
    public void generateCode() throws MakeException {
        compilePrerequisites();
	File[] sources = theContext.findFiles(theContext.getSourceRoot(), ".def", true);
        if (sources.length > 0) {
            theContext.makeTarget("moduleGenCode");
            for (int i = 0; i < sources.length; i++) {
                File defFile = sources[i];
                File genFile = theContext.reroot(defFile, theContext.getSourceRoot(), theContext.getGenCodeRoot(), ".gen");
                if (!(genFile.exists() && genFile.lastModified() >= defFile.lastModified())) {
                    System.out.println("generateCode: " + defFile);
                    theContext.generateCode(defFile, genFile);
                }
            }
        }
    }
    private void addFilesToJar(List args, File root, File[] files) throws MakeException {
        String[] tails = theContext.getTails(root, files);
        for (int i = 0; i < files.length; i++) {
            args.add("-C");
            args.add(root.getPath());
            args.add(tails[i]);
        }
    }

    private long getMaxModificationTime(File[] files) {
        long max = Long.MIN_VALUE;
        for (int i = 0; i < files.length; i++) {
            long thisTime = files[i].lastModified();
            if (thisTime > max) max = thisTime;
        }
        return max;
    }

    public void jar() throws MakeException {
        theContext.makeTarget("projectLib");
        theContext.makeTarget("compile");
        File jarFile = new File(theContext.getProjectLib(), theContext.getModuleName() + ".jar");
        long maxTime =
            getMaxModificationTime(theContext.findFiles(theContext.getClassesRoot(), null, true));
        String[] extensionsToJar = theContext.getExtensionsToJar();
        File[][] files = new File[extensionsToJar.length][];
        for (int i = 0; i < extensionsToJar.length; i++) {
            files[i] = theContext.findFiles(theContext.getSourceRoot(), extensionsToJar[i], true);
            maxTime = Math.max(maxTime, getMaxModificationTime(files[i]));
        }
        if (maxTime <= jarFile.lastModified()) {
            System.out.println(theContext.getModuleName() + ".jar is up to date");
            return;
        }
        List args = new ArrayList();
        args.add("-cf");
        args.add(jarFile.getPath());
        args.add("-C");
        args.add(theContext.getClassesRoot().getPath());
        args.add(".");
        for (int i = 0; i < files.length; i++) {
            addFilesToJar(args, theContext.getSourceRoot(), files[i]);
        }
        System.out.println("jar: " + jarFile.getName());
        theContext.jar((String[]) args.toArray(new String[args.size()]));
    }
}
