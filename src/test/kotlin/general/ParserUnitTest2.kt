package general

import de.unisaarland.cs.se.selab.parser.Parser
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class ParserUnitTest2 {
    private val mapFilePath = "src/systemtest/resources/mapFiles/bigMapForTyphoonAndRepair.json"
    private val corpFilePath = "src/systemtest/resources/corporationFiles/corporationsForTyphoonAndRepair.json"
    private val scenarioPath = "src/systemtest/resources/scenarioFiles/scenarioForTyphoonAndRepair.json"

    @Test
    fun runParseSuccess() {
        val parser = Parser()
        val simulationDataRes = parser.parse(listOf(mapFilePath, corpFilePath, scenarioPath))
        Assertions.assertTrue(simulationDataRes.isSuccess)
    }
}
