package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.enums.GarbageType

/**
 * Utility object for calculating sums of garbage amounts and ship garbage capacities per type.
 */
object SumsPerTypeUtil {

    /**
     * Sums up the garbage amounts per type for a given collection of garbage.
     */
    fun getGarbageSumsPerType(garbageOnTile: Collection<Garbage>): Map<GarbageType, Int> {
        val garbageSumsPerType = mutableMapOf<GarbageType, Int>()
        for (garbage in garbageOnTile) {
            val garbageSum = garbageSumsPerType[garbage.type] ?: 0
            garbageSumsPerType[garbage.type] = garbageSum + garbage.amount
        }
        return garbageSumsPerType
    }

    /**
     * Sums up the garbage capacity amounts per type for a given collection of ships.
     */
    fun getCapacitySumsPerType(ships: Collection<Ship>): Map<GarbageType, Int> {
        val capacitySumsPerType = mutableMapOf<GarbageType, Int>()
        for (ship in ships) {
            for ((garbageType, capacity) in ship.garbageCapacity) {
                val capacitySum = capacitySumsPerType[garbageType] ?: 0
                capacitySumsPerType[garbageType] = capacitySum + capacity
            }
        }
        return capacitySumsPerType
    }
}
