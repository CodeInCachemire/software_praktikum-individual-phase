package de.unisaarland.cs.se.selab.event

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.parser.JsonKeys
import kotlin.math.min

/**
 * Oil event will create new oil garbage to the tiles that are affected by this event.
 */
class OilEvent(
    private val radius: Int,
    val amount: Int,
    val location: Tile,
    id: Int,
    tick: Int
) : Event(id, tick) {
    val createdGarbage = mutableListOf<Garbage>()

    override fun execute(oceanMap: OceanMap) {
        Logger.logEvent(id, JsonKeys.OIL_SPILL)
        for (tile in oceanMap.getTilesInRadius(location, radius).sorted()) {
            val oilGarbageOnTile = oceanMap.getGarbageOnTile(tile)
                .filter { it.type == GarbageType.OIL }
            val oilAmountSum = oilGarbageOnTile.sumOf { it.amount }
            val createdAmount = min(amount, Constants.MAX_OIL_AMOUNT - oilAmountSum)
            if (createdAmount > 0) {
                val oil = Garbage(
                    oceanMap.getMaxGarbageId() + 1,
                    GarbageType.OIL,
                    createdAmount
                )
                oceanMap.addGarbage(oil, tile)
                createdGarbage.add(oil)
            }
        }
    }
}
