package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * test typhoon
 */
class RepairTest : SystemTestExtension() {
    // short description of the test
    override val description = "Test if ship repairs after typhoon."
    override val name = "Repair post typhoon check."

    override val map = "mapFiles/bigMapForTyphoonAndRepair.json"
    override val corporations = "corporationFiles/corporationsForTyphoonAndRepair.json"
    override val scenario = "scenarioFiles/scenarioForTyphoonAndRepair.json"

    override val maxTicks = 3
    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.REPAIR, LoggingUtils.logDamageRepairStart(0, 2, 200))
        // the next line should be this:
        skipUntilAndAssert(LogType.REPAIR, LoggingUtils.logDamageRepairFinish(0))
    }
}
