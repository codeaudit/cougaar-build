/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.util.HashMap;
import java.util.Map;

/**
 * This class defines sets of Fixers for removing various aspects of a society.
 **/
public class Remove {
    private static String pi1 = "plugin = org.cougaar.mlm.plugin.organization.GLSExpanderPlugin";
    private static String pi2 = "plugin = mil.darpa.log.alpine.blackjack.plugins.AntsInventoryPlugin";
    private static String pi3 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SupplyProjectionPlugin";
    private static String pi4 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SubsistenceInventoryPlugin";

    public static Fix.Fixer[] ammo = {
        new Fix.DeleteParameter(pi1, "Ammunition", true),
        new Fix.DeleteParameter(pi2, "+AmmunitionInventory"),
        new Fix.DeleteParameter(pi3, "Ammunition")
    };

    public static Fix.Fixer[] construction = {
        new Fix.Plugin("plugin = org.cougaar.mlm.construction.DT"),
        new Fix.DeleteParameter(pi1, "Construction", true),
    };

    public static Fix.Fixer[] food = {
        new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Subsistence"),
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
        new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Medical"),
        new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Patient"),
        new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.Treatment"),
        new Fix.Plugin("plugin = org.cougaar.mlm.plugin.sample.UniversalAllocatorPlugin(TREAT_PATIENT)"),
        new Fix.Plugin("plugin = org.cougaar.mlm.plugin.sample.MedicalInventoryPlugin"),
        new Fix.DeleteParameter(pi1, "ClassVIIIMedical", true),
    };

    public static Fix.Fixer[] sra = {
        new Fix.Plugin("plugin = org.cougaar.css."),
    };

    public static Fix.Fixer[] transport = {
        new Fix.Plugin("plugin = org.cougaar.mlm.plugin.sample.StrategicTransportProjectorPlugin"),
        new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportExpanderPlugin"),
        new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAggregatorPlugin"),
        new Fix.Plugin("plugin = mil.darpa.log.alpine.blackjack.plugins.TransportAllocatorPlugin"),
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
