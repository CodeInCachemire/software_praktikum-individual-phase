package de.unisaarland.cs.se.selab.parser

import de.unisaarland.cs.se.selab.Logger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

/**
 * The Parser class collects all the parsing logic
 * calls the parsers and creates data objects
 */
class Parser {

    private val simulationData = SimulationData()

    /**
     * Parses the input files
     */
    fun parse(filenames: List<String>): Result<SimulationData> {
        require(filenames.size == 3) { "There should be 3 filenames" }

        val mapJsonString = File(filenames[0]).readText()
        val corpJsonString = File(filenames[1]).readText()
        val scenarioJsonString = File(filenames[2]).readText()
        val kotlinLogger = KotlinLogging.logger("Parser")

        // Map
        parseMap(mapJsonString).onFailure {
            Logger.logParsingResult(false, File(filenames[0]).name)
            kotlinLogger.error { it.message }
            return Result.failure(it)
        }
        Logger.logParsingResult(true, File(filenames[0]).name)

        // Corporation
        parseCorporation(corpJsonString).onFailure {
            Logger.logParsingResult(false, File(filenames[1]).name)
            kotlinLogger.error { it.message }
            return Result.failure(it)
        }
        Logger.logParsingResult(true, File(filenames[1]).name)

        // Events and Tasks / Scenario
        parseScenario(scenarioJsonString).onFailure {
            kotlinLogger.error { it.message }
            Logger.logParsingResult(false, File(filenames[2]).name)
            return Result.failure(it)
        }
        Logger.logParsingResult(true, File(filenames[2]).name)

        return Result.success(simulationData)
    }

    /**
     * Parses the Map from the JSON file
     */
    private fun parseMap(jsonString: String): Result<Unit> {
        val parser = MapParser(simulationData)
        return parser.parse(jsonString)
    }

    /**
     * Parses the Corporation from the JSON file
     */
    private fun parseCorporation(jsonString: String): Result<Unit> {
        val parser = CorporationParser(jsonString, simulationData)
        return parser.parse()
    }

    /**
     * Parses the events and tasks from the JSON file
     */
    private fun parseScenario(jsonString: String): Result<Unit> {
        val parser = ScenarioParser(simulationData)
        return parser.parse(jsonString)
    }
}
