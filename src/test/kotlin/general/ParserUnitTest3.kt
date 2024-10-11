package general

import de.unisaarland.cs.se.selab.parser.Parser
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class ParserUnitTest3 {
    private val mapFilePath = "src/systemtest/resources/mapFiles/bigCorporationRefuels.json"
    private val corpFilePath = "src/systemtest/resources/corporationFiles/corporationScout.json"
    private val scenarioPath = "src/systemtest/resources/scenarioFiles/nothing.json"

    @Test
    fun runParseSuccess() {
        val parser = Parser()
        val simulationDataRes = parser.parse(listOf(mapFilePath, corpFilePath, scenarioPath))
        Assertions.assertTrue(simulationDataRes.isSuccess)
    }
}
