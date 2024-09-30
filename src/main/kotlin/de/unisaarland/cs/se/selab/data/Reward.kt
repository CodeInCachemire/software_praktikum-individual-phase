package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType

/**
 * The Reward class
 */
data class Reward(
    val id: Int,
    val type: RewardType,
    val visibilityRange: Int?,
    val capacity: Int?,
    val garbageType: GarbageType?
)
