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
    public static final String[] CLUSTERS = {
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

    public FixFood(String[] clusters) {
        System.out.println("[ Clusters ]");
        files = new File[clusters.length];
        for (int i = 0; i < clusters.length; i++) {
            files[i] = new File(clusters[i] + ".ini");
            System.out.println("cluster = " + clusters[i]);
        }
    }

    protected File[] getFiles() {
        return files;
    }


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
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportExpanderPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAggregatorPlugIn"),
            new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAllocatorPlugIn"),
            new Fix.DeleteParameter(pi1, "StrategicTransportation", true),
            new Fix.DeleteParameter(pi1, "Consumable", true),
            new Fix.DeleteParameter(pi1, "BulkPOL", true),
            new Fix.DeleteParameter(pi1, "PackagedPOL", true),
            new Fix.DeleteParameter(pi1, "ClassVIIIMedical", true),
            new Fix.DeleteParameter(pi2, "+BulkPOLInventory"),
            new Fix.DeleteParameter(pi2, "+PackagedPOLInventory"),
            new Fix.DeleteParameter(pi2, "+ConsumableInventory"),
            new Fix.DeleteParameter(pi2, "+FuelsSourcingAllocator"),
            new Fix.DeleteParameter(pi2, "+PackagedPOLTransport"),
            new Fix.DeleteParameter(pi2, "+ClassISubsistenceTransport"),
            new Fix.DeleteParameter(pi2, "+BulkPOLTransport"),
            new Fix.DeleteParameter(pi3, "BulkPOL"),
            new Fix.DeleteParameter(pi3, "PackagedPOL"),
            new Fix.DeleteParameter(pi3, "Consumable"),
            new Fix.DeleteParameter(pi3, "Ammunition"),
        };
    }

    public static void main(String[] args) {
        new FixFood(args.length > 0 ? args : CLUSTERS).fix();
    }
}
