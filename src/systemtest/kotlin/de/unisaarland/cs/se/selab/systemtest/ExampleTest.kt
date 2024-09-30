package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.LoggingUtils
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Example System test. Here we introduce the usage of the default SystemTest implementation
 * as well as the use of the utility methods. See utility method documentation for more details.
 */
class ExampleTest : SystemTestExtension() {
    // short description of the test
    override val description = "tests statistics after 0 ticks"

    // name of the test
    override val name = "ExampleTest"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/smallMap.json"
    override val corporations = "corporationFiles/corporations.json"
    override val scenario = "scenarioFiles/scenario.json"

    // max ticks parameter for the test execution
    override val maxTicks = 0

    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.SIMULATION_STATISTICS, LoggingUtils.simulationStatisticsCorporation(0, 0))
        // the next line should be this:
        assertNextLine(LoggingUtils.simulationStatisticsCorporation(1, 0))
        // skipping the next lines
        skipLines(10)
        // assert that the log ended
        // you do not necessarily need to assert this
        assertEnd()
    }
}
