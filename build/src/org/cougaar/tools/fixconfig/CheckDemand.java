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

public class CheckDemand {
    public static Fix.Checker[] getDemandCheckers() {
        String pi1 = "plugin = org.cougaar.mlm.plugin.organization.GLSExpanderPlugin";
        String pi2 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SubsistenceInventoryPlugin";
        String pi3 = "plugin = mil.darpa.log.alpine.blackjack.plugins.AntsInventoryPlugin";
        String pi4 = "plugin = mil.darpa.log.alpine.blackjack.plugins.MedicalInventoryPlugin";
        String pi5 = "plugin = mil.darpa.log.alpine.delta.plugin.DLARouterPlugin";
        Fix.Checker food =
            new Fix.CheckAny(Relationships.createRole("SubsistenceSupplyProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "ClassISubsistence"),
                                 new Fix.MatchParameter(pi2, "+SubsistenceInventory"),
                                 new Fix.MatchPlugin(pi5)
                             });
        Fix.Checker pol =
            new Fix.CheckAny(Relationships.createRole("FuelSupplyProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "BulkPOL"),
                                 new Fix.MatchParameter(pi3, "+BulkPOLInventory"),
                                 new Fix.MatchPlugin(pi5)
                             });
        Fix.Checker ammo =
            new Fix.CheckAny(Relationships.createRole("AmmunitionProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "Ammunition"),
                                 new Fix.MatchParameter(pi3, "+AmmunitionInventory"),
                                 new Fix.MatchPlugin(pi5)
                             });
        Fix.Checker medical =
            new Fix.CheckAny(Relationships.createRole("MedicalSupplyProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "ClassVIIIMedical"),
                                 new Fix.MatchParameter(pi4, "+MedicalInventory"),
                                 new Fix.MatchPlugin(pi5)
                             });
        Fix.Checker consumable =
            new Fix.CheckAny(Relationships.createRole("SparePartsProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "Consumable"),
                                 new Fix.MatchParameter(pi4, "+ConsumableInventory"),
                                 new Fix.MatchPlugin(pi5)
                             });
//          Fix.Checker strattrans =
//              new Fix.CheckParameter(Relationships.createRole("StrategicTransportationProvider"),
//                                     pi1, "StrategicTransportation");
        Fix.Checker[] result = {food, pol, ammo, medical, consumable/*, strattrans*/};
        return result;
    }
}
