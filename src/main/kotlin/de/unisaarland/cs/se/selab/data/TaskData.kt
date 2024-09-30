package de.unisaarland.cs.se.selab.data

/**
 * Contains data for tasks
 */
data class TaskData(
    val id: Int,
    val tick: Int,
    val shipId: Int,
    val reward: Reward,
    val rewardShipId: Int,
    val destination: Tile
)
