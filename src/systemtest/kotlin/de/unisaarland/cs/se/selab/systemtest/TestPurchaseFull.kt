package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * test typhoon
 */
class TestPurchaseFull : SystemTestExtension() {
    // short description of the test
    override val description = "Tests the purchase of a refuelingShip"
    override val name = "RefuelingShipPurchase"

    override val map = "mapFiles/shipPurchaseFull.json"
    override val corporations = "corporationFiles/corporationsForPurchase.json"
    override val scenario = "scenarioFiles/scenario.json"

    override val maxTicks = 5
    override suspend fun run() {
        skipLines(10)
        assertNextLine("Purchase: Ship 1 ordered a refueling ship with id 6 at harbor 0 for 1231 credits.")
        skipLines(26)
        assertNextLine("Purchase: Ship 6 delivered to corporation 0 at 7.")
    }
}
