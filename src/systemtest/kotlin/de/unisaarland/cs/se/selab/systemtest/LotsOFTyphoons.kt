package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * test typhoon
 */
class LotsOFTyphoons : SystemTestExtension() {
    // short description of the test
    override val description = "Test if ship repairs after typhoon."
    override val name = "Repair post typhoon check."

    override val map = "mapFiles/smallMap.json"
    override val corporations = "corporationFiles/lotsOfTyphoon.json"
    override val scenario = "scenarioFiles/lotsOfTyphoons.json"

    override val maxTicks = 3
    override suspend fun run() {
        skipUntilAndAssert(LogType.SIMULATION_INFO, "Simulation Info: Simulation started.")
        skipUntilAndAssert(LogType.SIMULATION_INFO, LoggingUtils.logTickStart(0))
        skipUntilAndAssert(LogType.SHIPMOVE, "Ship Movement: Ship 1 moved with speed 25 to tile 4.")
        skipUntilAndAssert(LogType.SIMULATION_INFO, LoggingUtils.logTickStart(1))
        skipUntilAndAssert(LogType.CORPORATION_ACTION, LoggingUtils.logStartMove(0))
        skipUntilAndAssert(LogType.CORPORATION_ACTION, LoggingUtils.logCorpCollectGarbage(0))
    }
}
