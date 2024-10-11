package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * test typhoon
 */
class IllegalTest : SystemTestExtension() {
    // short description of the test
    override val description = "Test if unload works"
    override val name = "Simple Unload Test"

    override val map = "mapFiles/bigmapIllegal.json"
    override val corporations = "corporationFiles/corporationsIllegal.json"
    override val scenario = "scenarioFiles/plasticIllegal.json"

    override val maxTicks = 15
    override suspend fun run() {
        // skipping until the statistics:
        skipUntilAndAssert(LogType.GARBAGE, "Garbage Collection: Ship 1 collected 2000 of garbage PLASTIC with 1.")
        // the next line should be this:
        skipUntilAndAssert(LogType.GARBAGE, "Garbage Collection: Ship 1 collected 2000 of garbage PLASTIC with 3.")
        skipUntilAndAssert(LogType.GARBAGE, "Garbage Collection: Ship 1 collected 1000 of garbage OIL with 2.")
        assertNextLine("Corporation Action: Corporation 0 is starting to cooperate with other corporations.")
        skipUntilAndAssert(
            LogType.UNLOAD,
            "Unload: Ship 1 unloaded 2000 of garbage PLASTIC " +
                "at harbor 2 and received 8000 credits."
        )
        assertNextLine("Corporation Action: Corporation 0 finished its actions.")
    }
}
