/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.tools.fixconfig;

import java.io.*;

/**
 * This program edits a set of cluster ini files to remove all but
 * what is needed to run class I subsistence. The set of clusters is
 * either a built-in set (see CLUSTERS variable) or a set of clusters
 * specified on the command line. In all cases, the contents of the
 * node ini file are written to stdout.
 **/
public class FixFood extends Fix {
    /**
     * This cluster list includes two battalions and _only_
     * subsistence. No transportation
     **/
    public static final String[] SHORT_CLUSTERS = {
        "NCA",
        "CENTCOM-HHC",
        "JTF-HHC",
        "3ID-HHC",
        "2-BDE-3ID-HHC",
        "2-7-INFBN",
        "3-69-ARBN",
        "3-FSB",
        "703-MSB",
        "553-CSB-HHD",
        "10-TCBN-HHC",
        "DLAHQ",
        "SubsistenceICP",
    };
    public static final String[] LONG_CLUSTERS = {
        "3-DISCOM-HHC",
        "NCA",
        "2-7-INFBN",
        "3-BDE-2ID-HHC",
        "2-BDE-3ID-HHC",
        "3-69-ARBN",
        "3-FSB",
        "3ID-HHC",
        "703-MSB",
        "CENTCOM-HHC",
        "DLAHQ",
        "JTF-HHC",
        "TRANSCOM",
        "296-SPTBN",
        "1-23-INFBN",
        "24-SPTGP-HHC",
        "24-CSB-HHD",
        "553-CSB-HHD",
        "10-TCBN-HHC",
        "89-TKCO-CGO",
        "SubsistenceICP",
    };

    private File[] files;

    protected Fixer[] getFixes() {
        return concatenateFixers(new Fixer[][] {
            Remove.ammo, Remove.pol, Remove.construction, Remove.sra, Remove.transport});
    }

    private static ClusterOptionSet[] clusterSetOptions = {
        new ClusterOptionSet("-short", SHORT_CLUSTERS),
        new ClusterOptionSet("-long", LONG_CLUSTERS),
    };

    public static void main(String[] args) {
        String[] roles = {"SubsistenceSupplyProvider"};
        fix(clusterSetOptions, args, roles, new FixFood());
    }
}
