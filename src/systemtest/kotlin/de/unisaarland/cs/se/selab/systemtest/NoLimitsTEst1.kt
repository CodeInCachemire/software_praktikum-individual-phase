package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * This is to test if a refueling station adheres to the limit.
 */
class NoLimitsTEst1 : SystemTestExtension() {
    // short description of the test
    override val description = "TEST 1 NOLIMITS"

    // name of the test
    override val name = "hellobrozer"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/bigCorporationRefuels.json"
    override val corporations = "corporationFiles/corporationScout.json"
    override val scenario = "scenarioFiles/nothing.json"

    // max ticks parameter for the test execution
    override val maxTicks = 10
    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.SHIPMOVE, "Ship 1 moved with speed 15 to tile 488.")
        assertNextLine("Ship Movement: Ship 2 moved with speed 15 to tile 488.")
        assertNextLine("Ship Movement: Ship 3 moved with speed 15 to tile 488.")
        assertNextLine("Ship Movement: Ship 4 moved with speed 15 to tile 488.")
        assertNextLine("Ship Movement: Ship 5 moved with speed 15 to tile 488.")
        assertNextLine("Ship Movement: Ship 6 moved with speed 15 to tile 488.")
        skipUntilAndAssert(LogType.SHIPMOVE, "Ship 1 moved with speed 30 to tile 413.")
        assertNextLine("Ship Movement: Ship 2 moved with speed 30 to tile 413.")
        assertNextLine("Ship Movement: Ship 3 moved with speed 30 to tile 413.")
        assertNextLine("Ship Movement: Ship 4 moved with speed 30 to tile 413.")
        assertNextLine("Ship Movement: Ship 5 moved with speed 30 to tile 413.")
        assertNextLine("Ship Movement: Ship 6 moved with speed 30 to tile 413.")
        skipUntilAndAssert(LogType.SHIPMOVE, "Ship 1 moved with speed 45 to tile 341.")
        skipLines(5)
        skipUntilAndAssert(LogType.SHIPMOVE, "Ship 1 moved with speed 60 to tile 193.")
        skipLines(5)
        skipUntilAndAssert(LogType.SHIPMOVE, "Ship 1 moved with speed 75 to tile 21.")
        skipLines(5)
        skipUntilAndAssert(LogType.SHIPMOVE, "Ship 1 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 2 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 3 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 4 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 5 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 6 moved with speed 80 to tile 143.")
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
