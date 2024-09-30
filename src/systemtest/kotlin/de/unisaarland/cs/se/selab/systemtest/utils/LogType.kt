package de.unisaarland.cs.se.selab.systemtest.utils

/**
 * The types of logs used in utility functions.
 * Not complete, extend this for more log types as you see fit!
 */
enum class LogType(private val message: String) {
    INITIALIZATION_INFO("Initialization Info"),
    SIMULATION_INFO("Simulation Info"),
    SIMULATION_STATISTICS("Simulation Statistics");

    override fun toString(): String {
        return message
    }
}
