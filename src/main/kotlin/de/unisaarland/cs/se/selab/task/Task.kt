package de.unisaarland.cs.se.selab.task

import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.TaskData
import de.unisaarland.cs.se.selab.enums.TaskType

/**
 * abstract class for all Tasks
 */
abstract class Task(taskData: TaskData) {
    val id = taskData.id
    val tick = taskData.tick
    val shipId = taskData.shipId
    val reward = taskData.reward
    val rewardShipId = taskData.rewardShipId
    val destination = taskData.destination
    var finished: Boolean = false
    var toBeRemoved: Boolean = false

    /**
     * This function will be called if the ship has reached its designated destination
     */
    abstract fun update(ship: Ship, oceanMap: OceanMap, movementPhase: Boolean)

    protected fun checkLocation(ship: Ship, oceanMap: OceanMap): Boolean {
        return oceanMap.getShipTile(ship) == destination
    }

    /**
     * returns the type of the task
     */
    abstract fun getType(): TaskType
}
