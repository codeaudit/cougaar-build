/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.tools.build;

import java.io.File;

public class WriterBase {

    protected String targetDirName = null;
    private File targetDir = null;
    private File sourceDir = null;
    private String genFileName = "generated.gen";
    protected File getTargetDir() {
	return targetDir;
    }
    protected File getSourceDir() {
	return sourceDir;
    }
    protected String getGenFileName() {
        return genFileName;
    }

    protected void setDirectories(String defFileName) {
        if (defFileName.equals("-")) {
            sourceDir = new File(System.getProperty("user.dir"));
        } else {
            File defFile = new File(defFileName);
            sourceDir = defFile.getParentFile();
            String s = defFile.getName();
            int dotPos = s.lastIndexOf('.');
            if (dotPos < 0) {
                genFileName = s + ".gen";
            } else {
                genFileName = s.substring(0, dotPos) + ".gen";
            }
        }
	if (targetDirName != null) {
	    targetDir = new File(targetDirName);
	} else {
	    targetDir = sourceDir;
	}
    }
}
