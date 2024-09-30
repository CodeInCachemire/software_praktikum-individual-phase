package de.unisaarland.cs.se.selab.event

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.parser.JsonKeys

/**
 * this event blocks a tile for a specific duration
 */
class RestrictionEvent(
    private val radius: Int,
    private var duration: Int,
    val location: Tile,
    id: Int,
    tick: Int
) : DurationEvent(id, tick) {

    override fun execute(oceanMap: OceanMap) {
        Logger.logEvent(id, JsonKeys.RESTRICTION)
        for (tile in oceanMap.getTilesInRadius(location, radius)) {
            tile.numRestrictions++
        }
    }

    override fun update(oceanMap: OceanMap): Boolean {
        duration--
        return if (duration == 0) {
            for (tile in oceanMap.getTilesInRadius(location, radius)) {
                tile.numRestrictions--
            }
            true
        } else {
            false
        }
    }
}
