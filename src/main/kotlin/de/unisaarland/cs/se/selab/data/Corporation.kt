package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.GarbageType
import java.util.SortedSet

/**
 * Corporation class.
 */
data class Corporation(
    val id: Int,
    val harbors: Set<Tile>,
    val garbageTypes: List<GarbageType>,
    var credits: Int
) {
    val ships = mutableListOf<Ship>()
    val assignedShipsPerTile = mutableMapOf<Tile, MutableList<Ship>>()
    val trackedGarbage = mutableSetOf<Garbage>()
    val information = mutableMapOf<Tile, SortedSet<Garbage>>()
    var lastCooperated: Corporation? = null
    var isBuyingShip = false
    var purchaseCounter = -1
    var assignedBuyingShipId = -1
    var storeBoughtTileNdShip: Pair<Tile?, Ship?> = Pair(null, null)
}
