package de.unisaarland.cs.se.selab.event

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.control.DriftingUtil
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.Direction
import de.unisaarland.cs.se.selab.parser.JsonKeys

/**
 * StormEvent will drift all garbage, which lies on the tile(location) within the radius,
 * to the direction and distance. It'll not drift any ships.
 */
class StormEvent(
    private val radius: Int,
    val speed: Int,
    val direction: Int,
    private val location: Tile,
    id: Int,
    tick: Int
) : Event(id, tick) {
    private val moveDirection = Direction.createDirection(direction)
    val affectedGarbage = mutableListOf<Garbage>()

    override fun execute(oceanMap: OceanMap) {
        val affectedTiles = oceanMap.getTilesInRadius(location, radius)
        Logger.logEvent(id, JsonKeys.STORM)
        for (tile in affectedTiles) {
            moveGarbage(oceanMap, tile)
        }
    }

    /**
     * If there is garbage on the tile,
     * move it according to the storm direction and speed.
     */
    private fun moveGarbage(oceanMap: OceanMap, tile: Tile) {
        for (garbage in oceanMap.getGarbageOnTile(tile)) {
            if (garbage !in affectedGarbage) {
                DriftingUtil.driftGarbage(oceanMap, garbage, tile, moveDirection, speed)
                affectedGarbage.add(garbage)
            }
        }
    }
}
