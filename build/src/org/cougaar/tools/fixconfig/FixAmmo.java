package org.cougaar.tools.fixconfig;

import java.io.*;

/**
 * This program edits a set of cluster ini files to remove all but
 * what is needed to run class V ammo. The set of clusters is
 * either a built-in set (see CLUSTERS variable) or a set of clusters
 * specified on the command line. In all cases, the contents of the
 * node ini file are written to stdout.
 **/
public class FixAmmo extends Fix {
    /**
     * This cluster list includes three battalions and _only_ ammunition. No transportation
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
        String pi1 = "plugin = org.cougaar.domain.mlm.plugin.organization.GLSExpanderPlugIn";
        String pi2 = "plugin = mil.darpa.log.alpine.blackjack.plugins.AntsInventoryPlugIn";
        String pi3 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SupplyProjectionPlugIn";
        return new Fixer[] {
            new Fix.PlugIn("plugin = org.cougaar.domain.css."),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Medical"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Patient"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Treatment"),
            new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.UniversalAllocatorPlugIn(TREAT_PATIENT)"),
            new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.MedicalInventoryPlugIn"),
            new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.StrategicTransportProjectorPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.LimitResourcesPolicyPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Transport"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Subsistence"),
            new Fix.PlugIn("plugin = org.cougaar.domain.mlm.construction.DT"),
            new Fix.DeleteParameter(pi1, "StrategicTransportation", true),
            new Fix.DeleteParameter(pi1, "Consumable", true),
            new Fix.DeleteParameter(pi1, "BulkPOL", true),
            new Fix.DeleteParameter(pi1, "PackagedPOL", true),
            new Fix.DeleteParameter(pi1, "ClassVIIIMedical", true),
            new Fix.DeleteParameter(pi1, "ClassISubsistence", true),
            new Fix.DeleteParameter(pi2, "+BulkPOLInventory"),
            new Fix.DeleteParameter(pi2, "+PackagedPOLInventory"),
            new Fix.DeleteParameter(pi2, "+ConsumableInventory"),
            new Fix.DeleteParameter(pi2, "+FuelsSourcingAllocator"),
            new Fix.DeleteParameter(pi2, "+PackagedPOLTransport"),
            new Fix.DeleteParameter(pi2, "+BulkPOLTransport"),
            new Fix.DeleteParameter(pi3, "BulkPOL"),
            new Fix.DeleteParameter(pi3, "PackagedPOL"),
            new Fix.DeleteParameter(pi3, "Consumable"),
//              new Fix.DeleteParameter(pi3, "Ammunition"),
        };
    }

    private static ClusterOptionSet[] clusterSetOptions = {
        new ClusterOptionSet("-short", SHORT_CLUSTERS),
        new ClusterOptionSet("-long", LONG_CLUSTERS),
    };

    public static void main(String[] args) {
        String[] roles = {"AmmunitionProvider"};
        fix(clusterSetOptions, args, roles, new FixAmmo());
    }
}