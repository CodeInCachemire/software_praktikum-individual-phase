package de.unisaarland.cs.se.selab.task

import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.TaskData
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.TaskType

/**
 * class for find task
 */
class FindTask(taskData: TaskData) : Task(taskData) {

    override fun update(ship: Ship, oceanMap: OceanMap, movementPhase: Boolean) {
        if (checkLocation(ship, oceanMap)) {
            val shipTile = oceanMap.getShipTile(ship)
            finished = oceanMap.getGarbageOnTile(shipTile).isNotEmpty()
            toBeRemoved = true
            ship.task = null
            ship.behaviour = Behaviour.DEFAULT
        }
    }

    /**
     * returns the type of the task
     */
    override fun getType(): TaskType {
        return TaskType.FIND
    }
}
