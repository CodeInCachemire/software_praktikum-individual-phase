package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * test typhoon
 */
class TestsPurchase : SystemTestExtension() {
    // short description of the test
    override val description = "Tests the purchase of a refuelingShip"
    override val name = "RefuelingShipPurchase"

    override val map = "mapFiles/shipPurchase.json"
    override val corporations = "corporationFiles/corporationsForPurchase.json"
    override val scenario = "scenarioFiles/scenario.json"

    override val maxTicks = 5
    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.PURCHASE, LoggingUtils.purchaseRefueling(1, 6, 0, 1231))
        // the next line should be this:
        skipUntilAndAssert(LogType.PURCHASE, LoggingUtils.purchaseRefueling(5, 7, 3, 1230))
        skipUntilAndAssert(LogType.PURCHASE, LoggingUtils.deliveredRefueling(6, 0, 7))
    }
}
