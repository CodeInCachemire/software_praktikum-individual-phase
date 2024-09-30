package de.unisaarland.cs.se.selab.enums

import de.unisaarland.cs.se.selab.parser.JsonKeys

/**
 * The RewardType enum class
 */
enum class RewardType {
    TELESCOPE,
    RADIO,
    CONTAINER,
    TRACKER;

    /**
     * Creates a RewardType based on the input type
     */

    companion object {
        /**
         * Returns the RewardType enum value from the given string
         */
        fun createRewardType(type: String): RewardType {
            return when (type) {
                JsonKeys.TELESCOPE -> TELESCOPE
                JsonKeys.RADIO -> RADIO
                JsonKeys.CONTAINER -> CONTAINER
                JsonKeys.TRACKER -> TRACKER
                else -> throw IllegalArgumentException("Invalid reward type")
            }
        }

        /**
         * Returns the corresponding TaskType for the given RewardType
         */
        fun correspondingTaskType(rewardType: RewardType): TaskType {
            return when (rewardType) {
                TELESCOPE -> TaskType.EXPLORE
                RADIO -> TaskType.COOPERATE
                CONTAINER -> TaskType.COLLECT
                TRACKER -> TaskType.FIND
            }
        }
    }
}
