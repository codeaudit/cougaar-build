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

public class CheckDemand {
    public static Fix.Checker[] getDemandCheckers() {
        String pi1 = "plugin = org.cougaar.domain.mlm.plugin.organization.GLSExpanderPlugIn";
        String pi2 = "plugin = mil.darpa.log.alpine.blackjack.plugins.SubsistenceInventoryPlugIn";
        String pi3 = "plugin = mil.darpa.log.alpine.blackjack.plugins.AntsInventoryPlugIn";
        String pi4 = "plugin = mil.darpa.log.alpine.blackjack.plugins.MedicalInventoryPlugIn";
        String pi5 = "plugin = mil.darpa.log.alpine.delta.plugin.DLARouterPlugIn";
        Fix.Checker food =
            new Fix.CheckAny(Relationships.createRole("SubsistenceSupplyProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "ClassISubsistence"),
                                 new Fix.MatchParameter(pi2, "+SubsistenceInventory"),
                                 new Fix.MatchPlugIn(pi5)
                             });
        Fix.Checker pol =
            new Fix.CheckAny(Relationships.createRole("FuelSupplyProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "BulkPOL"),
                                 new Fix.MatchParameter(pi3, "+BulkPOLInventory"),
                                 new Fix.MatchPlugIn(pi5)
                             });
        Fix.Checker ammo =
            new Fix.CheckAny(Relationships.createRole("AmmunitionProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "Ammunition"),
                                 new Fix.MatchParameter(pi3, "+AmmunitionInventory"),
                                 new Fix.MatchPlugIn(pi5)
                             });
        Fix.Checker medical =
            new Fix.CheckAny(Relationships.createRole("MedicalSupplyProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "ClassVIIIMedical"),
                                 new Fix.MatchParameter(pi4, "+MedicalInventory"),
                                 new Fix.MatchPlugIn(pi5)
                             });
        Fix.Checker consumable =
            new Fix.CheckAny(Relationships.createRole("SparePartsProvider"),
                             new Fix.Matcher[] {
                                 new Fix.MatchParameter(pi1, "Consumable"),
                                 new Fix.MatchParameter(pi4, "+ConsumableInventory"),
                                 new Fix.MatchPlugIn(pi5)
                             });
//          Fix.Checker strattrans =
//              new Fix.CheckParameter(Relationships.createRole("StrategicTransportationProvider"),
//                                     pi1, "StrategicTransportation");
        Fix.Checker[] result = {food, pol, ammo, medical, consumable/*, strattrans*/};
        return result;
    }
}
