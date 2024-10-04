package de.unisaarland.cs.se.selab.systemtest.mapParserTests

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Example System test. Here we introduce the usage of the default SystemTest implementation
 * as well as the use of the utility methods. See utility method documentation for more details.
 */
class Stations3Invalid : SystemTestExtension() {
    // short description of the test
    override val description = "Harbor With 3 Stations should be invalid"

    // name of the test
    override val name = "Harbor With 3 Stations"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/smallMapWithHarborWith3Stations.json"
    override val corporations = "corporationFiles/corporations.json"
    override val scenario = "scenarioFiles/scenario.json"

    // max ticks parameter for the test execution
    override val maxTicks = 0

    override suspend fun run() {
        assertNextLine("Initialization Info: smallMapWithHarborWith3Stations.json is invalid.")
    }
}
