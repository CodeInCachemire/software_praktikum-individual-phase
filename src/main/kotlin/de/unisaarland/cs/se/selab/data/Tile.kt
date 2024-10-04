package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.TileType

/**
 * The Tile class
 */
data class Tile(
    val id: Int,
    val type: TileType,
    val current: Current?,
    val coordinate: Coordinate,
    val harbor: Boolean,
    val harborID: Int?
) : Comparable<Tile> {
    var numRestrictions = 0
    val restricted get() = numRestrictions > 0

    override fun compareTo(other: Tile): Int {
        return id.compareTo(other.id)
    }
}
