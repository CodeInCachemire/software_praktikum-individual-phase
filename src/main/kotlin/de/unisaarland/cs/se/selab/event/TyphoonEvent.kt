package de.unisaarland.cs.se.selab.event

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Logger.logEvent
import de.unisaarland.cs.se.selab.Logger.printer
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.TileType
import de.unisaarland.cs.se.selab.parser.JsonKeys
import java.util.*

const val ACCELERATION_DECREASE = 4
const val MIN_VELOCITY = 40
const val STRENGTH1 = 1
const val STRENGTH2 = 2
const val STRENGTH3 = 3
const val STRENGTH4 = 4

/**
 * Oil event will create new oil garbage to the tiles that are affected by this event.
 */
class TyphoonEvent(
    private val radius: Int,
    private val strength: Int,
    val location: Tile,
    id: Int,
    tick: Int
) : Event(id, tick) {
    val createdGarbage = mutableListOf<Garbage>()

    override fun execute(oceanMap: OceanMap) {
        val affectedTiles = oceanMap.getTilesInRadius(location, radius)
        val affectedShips = sortedSetOf<Ship>()
        affectedTiles.forEach {
                tile ->
            affectedShips.addAll(oceanMap.getShipsOnTile(tile))
        }
        logEvent(id, JsonKeys.TYPHOON)
        logTyphoon(id, radius, location.id, affectedShips)
        when (strength) {
            STRENGTH1 -> affectedShips.forEach { ship -> increaseFuelConsumption(ship) }
            STRENGTH2 -> affectedShips.forEach { ship ->
                increaseFuelConsumption(ship)
                destroyTelescopesAndRadios(ship)
            }
            STRENGTH3 -> affectedShips.forEach { ship ->
                increaseFuelConsumption(ship)
                destroyTelescopesAndRadios(ship)
                unloadGarbageShip(ship, oceanMap)
            }
            STRENGTH4 -> affectedShips.forEach { ship ->
                increaseFuelConsumption(ship)
                destroyTelescopesAndRadios(ship)
                unloadGarbageShip(ship, oceanMap)
                damageShip(ship)
            }
        }
    }

    /**
     * Increases Fuel Consumption
     */
    private fun increaseFuelConsumption(ship: Ship) {
        ship.fuelConsumption *= 2
        ship.fuelConsumptionPerTile = ship.fuelConsumption * Constants.TILE_DISTANCE
    }

    /**
     * Destroy Telescope and Radios
     */
    private fun destroyTelescopesAndRadios(ship: Ship) {
        if (RewardType.RADIO in ship.reward || RewardType.TELESCOPE in ship.reward) {
            ship.reward.removeAll(listOf(RewardType.TELESCOPE, RewardType.RADIO))
            ship.visibilityRange = ship.visibilityRangeOriginal
        }
    }

    /**
     * Unload Garbage Ship
     */
    private fun unloadGarbageShip(ship: Ship, oceanMap: OceanMap) {
        // So we will iterate through every garbage type in the ORDER PLASTIC, OIL, CHEMICALS
        for (garbageType in GarbageType.entries) {
            // IF a garbage type is in the ship's garbage capacity type then we continue
            if (garbageType in ship.garbageCapacity.keys) {
                // we first get the maxcapacity for that garbage capacity
                val garbageTypeCapacityMax = ship.maxGarbageCapacity.getValue(garbageType)
                val currentGarbageCapacity = ship.garbageCapacity.getValue(garbageType)
                val amountCollectedForType = garbageTypeCapacityMax - currentGarbageCapacity
                ship.garbageCapacity[garbageType] = garbageTypeCapacityMax
                unloadGarbageTyphoon(garbageType, amountCollectedForType, oceanMap, ship)
            }
        }
    }

    /**
     * Create And Unload Garbage
     */
    private fun unloadGarbageTyphoon(
        garbageType: GarbageType,
        amountCollectedForType: Int,
        oceanMap: OceanMap,
        ship: Ship
    ) {
        if (amountCollectedForType > 0) {
            val garbageNew =
                Garbage(
                    oceanMap.getMaxGarbageId() + 1,
                    garbageType,
                    amountCollectedForType
                )
            if (garbageType != GarbageType.CHEMICALS &&
                oceanMap.getShipTile(ship).type != TileType.DEEP_OCEAN
            ) {
                oceanMap.addGarbage(garbageNew, oceanMap.getShipTile(ship))
                createdGarbage.add(garbageNew)
            } else {
                oceanMap.incMaxGarbageID()
            }
        }
    }

    /**
     * Damages the ship
     */
    private fun damageShip(ship: Ship) {
        ship.acceleration = (ship.acceleration - ACCELERATION_DECREASE).coerceAtLeast(1)
        if (ship.maxVelocity > MIN_VELOCITY) {
            ship.maxVelocity = MIN_VELOCITY
        }
        ship.isDamaged = true
    }

    /**
     * Log event typhoon.
     */
    private fun logTyphoon(eventId: Int, radius: Int, tileID: Int, sortedShips: SortedSet<Ship>) {
        val sortedShipsIDs = sortedShips.map { it.id }.joinToString(", ")
        printer.println(
            "Event: Typhoon $eventId at tile $tileID with radius $radius affected" +
                " ships: $sortedShipsIDs."
        )
        printer.flush()
    }
}
