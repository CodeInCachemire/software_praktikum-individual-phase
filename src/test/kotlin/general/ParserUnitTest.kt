package general

import de.unisaarland.cs.se.selab.parser.Parser
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class ParserUnitTest {
    private val mapFilePath = "src/systemtest/resources/mapFiles/shipPurchase.json"
    private val corpFilePath = "src/systemtest/resources/corporationFiles/corporationsForPurchase.json"
    private val scenarioPath = "src/systemtest/resources/scenarioFiles/scenario.json"

    @Test
    fun runParseSuccess() {
        val parser = Parser()
        val simulationDataRes = parser.parse(listOf(mapFilePath, corpFilePath, scenarioPath))
        Assertions.assertTrue(simulationDataRes.isSuccess)
    }
}
