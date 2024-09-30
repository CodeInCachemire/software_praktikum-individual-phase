package de.unisaarland.cs.se.selab.task

import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.TaskData
import de.unisaarland.cs.se.selab.enums.TaskType

/**
 * class for coordinating task
 */
class CooperationTask(taskData: TaskData) : Task(taskData) {
    private var waitingAtHarbor = false

    override fun update(ship: Ship, oceanMap: OceanMap, movementPhase: Boolean) {
        if (checkLocation(ship, oceanMap)) {
            if (!waitingAtHarbor) {
                waitingAtHarbor = true
            } else {
                finished = movementPhase
            }
        } else {
            waitingAtHarbor = false
        }
    }

    /**
     * returns the type of the task
     */
    override fun getType(): TaskType {
        return TaskType.COOPERATE
    }
}
