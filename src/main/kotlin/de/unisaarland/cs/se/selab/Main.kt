package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.control.Simulation
import de.unisaarland.cs.se.selab.parser.Parser
import kotlin.system.exitProcess

/**
 * Main class
 */
fun main(args: Array<String>) {
    lateinit var map: String
    lateinit var corporations: String
    lateinit var scenario: String

    var ticks: Int? = null

    if ("--help" in args) {
        help()
    }

    val pa = args.filter { it != "--help" }.zipWithNext()
    for (pair in pa) {
        when (pair.first) {
            "--map" -> map = pair.second
            "--corporations" -> corporations = pair.second
            "--scenario" -> scenario = pair.second
            "--max_ticks" -> ticks = pair.second.toInt()
            "--out" -> Logger.setFilePath(pair.second)
            else -> continue
        }
    }
    if (map == "" || corporations == "" || scenario == "") {
        exitProcess(1)
    }
    simulation(map, corporations, scenario, ticks ?: 0)
}

/**
 * Help!
 */
private fun help() {
    Logger.printer.println("HELP!!!")
    return
}

/**
 * parse the map, corporations, and scenario, and start the simulation.
 */
private fun simulation(map: String, corporations: String, scenario: String, ticks: Int) {
    val parser = Parser()
    val simulationDataRes = parser.parse(listOf(map, corporations, scenario))
    if (simulationDataRes.isFailure) {
        return
    }
    val simulationData = simulationDataRes.getOrThrow()
    Simulation(simulationData, ticks)
}
