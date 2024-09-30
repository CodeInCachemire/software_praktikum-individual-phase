package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Reward
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.TaskType
import de.unisaarland.cs.se.selab.parser.SimulationData
import de.unisaarland.cs.se.selab.task.Task

/**
 * the controller class for handling tasks.
 */
class TaskHandler(
    simulationData: SimulationData,
    private val oceanMap: OceanMap,
    private val pathFinder: PathFinder
) {
    private val tasks: ArrayDeque<Task> = ArrayDeque(simulationData.tasks.values.sortedBy { it.tick })
    private val activeTasks = sortedSetOf<Task>(compareBy { it.id })

    /**
     * this function is called by the simulation and updates each tick the tasks.
     * @param tick the current tick
     */
    fun updateTasks(tick: Int) {
        // start new tasks if the tick is reached
        while (tasks.firstOrNull()?.tick == tick) {
            val task = tasks.removeFirst()
            activeTasks.add(task)
        }
        // activate new tasks and give rewards if a task has been achieved.
        for (task in activeTasks) {
            if (task.tick == tick) {
                activateTask(task)
            } else if (task.finished) {
                giveReward(task)
            }
        }
        // remove finished or failed tasks from the active tasks.
        activeTasks.removeIf { it.finished || it.toBeRemoved }
    }

    /**
     * helper function that activates a task.
     * @param task the task that should be activated
     */
    private fun activateTask(task: Task) {
        val ship = oceanMap.shipToTile.keys.first { ship -> ship.id == task.shipId }
        val shipTile = oceanMap.shipToTile[ship] ?: return
        val distanceWithFuel = ship.getDistanceWithFuel()
        if (pathFinder.isReachableWithinDistance(shipTile, setOf(task.destination), distanceWithFuel)) {
            // remove overwritten tasks from the active tasks
            ship.task?.toBeRemoved = true
            ship.task = task
            ship.behaviour = Behaviour.DOING_TASK
            Logger.logTask(task.id, task.getType(), ship.id, task.destination.id)
            activeTasks.add(task)
            if (task.getType() == TaskType.COOPERATE) task.update(ship, oceanMap, false)
        }
    }

    /**
     * helper function that gives the reward of a task to the ship.
     * @param task the task was achieved
     */
    private fun giveReward(task: Task) {
        val ship = oceanMap.shipToTile.keys.first { it.id == task.rewardShipId }
        ship.reward.add(task.reward.type)
        changeParameters(ship, task.reward)
        Logger.logReward(task.id, ship.id, task.reward.type)
    }

    /**
     * helper function that changes the behavior of a ship according to the reward.
     * @param ship the ship that should be changed
     * @param reward the reward that should be given to the ship
     */
    private fun changeParameters(ship: Ship, reward: Reward) {
        when (reward.type) {
            RewardType.CONTAINER -> {
                changeContainer(ship, reward)
            }

            RewardType.TELESCOPE -> {
                val rewardVisibility = reward.visibilityRange
                if (rewardVisibility != null) {
                    ship.visibilityRange += rewardVisibility
                }
            }

            else -> {}
        }
    }

    /**
     * helper function that changes the container of a ship according to the reward.
     * @param ship the ship that should be changed
     * @param reward the reward that should be given to the ship
     */
    private fun changeContainer(ship: Ship, reward: Reward) {
        if (reward.garbageType in ship.corporation.garbageTypes) {
            rewardCapacity(ship.maxGarbageCapacity, reward)
            rewardCapacity(ship.garbageCapacity, reward)
        }
    }

    /**
     * helper function that changes the capacity of a ship according to the reward.
     * @param currentCapacity the current capacity of the ship
     * @param reward the reward that should be given to the ship
     */
    private fun rewardCapacity(currentCapacity: MutableMap<GarbageType, Int>, reward: Reward) {
        if (!currentCapacity.keys.contains(reward.garbageType)) {
            reward.garbageType?.let { reward.capacity?.let { it1 -> currentCapacity.put(it, it1) } }
        } else {
            val currentCapability = currentCapacity[reward.garbageType]
            if (reward.capacity != null && currentCapability != null) {
                reward.garbageType?.let { currentCapacity.put(it, currentCapability + reward.capacity) }
            }
        }
    }
}
