package de.unisaarland.cs.se.selab.systemtest.utils

/**
 * Utils object for providing strings for expected logs.
 * Not complete, extend this for more log messages as you see fit!
 */
object LoggingUtils {
    /**
     * Returns string for invalid config file [fileName].
     */
    fun initializationInfoInvalidAssertion(fileName: String): String {
        return "Initialization Info: $fileName is invalid."
    }

    /**
     * Returns string for corporation [corporationId] collected [amount] of garbage.
     */
    fun simulationStatisticsCorporation(corporationId: Int, amount: Int): String {
        return "Simulation Statistics: Corporation $corporationId collected $amount of garbage."
    }
}
