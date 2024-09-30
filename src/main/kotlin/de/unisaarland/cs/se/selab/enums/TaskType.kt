package de.unisaarland.cs.se.selab.enums

import de.unisaarland.cs.se.selab.parser.JsonKeys

/**
 * The TaskType enum class
 */
enum class TaskType {
    COLLECT,
    EXPLORE,
    FIND,
    COOPERATE;

    /**
     * Creates a TaskType based on the input type
     */
    companion object {
        /**
         * Returns the TaskType enum value from the given string
         */
        fun createTaskType(type: String): TaskType {
            return when (type) {
                JsonKeys.COLLECT -> COLLECT
                JsonKeys.EXPLORE -> EXPLORE
                JsonKeys.FIND -> FIND
                JsonKeys.COOPERATE -> COOPERATE
                else -> throw IllegalArgumentException("Invalid task type")
            }
        }
    }
}
