package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.task.CooperationTask

/**
 * Handles various ship-related tasks such as attaching trackers, collecting garbage,
 * refueling, unloading, and cooperation between corporations.
 */
class ShipHandler(
    private val oceanMap: OceanMap,
    private val visibilityHandler: VisibilityHandler,
    private val corporations: List<Corporation>
) {

    /**
     * Handles the tracker attachment for the given corporation.
     */
    fun attachTracker(corporation: Corporation) {
        for (ship in corporation.ships) {
            if (ship.canAttachTracker()) {
                attachTracker(ship)
            }
        }
    }

    /**
     * Attaches tracker to all garbage piles that are on the tile of the given ship.
     */
    private fun attachTracker(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        for (garbage in oceanMap.getGarbageOnTile(shipTile)) {
            if (garbage !in ship.corporation.trackedGarbage) {
                ship.corporation.trackedGarbage.add(garbage)
                Logger.logAttachTracker(ship.corporation.id, garbage.id, ship.id)
            }
        }
    }

    /**
     * Handles the collection phase for the given corporation.
     */
    fun collectionPhase(corporation: Corporation) {
        Logger.logCorpCollectGarbage(corporation.id)
        for (ship in corporation.ships) {
            if (ship.canCollect()) {
                collectGarbage(ship)
            }
        }
    }

    /**
     * Collects garbage on the tile of the given ship. The garbage will be collected and logged
     * in the following order: first by garbage type (PLASTIC, then OIL, then CHEMICALS),
     * and second by garbage id.
     *
     * The ship will stop collecting if it encounters garbage that it could theoretically pick up
     * (because it is a collecting ship of that garbage type or has a container for it),
     * but is unable to do so due to capacity limitations or other conditions.
     *
     * @see canCollectPlastic
     */
    private fun collectGarbage(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val garbageOnTile = oceanMap.getGarbageOnTile(shipTile)
        val garbagePerType = garbageOnTile.groupBy { it.type }
        val canCollectPlastic = canCollectPlastic(ship)

        for (garbageType in GarbageType.entries) {
            for (garbage in garbagePerType[garbageType].orEmpty()) {
                val couldCollect = collectGarbage(ship, garbage, canCollectPlastic)
                if (!couldCollect) break
            }
        }
    }

    /**
     * Checks if the ships on the tile of the given ship have a sufficient collective capacity
     * to pick up all plastic garbage on that tile.
     */
    private fun canCollectPlastic(ship: Ship): Boolean {
        val shipTile = oceanMap.getShipTile(ship)
        val garbageOnTile = oceanMap.getGarbageOnTile(shipTile)
        val shipsOnTile = oceanMap.getShipsOnTile(shipTile)
        val plasticGarbageSum = SumsPerTypeUtil.getGarbageSumsPerType(garbageOnTile)[GarbageType.PLASTIC] ?: 0
        val plasticCapacitySum = SumsPerTypeUtil.getCapacitySumsPerType(shipsOnTile)[GarbageType.PLASTIC] ?: 0
        return plasticCapacitySum >= plasticGarbageSum
    }

    /**
     * Collects and logs the given garbage with the given ship. Returns false if the ship could theoretically
     * pick up the garbage (because it is a collecting ship of that garbage type or has a container for it),
     * but is unable to do so due to capacity limitations or other conditions. Returns true otherwise.
     */
    private fun collectGarbage(ship: Ship, garbage: Garbage, canCollectPlastic: Boolean): Boolean {
        val shipCapacity = ship.garbageCapacity[garbage.type] ?: return true
        val isPlastic = garbage.type == GarbageType.PLASTIC
        if (shipCapacity == 0 || (isPlastic && !canCollectPlastic)) return false
        if (shipCapacity >= garbage.amount) {
            ship.garbageCapacity[garbage.type] = shipCapacity - garbage.amount
            oceanMap.removeGarbage(garbage)
            Logger.logCollectGarbage(
                ship.id,
                garbage.amount,
                garbage.id,
                garbage.type,
                ship.corporation.id
            )
        } else if (shipCapacity > 0) {
            ship.garbageCapacity[garbage.type] = 0
            garbage.amount -= shipCapacity
            Logger.logCollectGarbage(
                ship.id,
                shipCapacity,
                garbage.id,
                garbage.type,
                ship.corporation.id
            )
        }
        return true
    }

    /**
     * Handles the cooperation phase for the given corporation.
     */
    fun cooperationPhase(corporation: Corporation) {
        Logger.logCoopStart(corporation.id)

        for (ship in corporation.ships) {
            val shipTile = oceanMap.getShipTile(ship)
            for (otherShip in oceanMap.getShipsOnTile(shipTile)) {
                if (ship.canCooperateWith(otherShip)) {
                    visibilityHandler.updateCorpInformation(otherShip.corporation, corporation)
                    corporation.lastCooperated = otherShip.corporation
                    Logger.logCoopWith(corporation.id, otherShip.corporation.id, ship.id, otherShip.id)
                }
            }
            performCooperationTask(ship)
        }
    }

    /**
     * If the given ship has a cooperation task and waited at the target tile for one tick,
     * then it will now cooperate with every corporation that has a harbor at the target tile.
     */
    private fun performCooperationTask(ship: Ship) {
        val shipTask = ship.task ?: return
        if (shipTask is CooperationTask && shipTask.finished) {
            val shipTile = oceanMap.getShipTile(ship)
            // Get all corporations with harbors at the target tile.
            for (corpWithHarbour in corporations.filter { shipTile in it.harbors }) {
                visibilityHandler.updateCorpInformation(ship.corporation, corpWithHarbour)
            }
            // The task is now completed.
            ship.task = null
        }
    }

    /**
     * Handles the refueling phase for the given corporation.
     */
    fun refuelingPhase(corporation: Corporation) {
        Logger.logRefuelStart(corporation.id)
        for (ship in corporation.ships) {
            refuelShip(ship)
        }
    }

    /**
     * Refuel the given ship.
     */
    private fun refuelShip(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        if (ship.isRefueling()) {
            ship.fuel = ship.maxFuel
            Logger.logRefueling(ship.id, shipTile.id)
            ship.waitingAtHarbor = false
            if (ship.garbageCapacity.any { it.value == 0 }) {
                // If we have a full garbage container,
                // go unloading in the next tick.
                ship.behaviour = Behaviour.UNLOADING
            } else {
                ship.behaviour = Behaviour.DEFAULT
            }
        }
    }

    /**
     * Handles the unloading phase for the given corporation.
     */
    fun unloadingPhase(corporation: Corporation) {
        for (ship in corporation.ships) {
            unloadShip(ship)
        }
    }

    /**
     * Unload the given ship.
     */
    private fun unloadShip(ship: Ship) {
        val harbour = oceanMap.getShipTile(ship)

        if (ship.isUnloading()) {
            for (garbageType in GarbageType.entries) {
                if (ship.garbageCapacity[garbageType] == 0) {
                    val garbageTypeCapacity = ship.maxGarbageCapacity.getValue(garbageType)
                    ship.garbageCapacity[garbageType] = garbageTypeCapacity
                    Logger.logUnload(ship.id, garbageTypeCapacity, garbageType, harbour.id)
                }
            }
            ship.behaviour = Behaviour.DEFAULT
            ship.waitingAtHarbor = false
        }
    }
}
