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

import java.util.HashMap;
import java.util.Map;

/**
 * This class defines sets of Fixers for removing various aspects of a society.
 **/
public class Remove {
    private static String pi1 = "plugin = org.cougaar.domain.mlm.plugin.organization.GLSExpanderPlugIn";
    private static String pi2 = "plugin = mil.darpa.log.alpine.blackjack.plugins.AntsInventoryPlugIn";
    private static String pi3 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SupplyProjectionPlugIn";
    private static String pi4 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SubsistenceInventoryPlugIn";

    public static Fix.Fixer[] ammo = {
        new Fix.DeleteParameter(pi1, "Ammunition", true),
        new Fix.DeleteParameter(pi2, "+AmmunitionInventory"),
        new Fix.DeleteParameter(pi3, "Ammunition")
    };

    public static Fix.Fixer[] construction = {
        new Fix.PlugIn("plugin = org.cougaar.domain.mlm.construction.DT"),
        new Fix.DeleteParameter(pi1, "Construction", true),
    };

    public static Fix.Fixer[] food = {
        new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Subsistence"),
        new Fix.DeleteParameter(pi1, "ClassISubsistence", true),
    };

    public static Fix.Fixer[] consumable = {
        new Fix.DeleteParameter(pi1, "Consumable", true),
        new Fix.DeleteParameter(pi2, "+ConsumableInventory"),
        new Fix.DeleteParameter(pi3, "Consumable"),
    };

    public static Fix.Fixer[] pol = {
        new Fix.DeleteParameter(pi1, "BulkPOL", true),
        new Fix.DeleteParameter(pi1, "PackagedPOL", true),
        new Fix.DeleteParameter(pi2, "+BulkPOLInventory"),
        new Fix.DeleteParameter(pi2, "+PackagedPOLInventory"),
        new Fix.DeleteParameter(pi2, "+FuelsSourcingAllocator"),
        new Fix.DeleteParameter(pi2, "+PackagedPOLTransport"),
        new Fix.DeleteParameter(pi2, "+BulkPOLTransport"),
        new Fix.DeleteParameter(pi3, "BulkPOL"),
        new Fix.DeleteParameter(pi3, "PackagedPOL"),
    };

    public static Fix.Fixer[] medical = {
        new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Medical"),
        new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Patient"),
        new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.Treatment"),
        new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.UniversalAllocatorPlugIn(TREAT_PATIENT)"),
        new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.MedicalInventoryPlugIn"),
        new Fix.DeleteParameter(pi1, "ClassVIIIMedical", true),
    };

    public static Fix.Fixer[] sra = {
        new Fix.PlugIn("plugin = org.cougaar.domain.css."),
    };

    public static Fix.Fixer[] transport = {
        new Fix.PlugIn("plugin = org.cougaar.domain.mlm.plugin.sample.StrategicTransportProjectorPlugIn"),
        new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportExpanderPlugIn"),
        new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAggregatorPlugIn"),
        new Fix.PlugIn("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAllocatorPlugIn"),
        new Fix.DeleteParameter(pi1, "StrategicTransportation", true),
        new Fix.DeleteParameter(pi2, "+PackagedPOLTransport"),
        new Fix.DeleteParameter(pi4, "+ClassISubsistenceTransport"),
        new Fix.DeleteParameter(pi2, "+BulkPOLTransport"),
    };

    private static Map fixerMap = new HashMap();

    static {
        fixerMap.put("ammo", ammo);
        fixerMap.put("construction", construction);
        fixerMap.put("consumable", consumable);
        fixerMap.put("food", food);
        fixerMap.put("medical", medical);
        fixerMap.put("pol", pol);
        fixerMap.put("sra", sra);
        fixerMap.put("transport", transport);
    }

    public static Fix.Fixer[] getFixers(String option) {
        return (Fix.Fixer[]) fixerMap.get(option);
    }
}
