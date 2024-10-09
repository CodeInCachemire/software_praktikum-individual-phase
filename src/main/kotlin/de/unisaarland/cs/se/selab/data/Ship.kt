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
class Ship(
    val id: Int,
    val type: ShipType,
    val corporation: Corporation,
    var maxVelocity: Int,
    var acceleration: Int,
    val maxFuel: Int,
    var fuelConsumption: Int,
    var visibilityRange: Int,
    val maxGarbageCapacity: MutableMap<GarbageType, Int>,
) : Comparable<Ship> {
    var fuelConsumptionPerTile = fuelConsumption * Constants.TILE_DISTANCE
    var velocity = 0
    val reward = mutableListOf<RewardType>()
    var fuel = maxFuel
    var task: Task? = null
    var behaviour = Behaviour.DEFAULT
    val garbageCapacity = maxGarbageCapacity.toMutableMap()
    var waitingAtHarbor = false
    var waitingAtAShipyard = false
    var waitingAtARefuelingStation = false
    var waitingAtAUnloadingStation = false
    var isDamaged = false
    val maxVelocityOriginal = maxVelocity
    val accelerationOriginal = acceleration
    var beingRefueledByShip = false
    val visibilityRangeOriginal = visibilityRange
    var returnToRefuel = false
    var returnToUnload = false
    var returnToRepair = false
    var returnToPurchase = false
    private var refuelingCapacityOriginal: Int = -1
    var refuelingCapacity: Int = -1
    var refuelingTime: Int = -1

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
        return waitingAtHarbor && waitingAtARefuelingStation && behaviour == Behaviour.REFUELING
    }

    /**
     * Returns if the ship is unloading during this tick
     */
    fun isUnloading(): Boolean {
        return waitingAtHarbor && waitingAtAUnloadingStation && behaviour == Behaviour.UNLOADING
    }

    /**
     * Returns if the ship is repairing during this tick
     */
    fun isRepairing(): Boolean {
        return waitingAtHarbor && waitingAtAShipyard && behaviour == Behaviour.REPAIRING
    }

    /**
     * Returns if ship is buying this tick or not
     */
    fun isPurchasing(): Boolean {
        return returnToPurchase
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

    /**
     * Set refuelingcapacity and refuelingTime
     */
    fun setRefuelingCapacityAndTime(refuelingCapacity: Int, refuelingTime: Int) {
        // SHIP TYPE MUST BE REFUELING TYPE //TODO()
        this.refuelingCapacity = refuelingCapacity
        this.refuelingTime = refuelingTime
    }

    /**
     * Set original refueling capacity
     */
    fun setOriginalRefuelCap(refuelingCapacity: Int) {
        this.refuelingCapacityOriginal = refuelingCapacity
        this.refuelingCapacity = refuelingCapacity
    }

    /**
     * Get original refueling Capacity
     */
    fun getOriginalRefuelCap(): Int {
        return refuelingCapacityOriginal
    }
}
