package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.Direction
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.TileType

/**
 * Utility object for handling drifting behavior of garbage on the ocean map,
 * since this is needed in the DriftHandler as well as in the storm event.
 * Provides methods to compute drift paths and handle garbage movement based on speed, direction,
 * and tile conditions (e.g., oil limits and deep ocean removal of chemicals).
 */
object DriftingUtil {

    /**
     * Computes a list of candidate tiles to drift to. Taking into account speed and direction
     * and also stopping as soon as we would reach a non-traversable tile. The list is reversed,
     * so the furthest away tile is at the start of the list.
     */
    fun getCandidateDriftTiles(oceanMap: OceanMap, tile: Tile, direction: Direction, speed: Int): List<Tile> {
        // Generates a sequence of tiles going in the direction of the current and stopping
        // if we would reach a non-traversable tile (since land tiles are removed from the map,
        // oceanMap.getNeighbour(it, direction) returns null for both empty tiles and land tiles).
        val candidateDriftTiles = generateSequence(tile) { oceanMap.getNeighbour(it, direction) }
            // Takes tiles from the sequence according to the given speed or less if the sequence is shorter.
            .take(speed / Constants.TILE_DISTANCE + 1).toList()
        return candidateDriftTiles.asReversed()
    }

    /**
     * Drifts the given garbage according to speed, direction and oil limit conditions.
     * Also removes chemicals from the map, if they drifted into the deep ocean.
     */
    fun driftGarbage(oceanMap: OceanMap, garbage: Garbage, tile: Tile, direction: Direction, speed: Int) {
        val candidateDriftTiles = getCandidateDriftTiles(oceanMap, tile, direction, speed)

        for (candidateDriftTile in candidateDriftTiles) {
            if (canDriftOnTile(oceanMap, garbage, candidateDriftTile)) {
                oceanMap.moveGarbage(garbage, candidateDriftTile)
                val isChemicals = garbage.type == GarbageType.CHEMICALS
                val isDeepOcean = candidateDriftTile.type == TileType.DEEP_OCEAN
                if (isChemicals && isDeepOcean) {
                    oceanMap.removeGarbage(garbage)
                }
                return
            }
        }
    }

    /**
     * Checks that we don't surpass the maximum oil amount on a tile when drifting.
     */
    private fun canDriftOnTile(oceanMap: OceanMap, garbage: Garbage, tile: Tile): Boolean {
        if (garbage.type != GarbageType.OIL) return true
        val garbageOnTile = oceanMap.getGarbageOnTile(tile)
        val oilSumOnTile = SumsPerTypeUtil.getGarbageSumsPerType(garbageOnTile)[GarbageType.OIL] ?: 0
        return oilSumOnTile + garbage.amount <= Constants.MAX_OIL_AMOUNT
    }
}
