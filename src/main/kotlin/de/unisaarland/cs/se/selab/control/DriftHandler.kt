package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Current
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile

/**
 * DriftHandler class handles the drift of garbage and ships
 */
class DriftHandler(private val oceanMap: OceanMap) {
    private val sortedCurrentTiles = oceanMap.tiles.values
        .filter { it.current != null }
        .sortedBy { it.id }

    /**
     * Drift garbage
     */
    fun driftGarbage() {
        val driftedGarbage = mutableSetOf<Garbage>()
        for (tile in sortedCurrentTiles) {
            val current = tile.current
            if (current != null) driftGarbageOnTile(tile, current, driftedGarbage)
        }
    }

    /**
     * Drift garbage on a tile
     */
    private fun driftGarbageOnTile(tile: Tile, current: Current, driftedGarbage: MutableSet<Garbage>) {
        oceanMap.getNeighbour(tile, current.direction) ?: return
        var intensityForGarbage = current.intensity * Constants.GARBAGE_PER_INTENSITY
        for (garbage in oceanMap.getGarbageOnTile(tile).filter { it !in driftedGarbage }) {
            if (intensityForGarbage >= garbage.amount) {
                intensityForGarbage -= garbage.amount
                driftAndLogGarbage(garbage, tile, current, false)
                driftedGarbage.add(garbage)
            } else if (intensityForGarbage > 0) {
                val splitGarbage = Garbage(
                    oceanMap.getMaxGarbageId() + 1,
                    garbage.type,
                    intensityForGarbage
                )
                garbage.amount -= intensityForGarbage
                intensityForGarbage = 0
                driftAndLogGarbage(splitGarbage, tile, current, true)
                driftedGarbage.add(splitGarbage)
            }
        }
    }

    /**
     * Drifts garbage and log it
     */
    private fun driftAndLogGarbage(garbage: Garbage, tile: Tile, current: Current, isNewGarbage: Boolean) {
        DriftingUtil.driftGarbage(oceanMap, garbage, tile, current.direction, current.speed)
        val endTile = oceanMap.getGarbageTile(garbage)
        if (isNewGarbage || tile != endTile) {
            Logger.logGarbageDrift(
                garbage.type,
                garbage.id,
                garbage.amount,
                tile.id,
                endTile.id
            )
        }
    }

    /**
     * Drift ships
     */
    fun driftShips() {
        val driftedShips = mutableSetOf<Ship>()
        for (tile in sortedCurrentTiles) {
            val current = tile.current
            if (current != null) driftShipsOnTile(tile, current, driftedShips)
        }
    }

    /**
     * Drift ships on a tile
     */
    private fun driftShipsOnTile(tile: Tile, current: Current, driftedShips: MutableSet<Ship>) {
        val shipsToDrift = oceanMap.getShipsOnTile(tile)
            .filter { it !in driftedShips }
            .take(current.intensity)
        for (ship in shipsToDrift) {
            val candidateDriftTiles = DriftingUtil.getCandidateDriftTiles(
                oceanMap,
                tile,
                current.direction,
                current.speed
            )
            val endTile = candidateDriftTiles.first()
            driftedShips.add(ship)
            if (tile != endTile) {
                oceanMap.moveShip(ship, endTile)
                Logger.logShipDrift(ship.id, tile.id, endTile.id)
            }
        }
    }
}
