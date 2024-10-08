package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * test typhoon
 */
class SimpleUnloadTest : SystemTestExtension() {
    // short description of the test
    override val description = "Test if unload works"
    override val name = "Simple Unload Test"

    override val map = "mapFiles/bigMapForTyphoonAndRepair.json"
    override val corporations = "corporationFiles/corporationsForTyphoonAndRepair.json"
    override val scenario = "scenarioFiles/scenarioForTyphoonAndRepair.json"

    override val maxTicks = 15
    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.UNLOAD, LoggingUtils.logUnload(3, 2000, GarbageType.CHEMICALS, 0, 8000))
        // the next line should be this:
    }
}
