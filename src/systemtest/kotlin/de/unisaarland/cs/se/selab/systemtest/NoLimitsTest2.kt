package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * This is to test if a refueling station adheres to the limit.
 */
class NoLimitsTest2 : SystemTestExtension() {
    // short description of the test
    override val description = "Test station brosef"

    // name of the test
    override val name = "Test station brosef"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/bigCorporationRefuels.json"
    override val corporations = "corporationFiles/corporationScout.json"
    override val scenario = "scenarioFiles/nothing.json"

    // max ticks parameter for the test execution
    override val maxTicks = 10
    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.REFUELING, LoggingUtils.logRefueling(1, 0, 450))
        skipUntilAndAssert(LogType.REFUELING, LoggingUtils.logRefueling(2, 0, 450))
        skipUntilAndAssert(LogType.REFUELING, LoggingUtils.logRefueling(3, 0, 450))
        skipUntilAndAssert(LogType.REFUELING, LoggingUtils.logRefueling(4, 0, 450))
        skipUntilAndAssert(LogType.REFUELING, LoggingUtils.logRefueling(5, 0, 450))
        skipUntilAndAssert(LogType.REFUELING, LoggingUtils.logRefueling(6, 0, 450))
        skipUntilAndAssert(LogType.REFUELING, "Refueling: Ship 1 refueled at harbor 0.")
        skipUntilAndAssert(LogType.REFUELING, "Refueling: Ship 2 refueled at harbor 0.")
        skipUntilAndAssert(LogType.REFUELING, "Refueling: Ship 3 refueled at harbor 0.")
        skipUntilAndAssert(LogType.REFUELING, "Refueling: Ship 4 refueled at harbor 0.")
        skipUntilAndAssert(LogType.REFUELING, "Refueling: Ship 5 refueled at harbor 0.")
        assertNextLine("Corporation Action: Corporation 0 finished its actions.")
    }
}
