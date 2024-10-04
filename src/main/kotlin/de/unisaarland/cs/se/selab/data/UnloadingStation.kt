package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.GarbageType

/**
 * This is the unloadingStationClass
 */
data class UnloadingStation(
    val unloadReturn: Int,
    val garbageTypes: Set<GarbageType>
)
