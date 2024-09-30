package de.unisaarland.cs.se.selab.systemtest.utils

import de.unisaarland.cs.se.selab.systemtest.api.SystemTest
import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError

/**
 * Extension to the default SystemTest class which adds utility functions for skipping logs.
 * Feel free to extend/change this class as you see fit!
 */
abstract class SystemTestExtension : SystemTest() {

    /**
     * Skips until the given [startString] is found. Returns the line which starts with the given string.
     * Supposed to be used by skipUntilLogType to get the next log of a specific type.
     */
    private suspend fun skipUntilString(startString: String): String {
        val line: String = getNextLine()
            ?: throw SystemTestAssertionError("End of log reached when there should be more.")
        return if (line.startsWith(startString)) {
            line
        } else {
            skipUntilString(startString)
        }
    }

    /**
     * Skips until the given [log] is found. Returns the line of the correct logType.
     * Supposed to be used by skipUntilAnd Assert to assert the correctness of the next log of a specific type.
     */
    private suspend fun skipUntilLogType(log: LogType): String {
        return skipUntilString(log.toString())
    }

    /**
     * Skips until the given [log] is found and asserts that it ends with [expected] string.
     */
    suspend fun skipUntilAndAssert(log: LogType, expected: String) {
        val line = skipUntilLogType(log)
        if (!line.endsWith(expected)) {
            throw SystemTestAssertionError("expected:\n$expected\n but got $line")
        }
    }
}
