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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

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

    public void projectBin() throws MakeException {
	theContext.insureDirectory(theContext.getProjectBin());
    }

    public void moduleClasses() throws MakeException {
	theContext.insureDirectory(theContext.getClassesRoot());

    }

    public void moduleGenCode() throws MakeException {
	theContext.insureDirectory(theContext.getGenCodeRoot());
    }

    /**
       Compile all the prerequisite modules
     **/
    private void compilePrerequisites() throws MakeException {
        String[] prerequisiteModules =
            theContext.getPrerequisites(theContext.getModuleName());
        for (int i = 0; i < prerequisiteModules.length; i++) {
            String moduleName = prerequisiteModules[i];
            File moduleDirectory = theContext.getModuleRoot(moduleName);
            if (moduleDirectory.isDirectory()) {
                String tgt = moduleName + ".compile";
                if (theContext.isNoPrerequisites()) continue;
                theContext.makeTarget(tgt);
            }
        }
    }

    /**
     * Compile the sources in the current directory that are
     * out-of-date w.r.t. their class file.
     **/
    public void compileDir() throws MakeException {
        compileSome(theContext.getCurrentDirectory(), false);
    }
    public void compileAll() throws MakeException {
        compileSome(theContext.getCurrentDirectory(), true);
    }
    public void compile() throws MakeException {
        compileSome(theContext.getSourceRoot(), true);
    }
    public void all_recompile() throws MakeException {
        theContext.makeTarget("all.cleanGenCode");
        theContext.makeTarget("all.cleanClassFiles");
        theContext.makeTarget("all.compile");
    }
    public void recompile() throws MakeException {
        theContext.makeTarget("cleanGenCode");
        theContext.makeTarget("cleanClassFiles");
        theContext.makeTarget("compile");
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
        System.out.println(theContext.getModuleName()
                           + ".javac"
                           + MakeContext.COLON
                           + "Compiling "
                           + needed.length
                           + " files");
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
			       + MakeContext.COLON
			       + targets.length
			       + " files");
            theContext.delete(targets);
        }
    }

    public void clean() throws MakeException {
        getModuleJarFile().delete();
        cleanDirectory(theContext.getModuleTemp(), null, true, true);
    }
        
    public void cleanDir() throws MakeException {
        cleanDirClassFiles(false);
    }

    public void cleanAll() throws MakeException {
        cleanDirClassFiles(true);
    }

    private void cleanDirClassFiles(boolean recurse) throws MakeException {
        String[] tails =
            theContext.getTails(theContext.getSourceRoot(),
                                new File[] {theContext.getCurrentDirectory()});
        File targetDir = new File(theContext.getClassesRoot(), tails[0]);
        cleanDirectory(targetDir, null, recurse, false);
    }

    public void cleanClassFiles() throws MakeException {
        cleanDirectory(theContext.getClassesRoot(), null, true, true);
    }

    public void cleanGenCode() throws MakeException {
        cleanDirectory(theContext.getGenCodeRoot(), null, true, true);
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
                    System.out.println(theContext.getModuleName() + ".generateCode" + MakeContext.COLON + defFile);
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

    private File getModuleJarFile() {
        return new File(theContext.getProjectLib(), theContext.getModuleName() + ".jar");
    }

    public void jar() throws MakeException {
        theContext.makeTarget("projectLib");
        theContext.makeTarget("rmic");
        File jarFile = getModuleJarFile();
        MakeContext.JarSet[] jarSets =
            getJarSets(theContext.getClassesRoot(), theContext.getSourceRoot(), null, null);
        jarCommon(jarFile, null, jarSets);
    }

    private MakeContext.JarSet[] getJarSets(File classesRoot, File sourceRoot,
                                            File sourceDir,
                                            MakeContext.JarSet[] others)
        throws MakeException
    {
        String[] extensionsToJar = theContext.getExtensionsToJar();
        int thisSize = 1 + extensionsToJar.length;
        int totalSize = thisSize;
        if (others != null) totalSize += others.length;
        MakeContext.JarSet[] jarSets = new MakeContext.JarSet[totalSize];
        if (sourceDir == null) {
            jarSets[0] = theContext.createJarSet(classesRoot);
        } else {
            File classesDir =
                theContext.reroot(sourceDir,
                                  sourceRoot,
                                  classesRoot,
                                  null);
            System.out.println("classesDir=" + classesDir);
            jarSets[0] = theContext.createJarSet(classesRoot, classesDir, ".class");
        }
        for (int i = 0; i < extensionsToJar.length; i++) {
            jarSets[1 + i] =
                theContext.createJarSet(sourceRoot, sourceDir, extensionsToJar[i]);
        }
        if (others != null) {
            System.arraycopy(others, 0, jarSets, thisSize, others.length);
        }
        return jarSets;
    }

    private void jarCommon(File jarFile, File manifestFile,
                           MakeContext.JarSet[] jarSets)
        throws MakeException
    {
        long maxTime = Long.MIN_VALUE;
        int fileCount = 0;
        for (int i = 0; i < jarSets.length; i++) {
            maxTime = Math.max(maxTime, jarSets[i].getMaxModificationTime());
            fileCount += jarSets[i].getFileCount();
        }
        if (manifestFile != null) {
            maxTime = Math.max(maxTime, manifestFile.lastModified());
        }
        if (maxTime <= jarFile.lastModified()) {
            System.out.println(jarFile.getName() + " is up to date");
            return;
        }
        System.out.println(jarFile.getName() + MakeContext.COLON + fileCount + " files");
        theContext.jar(jarFile, manifestFile, jarSets);
    }

    /**
     * Make an executable jar from all ejm files in a module
     **/
    public void jax() throws MakeException {
        theContext.makeTarget("projectBin");
        theContext.makeTarget("compile");
        // Find all the .ejm (executable jar manifest) files
        jaxSome(theContext.findFiles(theContext.getSourceRoot(), ".ejm", true, false));
    }

    /**
     * Make an executable jars from ejm files in a directory
     **/
    public void jaxDir() throws MakeException {
        theContext.makeTarget("projectBin");
        theContext.makeTarget("compileDir");
        // Find all the .ejm (executable jar manifest) files
        jaxSome(theContext.findFiles(theContext.getCurrentDirectory(), ".ejm", false, false));
    }

    private void jaxSome(File[] manifests) throws MakeException {
        String[] prerequisiteModules =
            theContext.getPrerequisites(theContext.getModuleName());
        MakeContext.JarSet[] jarSets = null;
        for (int i = 0; i < prerequisiteModules.length; i++) {
            String moduleName = prerequisiteModules[i];
            File moduleDirectory = theContext.getModuleRoot(moduleName);
            if (moduleDirectory.isDirectory()) {
                jarSets = getJarSets(theContext.getClassesRoot(moduleDirectory),
                                     theContext.getSourceRoot(moduleDirectory),
                                     null, jarSets);
            }
        }
        for (int i = 0; i < manifests.length; i++) {
            File mf = manifests[i];
            File jarFile =
                new File(theContext.getProjectBin(),
                         replaceExtension(mf.getName(), ".jax"));
            File sourceDir = mf.getParentFile();
            jarCommon(jarFile, mf,
                      getJarSets(theContext.getClassesRoot(),
                                 theContext.getSourceRoot(),
                                 sourceDir,
                                 jarSets));
        }
    }

    public static String replaceExtension(String name, String newExtension) {
        int pos = name.lastIndexOf('.');
        if (pos < 0) {
            return name + newExtension;
        } else {
            return name.substring(0, pos) + newExtension;
        }
    }

    public void projectTags() throws MakeException {
        theContext.makeTarget("all.tags");
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
            System.out.println(theContext.getModuleName()
                               + ".tags"
                               + MakeContext.COLON
                               + (srcSources.length + genSources.length)
                               + " files");
            theContext.etags(tagsFile, srcSources, null, false);
            theContext.etags(tagsFile, genSources, null, true);
        } else {
            System.out.println(theContext.getModuleName() + ".tags is up to date");
        }
    }

    private void readRMICFile(Set classes, File rmicFile)
        throws MakeException
    {
        try {
            BufferedReader reader =
                new BufferedReader(new FileReader(rmicFile));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#")) continue;
                    if (line.startsWith("//")) continue;
                    classes.add(line);
                }
            } finally {
                reader.close();
            }
        } catch (IOException ioe) {
            throw new MakeException(ioe.toString());
        }
    }

    public void rmic() throws MakeException {
        theContext.makeTarget("compile");
        rmicSome(theContext.getSourceRoot(), true);
    }

    public void rmicDir() throws MakeException {
        theContext.makeTarget("compileDir");
        rmicSome(theContext.getCurrentDirectory(), false);
    }

    private void rmicSome(File srcDirectory, boolean recurse)
        throws MakeException
    {
        File[] rmicFiles =
            theContext.findFiles(srcDirectory, ".rmic", recurse, false);
        Set classNames = new TreeSet();
        for (int i = 0; i < rmicFiles.length; i++) {
            readRMICFile(classNames, rmicFiles[i]);
        }
        if (classNames.size() > 0) {
            theContext.rmic((String[]) classNames.toArray(new String[0]));
        }
    }
}
