package general

import de.unisaarland.cs.se.selab.parser.Parser
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class ParserUnitTest1 {
    private val mapFilePath = "src/systemtest/resources/mapFiles/smallMap3Stations.json"
    private val corpFilePath = "src/systemtest/resources/corporationFiles/corporations.json"
    private val scenarioPath = "src/systemtest/resources/scenarioFiles/scenario.json"

    @Test
    fun runParseFail() {
        val parser = Parser()
        val simulationDataRes = parser.parse(listOf(mapFilePath, corpFilePath, scenarioPath))
        Assertions.assertTrue(simulationDataRes.isFailure)
    }
}
