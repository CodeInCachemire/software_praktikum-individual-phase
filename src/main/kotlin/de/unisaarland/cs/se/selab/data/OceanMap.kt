package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.Direction
import de.unisaarland.cs.se.selab.enums.GarbageType
import java.util.*

/**
 * Ocean map
 */
data class OceanMap(
    val tiles: MutableMap<Coordinate, Tile>
) {
    val tileToGarbage = mutableMapOf<Tile, SortedSet<Garbage>>()
    val garbageToTile = mutableMapOf<Garbage, Tile>()

    val tileToShip = mutableMapOf<Tile, SortedSet<Ship>>()
    val shipToTile = mutableMapOf<Ship, Tile>() // needs handling
    val tileToHarbor = mutableMapOf<Tile, Harbor>()
    val harborToTile = mutableMapOf<Harbor, Tile>()
    val harborsMap: MutableMap<Int, Harbor> = mutableMapOf()
    val harborTiles: MutableSet<Tile> = mutableSetOf()

    private var maxGarbageId = 0
    var maxShipID = 0

    /**
     * Get tile by id
     */
    fun getTileByID(id: Int): Result<Tile> {
        for ((_, tile) in tiles) {
            if (tile.id == id) {
                return Result.success(tile)
            }
        }
        return Result.failure(Exception("Could not find tile with id $id"))
    }

    /**
     * Get tiles in radius
     */
    fun getTilesInRadius(tile: Tile, radius: Int): List<Tile> {
        val tilesInRadius = tile.coordinate
            .getNeighbours(radius)
            .mapNotNull { tiles[it] }
        return tilesInRadius
    }

    /**
     * Get neighbouring tiles
     */
    fun getNeighbouringTiles(tile: Tile): List<Tile> {
        return getTilesInRadius(tile, 1)
    }

    /**
     * Get neighbour
     */
    fun getNeighbour(tile: Tile, direction: Direction): Tile? {
        val neighbourCoordinate = tile.coordinate.getNeighbourCoordinate(direction)
        return tiles[neighbourCoordinate]
    }

    /**
     * Get ship tile
     */
    fun getShipTile(ship: Ship): Tile {
        return shipToTile.getValue(ship)
    }

    /**
     * Get ships on tile
     */
    fun getShipsOnTile(tile: Tile): Collection<Ship> {
        return tileToShip[tile].orEmpty()
    }

    /**
     * Get harbor on Tile
     */
    fun getHarborTile(tile: Tile): Harbor {
        return tileToHarbor.getValue(tile)
    }

    /**
     * Move ship
     */
    fun moveShip(ship: Ship, destination: Tile) {
        removeShip(ship)
        addShip(ship, destination)
    }

    /**
     * Add ship
     */
    fun addShip(ship: Ship, tile: Tile) {
        tileToShip.getOrPut(tile) { sortedSetOf() }.add(ship)
        shipToTile[ship] = tile
        if (ship.id > maxShipID) {
            maxShipID = ship.id
        }
    }

    /**
     * Remove ship
     */
    fun removeShip(ship: Ship) {
        val shipsOnTile = tileToShip.getValue(getShipTile(ship))
        if (shipsOnTile.size > 1) {
            shipsOnTile.remove(ship)
        } else {
            tileToShip.remove(getShipTile(ship))
        }
        shipToTile.remove(ship)
    }

    /**
     * Get garbage tile
     */
    fun getGarbageTile(garbage: Garbage): Tile {
        return garbageToTile.getValue(garbage)
    }

    /**
     * Get garbage on tile
     */
    fun getGarbageOnTile(tile: Tile): Collection<Garbage> {
        return tileToGarbage[tile].orEmpty()
    }

    /**
     * Move garbage
     */
    fun moveGarbage(garbage: Garbage, destination: Tile) {
        removeGarbage(garbage)
        addGarbage(garbage, destination)
    }

    /**
     * Add garbage
     */
    fun addGarbage(garbage: Garbage, tile: Tile) {
        tileToGarbage.getOrPut(tile) { sortedSetOf() }.add(garbage)
        garbageToTile[garbage] = tile
        if (garbage.id > maxGarbageId) {
            maxGarbageId = garbage.id
        }
    }

    /**
     * Remove garbage
     */
    fun removeGarbage(garbage: Garbage) {
        if (garbage !in garbageToTile) return
        val garbageOnTile = tileToGarbage.getValue(getGarbageTile(garbage))
        if (garbageOnTile.size > 1) {
            garbageOnTile.remove(garbage)
        } else {
            tileToGarbage.remove(getGarbageTile(garbage))
        }
        garbageToTile.remove(garbage)
    }

    /**
     * Checks if ship is still on map.
     */
    fun getShipExists(ship: Ship): Boolean {
        return ship in shipToTile
    }

    /**
     * Gets the maximum garbage id.
     */
    fun getMaxGarbageId(): Int {
        return maxGarbageId
    }

    /**
     * Gets the maximum garbage id.
     */
    fun incMaxGarbageID() {
        maxGarbageId += 1
    }

    /**
     * Get all refueling station harbors that are active
     */
    private fun getRefuelingStationHarbors(): Set<Harbor> {
        return harborsMap.values
            .filter {
                if (it.refuelingStation != null) { it.refuelingStation.stationNotClosed() } else { false }
            }
            .toSortedSet(compareBy { it.id })
    }

    /**
     * Get all refueling station harbor tiles
     */
    fun getRefuelingStationHarborTiles(): Set<Tile> {
        return getRefuelingStationHarbors().map { harborToTile.getValue(it) }.toSet()
    }

    /**
     * Get all refueling station harbors that are active
     */
    private fun getUnloadingStationHarbors(corpID: Int, garbageType: GarbageType): Set<Harbor> {
        return harborsMap.values.filter {
            if (it.unloadingStation != null) {
                garbageType in it.unloadingStation.garbageTypes
            } else {
                false
            }
        }.filter { corpID in it.corporations }.toSortedSet(compareBy { it.id })
    }

    /**
     * Get all refueling station harbor tiles
     */
    fun getUnloadingHarborTiles(corpId: Int, garbageType: GarbageType): Set<Tile> {
        return getUnloadingStationHarbors(corpId, garbageType).map { harborToTile.getValue(it) }.toSet()
    }

    /**
     * Get all refueling station harbors that are active
     */
    private fun getRepairingStationHarbors(): Set<Harbor> {
        return harborsMap.values
            .filter { it.shipyardStation != null }
            .toSortedSet(compareBy { it.id })
            /*
            .toSortedSet(compareBy { it.id })
            .minOf { it }*/
    }

    /**
     * Get all refueling station harbor tiles
     */
    fun getRepairingStationHarborTiles(): Set<Tile> {
        return getRepairingStationHarbors().map { harborToTile.getValue(it) }.toSet()
    }

    /**
     * Get all refueling station harbors that are active
     */
    fun getPurchasingStationCheapestCost(corporation: Corporation): Int {
        val validStations = harborsMap.values
            .filter { it.shipyardStation != null }
            .filter { corporation.id in it.corporations }.mapNotNull { it.shipyardStation }
        return validStations.minOf { it.shipCost }
    }

    /**
     * Get all refueling station harbors that are active
     */
    fun getPurchasingStations(corporation: Corporation): List<Harbor> {
        return harborsMap.values
            .filter { it.shipyardStation != null }
            .filter { corporation.id in it.corporations }
    }

    /**
     *
     */
    fun getRefuelingStationOnTile(tile: Tile): RefuelingStation? {
        if (tileToHarbor.getValue(tile).hasRefuelingStation) {
            return null
        } else {
            return tileToHarbor.getValue(tile).refuelingStation
        }
    }

    /**
     *
     */
    fun getUnloadingStationOnTile(tile: Tile): UnloadingStation? {
        if (tileToHarbor.getValue(tile).hasUnloadingStation) {
            return tileToHarbor.getValue(tile).unloadingStation
        } else {
            return null
        }
    }

    /**
     *
     */
    fun getShipyardStationOnTile(tile: Tile): ShipyardStation? {
        if (tileToHarbor.getValue(tile).hasShipyardStation) {
            return tileToHarbor.getValue(tile).shipyardStation
        } else {
            return null
        }
    }
}
