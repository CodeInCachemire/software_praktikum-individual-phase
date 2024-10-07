package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * test typhoon
 */
class TestTyphoonEvent : SystemTestExtension() {
    // short description of the test
    override val description = "Tests event logging of typhoon and typhoon logging"
    override val name = "Typhoon Event and Normal Log"

    override val map = "mapFiles/bigMapForTyphoonAndRepair.json"
    override val corporations = "corporationFiles/corporationsForTyphoonAndRepair.json"
    override val scenario = "scenarioFiles/scenarioForTyphoonAndRepair.json"

    override val maxTicks = 3
    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.EVENT, LoggingUtils.eventTyphoon(0, "TYPHOON"))
        // the next line should be this:
        assertNextLine(LoggingUtils.typhoonEvent(0, 1, 513, sortedSetOf(0, 1, 3, 4)))
    }
}
