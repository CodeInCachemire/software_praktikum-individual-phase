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
    val visibilityRangeOriginal = visibilityRange
    var returnToRefuel = false
    var returnToUnload = false
    var returnToRepair = false
    var returnToPurchase = false

    var isRefillingCapacity = false
    private var refuelingCapacityOriginal: Int = -1
    var currentRefuelingCapacity: Int = -1

    // /////////////////////////////////////////////////////
    private var refuelTimeOriginal = -1
    var tickCounter: Int = -1

    var beingRefueledByShip = false
    var refuelingShipCurrently = false
    var shipIsClaimed = false
    var shipToRefuel: Ship? = null

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
     * Returns if the ship is refilling it's capacity during this tick.
     */
    fun isRefilling(): Boolean {
        return waitingAtHarbor && waitingAtARefuelingStation &&
            behaviour == Behaviour.REFUELING && isRefillingCapacity && type == ShipType.REFUELING
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
     * Set original refueling capacity
     */
    fun setOriginalRefuelCap(refuelingCapacity: Int, refuelingTime: Int) {
        this.refuelingCapacityOriginal = refuelingCapacity
        this.currentRefuelingCapacity = refuelingCapacity
        this.refuelTimeOriginal = refuelingTime
        this.tickCounter = refuelingTime
    }

    /**
     * Get original refueling Capacity
     */
    fun getOriginalRefuelCap(): Int {
        return refuelingCapacityOriginal
    }

    /**
     * Get original refueling Capacity
     */
    fun getOriginalRefuelTime(): Int {
        return refuelTimeOriginal
    }

    /**
     * Get the original refueling time.
     */
    fun setRefuelTimeBack() {
        this.tickCounter = this.refuelTimeOriginal
    }
}
