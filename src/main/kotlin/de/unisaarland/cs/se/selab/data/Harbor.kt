package de.unisaarland.cs.se.selab.data

/**
 * This is the harbor class
 */
data class Harbor(
    val id: Int,
    val location: Int,
    val corporations: Set<Int>,
    val shipyardStation: ShipyardStation?,
    val refuelingStation: RefuelingStation?,
    val unloadingStation: UnloadingStation?
)
