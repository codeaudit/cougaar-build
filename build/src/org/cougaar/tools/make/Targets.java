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
        System.out.println(theContext.getModuleName() + ".javac: Compiling " + needed.length + " files");
	theContext.javac(needed);
    }
    private void cleanSome(File root, String suffix, boolean recurse) throws MakeException {
        if (root.isDirectory()) {
            File[] targets = theContext.findFiles(root, suffix, recurse);
            System.out.println(theContext.getModuleName() + ".delete " + root + ": " + targets.length + " files");
            theContext.delete(targets);
        }
    }

    public void clean() throws MakeException {
        cleanSome(theContext.getClassesRoot(), null, true);
        cleanSome(theContext.getGenCodeRoot(), null, true);
    }
        
    public void cleanDir() throws MakeException {
        cleanClassFiles(false);
    }

    public void cleanAll() throws MakeException {
        cleanClassFiles(true);
    }

    private void cleanClassFiles(boolean recurse) throws MakeException {
        String[] tails =
            theContext.getTails(theContext.getSourceRoot(),
                                new File[] {theContext.getCurrentDirectory()});
        File targetDir = new File(theContext.getClassesRoot(), tails[0]);
        cleanSome(targetDir, null, recurse);
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
                    System.out.println(theContext.getModuleName() + ".generateCode: " + defFile);
                    theContext.generateCode(defFile, genFile);
                }
            }
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
        String[] extensionsToJar = theContext.getExtensionsToJar();
        MakeContext.JarSet[] jarSets = new MakeContext.JarSet[1 + extensionsToJar.length];
        jarSets[0] = theContext.createJarSet(theContext.getClassesRoot());
        long maxTime =
            getMaxModificationTime(theContext.findFiles(theContext.getClassesRoot(), null, true));
        for (int i = 0; i < extensionsToJar.length; i++) {
            jarSets[1 + i] =
                theContext.createJarSet(theContext.getSourceRoot(), extensionsToJar[i], true);
            maxTime = Math.max(maxTime, getMaxModificationTime(jarSets[1 + i].files));
        }
        if (maxTime <= jarFile.lastModified()) {
            System.out.println(theContext.getModuleName() + ".jar is up to date");
            return;
        }
        System.out.println(theContext.getModuleName() + ".jar: " + jarFile.getName());
        theContext.jar(jarFile, null, jarSets);
    }

    public void projectTags() throws MakeException {
        File tagsFile = new File(theContext.getProjectRoot(), "TAGS");
        File[] modules = theContext.getAllModuleRoots();
        for (int i = 0; i < modules.length; i++) {
            File t = new File(theContext.getModuleTemp(modules[i]), "TAGS");
            if (!t.exists()) {
                theContext.makeTarget(modules[i].getName() + ".tags");
            }
            modules[i] = t;
        }
        theContext.etags(tagsFile, null, modules, false);
    }

    public void tags() throws MakeException {
        theContext.makeTarget("generateCode");
        File[] srcSources = theContext.findFiles(theContext.getSourceRoot(), ".java", true);
        File[] genSources = theContext.findFiles(theContext.getGenCodeRoot(), ".java", true);
        File tagsFile = new File(theContext.getModuleTemp(), "TAGS");
        long maxTime = Math.max(getMaxModificationTime(srcSources),
                                getMaxModificationTime(genSources));
        if (maxTime > tagsFile.lastModified()) {
            System.out.println(theContext.getModuleName() + ".tags: " + (srcSources.length + genSources.length) + " files");
            theContext.etags(tagsFile, srcSources, null, false);
            theContext.etags(tagsFile, genSources, null, true);
        } else {
            System.out.println(theContext.getModuleName() + ".tags is up to date");
        }
    }
}
