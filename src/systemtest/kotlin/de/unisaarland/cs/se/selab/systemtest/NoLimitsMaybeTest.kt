package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * This is to test if a refueling station adheres to the limit.
 */
class NoLimitsMaybeTest : SystemTestExtension() {
    // short description of the test
    override val description = "tests credits"

    // name of the test
    override val name = "tests credits"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/bigCorporationRefuels.json"
    override val corporations = "corporationFiles/corporationScout.json"
    override val scenario = "scenarioFiles/nothing.json"

    // max ticks parameter for the test execution
    override val maxTicks = 5
    override suspend fun run() {
        skipLines(46)
        assertNextLine("Ship Movement: Ship 1 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 2 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 3 moved with speed 80 to tile 143.")
        assertNextLine("Ship Movement: Ship 4 moved with speed 80 to tile 143.")
        skipLines(3)
        assertNextLine("Refueling: Ship 1 starts to refuel at harbor 0 and paid 450 credits.")
        assertNextLine("Refueling: Ship 2 starts to refuel at harbor 0 and paid 450 credits.")
        assertNextLine("Refueling: Ship 3 cannot refuel at harbor 0.")
        assertNextLine("Refueling: Ship 4 cannot refuel at harbor 0.")
    }
}
