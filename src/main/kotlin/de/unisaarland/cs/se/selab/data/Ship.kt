package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.ShipType
import de.unisaarland.cs.se.selab.task.Task
import kotlin.math.min

/**
 * The Ship class
 */
open class Ship(
    val id: Int,
    val type: ShipType,
    val corporation: Corporation,
    val maxVelocity: Int,
    var acceleration: Int,
    val maxFuel: Int,
    var fuelConsumption: Int,
    var visibilityRange: Int,
    val maxGarbageCapacity: MutableMap<GarbageType, Int>
) : Comparable<Ship> {
    val fuelConsumptionPerTile = fuelConsumption * Constants.TILE_DISTANCE
    var velocity = 0
    val reward = mutableListOf<RewardType>()
    var fuel = maxFuel
    var task: Task? = null
    var behaviour = Behaviour.DEFAULT
    val garbageCapacity = maxGarbageCapacity.toMutableMap()
    var waitingAtHarbor = false
    var isDamaged = false

    /**
     * Accelerates the ship and increases its current velocity
     */
    fun accelerate() {
        velocity = min(velocity + acceleration, maxVelocity)
    }

    /**
     * Checks if ship can attach tracker
     */
    fun canAttachTracker(): Boolean {
        return RewardType.TRACKER in reward && !isRefueling()
    }

    /**
     * Checks if ship can collect garbage
     */
    fun canCollect(): Boolean {
        return garbageCapacity.any { it.value > 0 } && !isRefueling()
    }

    /**
     * Checks if the ship can cooperate with another ship.
     */
    fun canCooperateWith(otherShip: Ship): Boolean {
        val canCooperate = type == ShipType.COORDINATING || reward.contains(RewardType.RADIO)
        val fromDifferentCorporation = otherShip.corporation != this.corporation
        val notLastCooperatedWith = otherShip.corporation != this.corporation.lastCooperated
        return canCooperate && fromDifferentCorporation && notLastCooperatedWith && !isRefueling()
    }

    /**
     * Returns if the ship is refueling during this tick
     */
    fun isRefueling(): Boolean {
        return waitingAtHarbor && behaviour == Behaviour.REFUELING
    }

    /**
     * Returns if the ship is unloading during this tick
     */
    fun isUnloading(): Boolean {
        return waitingAtHarbor && behaviour == Behaviour.UNLOADING
    }

    /**
     * Number of tiles reachable with current velocity
     */
    fun getDistanceWithVelocity(velocity: Int = this.velocity): Int {
        return velocity / Constants.TILE_DISTANCE
    }

    /**
     * Number of tiles reachable with current fuel
     */
    fun getDistanceWithFuel(fuel: Int = this.fuel): Int {
        return fuel / fuelConsumptionPerTile
    }

    override fun compareTo(other: Ship): Int {
        return id.compareTo(other.id)
    }
}
