package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.ShipType
import java.util.SortedSet

/**
 * Class to handle visibility and information of corporations
 */
class VisibilityHandler(
    private val oceanMap: OceanMap,
    private val corporations: List<Corporation>
) {
    private fun getTilesInShipVisibility(ship: Ship): Set<Tile> {
        if (ship.type != ShipType.REFUELING) {
            val shipTile = oceanMap.getShipTile(ship)
            val tilesInShipVisibility = oceanMap.getTilesInRadius(shipTile, ship.visibilityRange)
            return tilesInShipVisibility.toSet()
        } else {
            return emptySet()
        }
    }

    private fun getTilesInCorpVisibility(corporation: Corporation): Set<Tile> {
        return corporation.ships.flatMapTo(mutableSetOf()) { getTilesInShipVisibility(it) }
    }

    private fun getTileToGarbage(garbage: Collection<Garbage>): Map<Tile, Collection<Garbage>> {
        return garbage.filter { it in oceanMap.garbageToTile.keys }.groupBy { oceanMap.getGarbageTile(it) }
    }

    private fun MutableMap<Tile, SortedSet<Garbage>>.update(tileToGarbage: Map<Tile, Collection<Garbage>>) {
        for ((tile, garbageOnTile) in tileToGarbage) {
            val originalGarbageOnTile = getOrPut(tile) { sortedSetOf<Garbage>() }
            originalGarbageOnTile.addAll(garbageOnTile)
        }
    }

    /**
     * Update corporation information
     */
    fun updateCorpInformation(corporation: Corporation) {
        val tilesInCorpVisibility = getTilesInCorpVisibility(corporation)
        val visibleTileToGarbage = oceanMap.tileToGarbage
            .filterKeys { it in tilesInCorpVisibility }
            .mapValues { it.value.toSortedSet() }
        val tileToTrackedGarbage = getTileToGarbage(corporation.trackedGarbage)
        val garbageInCorpVisibility = visibleTileToGarbage.values
            .flatMapTo(mutableSetOf()) { it }
            .apply { addAll(corporation.trackedGarbage) }

        corporation.information.keys.removeIf { it in tilesInCorpVisibility }
        for (garbageOnTile in corporation.information.values) {
            garbageOnTile.removeIf { it in garbageInCorpVisibility }
        }
        corporation.information.values.removeIf { it.isEmpty() }
        corporation.information.putAll(visibleTileToGarbage)
        corporation.information.update(tileToTrackedGarbage)
    }

    /**
     * Update corporation information based on coordination
     */
    fun updateCorpInformation(sender: Corporation, receiver: Corporation) {
        val garbageInReceiverInformation = receiver.information.values.flatten().toSet()
        val coordinatingInformation = sender.information
            .mapValues { it.value.filter { garbage -> garbage !in garbageInReceiverInformation } }
            .filterValues { it.isNotEmpty() }
        receiver.information.update(coordinatingInformation)
    }

    /**
     * Update the information of all corporations with the locations of garbage
     * that was affected or created by an event
     */
    fun globalUpdateCorpInformation(affectedGarbage: Collection<Garbage>) {
        val tileToAffectedGarbage = getTileToGarbage(affectedGarbage)
        for (corporation in corporations) {
            for (garbageOnTile in corporation.information.values) {
                garbageOnTile.removeIf { it in affectedGarbage }
            }
            corporation.information.values.removeIf { it.isEmpty() }
            corporation.information.update(tileToAffectedGarbage)
        }
    }

    /**
     * Get garbage in ship visibility
     */
    fun getGarbageInShipVisibility(ship: Ship): Map<Tile, Collection<Garbage>> {
        val tilesInShipVisibility = getTilesInShipVisibility(ship)
        return oceanMap.tileToGarbage.filterKeys { it in tilesInShipVisibility }
    }

    /**
     * Get garbage in corp visibility
     */
    fun getGarbageInCorpVisibility(ship: Ship): Map<Tile, Collection<Garbage>> {
        val tileToTrackedGarbage = getTileToGarbage(ship.corporation.trackedGarbage)
        val tilesInCorpVisibility = getTilesInCorpVisibility(ship.corporation)
        val garbageInCorpVisibility = oceanMap.tileToGarbage
            .filterKeys { it in tilesInCorpVisibility }.toMutableMap()
            .also { it.update(tileToTrackedGarbage) }
        return garbageInCorpVisibility
    }

    /**
     * Get garbage in corp information
     */
    fun getGarbageInCorpInformation(ship: Ship): Map<Tile, Collection<Garbage>> {
        return ship.corporation.information
    }

    /**
     * Get ships in corp visibility
     */
    fun getShipsInCorpVisibility(ship: Ship): Map<Tile, Collection<Ship>> {
        val tilesInCorpVisibility = getTilesInCorpVisibility(ship.corporation)
        return oceanMap.tileToShip.filterKeys { it in tilesInCorpVisibility }
    }
}
