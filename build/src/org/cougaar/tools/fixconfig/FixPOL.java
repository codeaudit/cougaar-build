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
package org.cougaar.tools.fixconfig;

import java.io.*;

/**
 * This program edits a set of cluster ini files to remove all but
 * what is needed to run class III POL. The set of clusters is
 * either a built-in set (see CLUSTERS variable) or a set of clusters
 * specified on the command line. In all cases, the contents of the
 * node ini file are written to stdout.
 **/
public class FixPOL extends Fix {
    /**
     * This cluster list includes three battalions and _only_ fuel. No transportation
     **/
    public static final String[] SHORT_CLUSTERS = {
        "2-7-INFBN",
        "3-69-ARBN",
        "1-37-FABN",
        "1-23-INFBN"
    };
    public static final String[] LONG_CLUSTERS = {
        "1-23-INFBN",
        "1-37-FABN",
        "18-ENGCO",
        "2-3-INFBN",
        "2-7-INFBN",
        "3-69-ARBN",
        "334-SIGCO",
        "5-20-INFBN",
        "52-INF-CCO",
    };

    protected Fixer[] getFixes() {
        String pi1 = "plugin = org.cougaar.mlm.plugin.organization.GLSExpanderPlugin";
        String pi2 = "plugin = mil.darpa.log.alpine.blackjack.plugins.AntsInventoryPlugin";
        String pi3 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SupplyProjectionPlugin";
        return new Fixer[] {
            new Fix.Plugin("plugin = org.cougaar.css."),
            new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Medical"),
            new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Patient"),
            new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Treatment"),
            new Fix.Plugin("plugin = org.cougaar.mlm.plugin.sample.UniversalAllocatorPlugin(TREAT_PATIENT)"),
            new Fix.Plugin("plugin = org.cougaar.mlm.plugin.sample.MedicalInventoryPlugin"),
            new Fix.Plugin("plugin = org.cougaar.mlm.plugin.sample.StrategicTransportProjectorPlugin"),
            new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.LimitResourcesPolicyPlugin"),
            new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Transport"),
            new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Subsistence"),
            new Fix.Plugin("plugin = org.cougaar.mlm.construction.DT"),
            new Fix.Plugin("plugin = org.cougaar.css.subsistence."),
            new Fix.Plugin("plugin = org.cougaar.css.policy."),
            new Fix.DeleteParameter(pi1, "StrategicTransportation", true),
            new Fix.DeleteParameter(pi1, "Consumable", true),
            new Fix.DeleteParameter(pi1, "ClassVIIIMedical", true),
            new Fix.DeleteParameter(pi1, "ClassISubsistence", true),
            new Fix.DeleteParameter(pi1, "PackagedPOL", true),
            new Fix.DeleteParameter(pi2, "+ConsumableInventory"),
            new Fix.DeleteParameter(pi2, "+PackagedPOLInventory"),
            new Fix.DeleteParameter(pi2, "+BulkPOLTransport"),
            new Fix.DeleteParameter(pi3, "Consumable"),
            new Fix.DeleteParameter(pi3, "Ammunition"),
        };
    }

    private static ClusterOptionSet[] clusterSetOptions = {
        new ClusterOptionSet("-short", SHORT_CLUSTERS),
        new ClusterOptionSet("-long", LONG_CLUSTERS),
    };

    public static void main(String[] args) {
        String[] roles = {"FuelSupplyProvider"};
        fix(clusterSetOptions, args, roles, new FixPOL());
    }
}
