package org.cougaar.tools.fixconfig;

import java.io.*;

/**
 * This program edits a set of cluster ini files to remove all but
 * what is needed to run Class VIII Medical. The set of clusters is
 * either a built-in set (see CLUSTERS variable) or a set of clusters
 * specified on the command line. In all cases, the contents of the
 * node ini file are written to stdout.
 *
 * Note:  You must ensure that FixMedical is called from Fix.java,
 *        with the form:  new FixMedical(clusterNames).fix();
 *        in the method:  fix(ClusterOptionSet[] options,
 *                        String[] args,
 *                        String[] roles)
 *
 *        The repository version of "Fix.java" contains a call only for
 *        Subsistence (ie, FixFood(clusterNames).fix();
 **/

public class FixMedical extends Fix {
    /**
     * This cluster list includes two battalions and _only_ medical. No transportation
     **/
    public static final String[] SHORT_CLUSTERS = {
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
        "21-MED-CSHOSP",
        "147-MEDLOGBN",
        "10-TCBN-HHC",
        "89-TKCO-CGO",
        "126-MEDTM-FSURG",
        "61-ASMEDBN-MEDCO",
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
        "21-MED-CSHOSP",
        "147-MEDLOGBN",
        "10-TCBN-HHC",
        "89-TKCO-CGO",
        "126-MEDTM-FSURG",
        "61-ASMEDBN-MEDCO",
    };

    protected Fixer[] getFixes() {

        String pi1 = "plugin = org.cougaar.domain.mlm.plugin.organization.GLSExpanderPlugIn";
        String pi2 = "plugin = mil.darpa.log.alpine.blackjack.plugins.AntsInventoryPlugIn";
        String pi3 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SupplyProjectionPlugIn";
        String pi4 = "plugin = mil.darpa.log.alpine.blackjack.plugins.MedicalInventoryPlugIn";
        return new Fixer[] {
            new Fix.PlugIn("plugin = org.cougaar.domain.css."),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Subsistence"),
            // new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.UniversalAllocatorPlugIn(TREAT_PATIENT)"),
            new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.SubsistenceInventoryPlugIn"),
            new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.StrategicTransportProjectorPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.LimitResourcesPolicyPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportExpanderPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAggregatorPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAllocatorPlugIn"),
            new Fix.DeleteParameter(pi1, "StrategicTransportation", true),
            new Fix.DeleteParameter(pi1, "Consumable", true),
            new Fix.DeleteParameter(pi1, "BulkPOL", true),
            new Fix.DeleteParameter(pi1, "PackagedPOL", true),
            new Fix.DeleteParameter(pi1, "ClassISubsistence", true),
            new Fix.DeleteParameter(pi2, "+BulkPOLInventory"),
            new Fix.DeleteParameter(pi2, "+PackagedPOLInventory"),
            new Fix.DeleteParameter(pi2, "+ConsumableInventory"),
            new Fix.DeleteParameter(pi2, "+FuelsSourcingAllocator"),
            new Fix.DeleteParameter(pi2, "+PackagedPOLTransport"),
            new Fix.DeleteParameter(pi4, "+ClassISubsistenceTransport"),
            new Fix.DeleteParameter(pi2, "+BulkPOLTransport"),
            new Fix.DeleteParameter(pi3, "BulkPOL"),
            new Fix.DeleteParameter(pi3, "PackagedPOL"),
            new Fix.DeleteParameter(pi3, "Consumable"),
            new Fix.DeleteParameter(pi3, "Ammunition"),
        };
    }

    private static ClusterOptionSet[] clusterSetOptions = {
        new ClusterOptionSet("-short", SHORT_CLUSTERS),
        new ClusterOptionSet("-long", LONG_CLUSTERS),
    };

    public static void main(String[] args) {
        String[] roles = {"MedicalSupplyProvider","Level1HealthCareProvider","Level2HealthCareProvider","Level3HealthCareProvider"};
        fix(clusterSetOptions, args, roles, new FixMedical());
    }
}