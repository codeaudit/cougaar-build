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
            File moduleDirectory = theContext.getModuleRoot(moduleName);
            if (moduleDirectory.isDirectory()) {
                String tgt = moduleName + ".jar";
                if (theContext.isNoPrerequisites()) {
                    if (new File(theContext.getProjectLib(), tgt).exists()) continue;
                }
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
        File[] sources = theContext.findFiles(srcDirectory, ".java", recurse, false);
        compileCommon(theContext.getSourceRoot(), sources);
    }

    public void compileGenCode() throws MakeException {
        theContext.makeTarget("generateCode");
        File srcRoot = theContext.getGenCodeRoot();
        if (srcRoot.isDirectory()) {
            File[] sources = theContext.findFiles(srcRoot, ".java", true, false);
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
    private void cleanDirectory(File dir, String suffix, boolean recurse, boolean includeDirectories)
	throws MakeException
    {
        if (dir.isDirectory()) {
            File[] targets = theContext.findFiles(dir, suffix, recurse, includeDirectories);
            System.out.println(theContext.getModuleName()
			       + ".delete "
			       + dir
			       + ": "
			       + targets.length
			       + " files");
            theContext.delete(targets);
        }
    }

    public void clean() throws MakeException {
        cleanDirectory(theContext.getModuleTemp(), null, true, true);
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
        cleanDirectory(targetDir, null, recurse, false);
    }

    public void generateCode() throws MakeException {
        compilePrerequisites();
	File[] sources = theContext.findFiles(theContext.getSourceRoot(), ".def", true, false);
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
	File[] classFiles = theContext.findFiles(theContext.getClassesRoot(), null, true, false);
	int fileCount = classFiles.length;
        long maxTime =
            getMaxModificationTime(classFiles);
        for (int i = 0; i < extensionsToJar.length; i++) {
            jarSets[1 + i] =
                theContext.createJarSet(theContext.getSourceRoot(), extensionsToJar[i], true);
            maxTime = Math.max(maxTime, getMaxModificationTime(jarSets[1 + i].files));
	    fileCount += jarSets[1 + i].files.length;
        }
        if (maxTime <= jarFile.lastModified()) {
            System.out.println(theContext.getModuleName() + ".jar is up to date");
            return;
        }
        System.out.println(theContext.getModuleName() + ".jar: " + fileCount + " files");
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
	theContext.insureDirectory(theContext.getModuleTemp());
        theContext.makeTarget("generateCode");
        File[] srcSources = theContext.findFiles(theContext.getSourceRoot(), ".java", true, false);
        File[] genSources = theContext.findFiles(theContext.getGenCodeRoot(), ".java", true, false);
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
