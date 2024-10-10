package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.ShipyardStation
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.data.UnloadingStation
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType
import de.unisaarland.cs.se.selab.parser.SimulationData
import de.unisaarland.cs.se.selab.task.CooperationTask

/**
 * Handles various ship-related tasks such as attaching trackers, collecting garbage,
 * refueling, unloading, and cooperation between corporations.
 */
class ShipHandler(
    private val oceanMap: OceanMap,
    private val visibilityHandler: VisibilityHandler,
    private val corporations: List<Corporation>,
    private val purchaseHandler: PurchaseHandler,
    private val simulationData: SimulationData
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
        // RUN FOR REFUELING SHIPS
        for (ship in corporation.ships) {
            val shipTile = oceanMap.getShipTile(ship)
            if (!ship.receivingRefuel && !refuelingShipPresentOnTile(ship)) {
                val shipOnRefuelTile = shipTile in oceanMap.getRefuelingStationHarborTiles()
                if ((ship.returnToRefuel || ship.waitingAtARefuelingStation) && shipOnRefuelTile) {
                    refuelAndRefillShip(ship)
                }
            }
        }
        for (ship in corporation.ships.sortedBy { it.id }) { // .sortedBy { it.id }
            val allowRefuelShip = ship.behaviour != Behaviour.REFUELING &&
                !oceanMap.getShipTile(ship).restricted && ship.type == ShipType.REFUELING &&
                !ship.receivingRefuel
            if (allowRefuelShip) {
                refuelingByAAShip(ship)
            }
        }
    }

    /**
     * REFUELING ANOTHER SHIP IF YOU YOURSELF ARE A REFUELING SHIP
     */
    /*private fun refuelingByAShip(refShip: Ship) {
        val refShipTile = oceanMap.getShipTile(refShip) // t
        val refuelCapacity = refShip.currentRefuelingCapacity
        if (refShip.activeRefueling && refShip.shipToRefuel != null) {
            val shipREFUEL = refShip.shipToRefuel ?: return
            val shipTile = oceanMap.getShipTile(shipREFUEL)
            if (shipREFUEL.receivingRefuel && refShipTile == shipTile) {
                refShip.tickCounter -= 1
                if (refShip.tickCounter == 0) {
                    val amountToReduce = shipREFUEL.maxFuel - shipREFUEL.fuel
                    shipREFUEL.fuel = shipREFUEL.maxFuel
                    shipREFUEL.behaviour = Behaviour.DEFAULT
                    shipREFUEL.receivingRefuel = false
                    refShip.currentRefuelingCapacity -= amountToReduce
                    refShip.shipToRefuel = null
                    refShip.activeRefueling = false
                    refShip.tickCounter = refShip.getOriginalRefuelTime()
                    Logger.logReuelingByShipEnd(refShip.id, shipREFUEL.id)
                    return
                }
                return
            }
        }
        val shipProspects = oceanMap.getShipsOnTile(refShipTile)
            .filter {
                it.fuel < it.maxFuel / 2 && it.corporation.id == refShip.corporation.id
                refuelCapacity >= it.maxFuel - it.fuel
                it.id != refShip.id && !it.receivingRefuel &&
                    (
                        !it.isRefueling() || (
                            oceanMap.getShipTile(it) in oceanMap.getRefuelingStationHarborTiles() &&
                                it.behaviour == Behaviour.REFUELING
                            )
                        )
            }
        val refuelThisShip = shipProspects.minByOrNull { it.id }
        if (refuelThisShip != null) {
            // refShip.isClaimedShip = null
            refShip.velocity = 0
            refShip.activeRefueling = true
            refShip.shipToRefuel = refuelThisShip
            refuelThisShip.velocity = 0
            refuelThisShip.receivingRefuel = true
            refuelThisShip.behaviour = Behaviour.DEFAULT
            // refuelThisShip.tickCounter = refuelThisShip.getOriginalRefuelTime()
            // refuelThisShip.tickCounter = refuelThisShip.getOriginalRefuelTime()
            Logger.logReuelingByShipStart(refShip.id, refuelThisShip.id)
        }
    }*/

    /**
     * Refueling by a ship
     */
    fun refuelingByAAShip(refShip: Ship) {
        // COMPLETE CODE REWRITE
        val refShipTile = oceanMap.getShipTile(refShip)
        if (refShip.activeRefueling) {
            activeRefueling(refShip)
        } else {
            attemptRefueling(refShip, refShipTile)
        }
    }

    /**
     * ActiveRefueling
     */
    fun activeRefueling(refShip: Ship) {
        val refShipTile = oceanMap.getShipTile(refShip)
        val targetShip = refShip.shipToRefuel ?: return
        val targetShipTile = oceanMap.getShipTile(targetShip)
        if (targetShip.receivingRefuel && refShipTile == targetShipTile) {
            refShip.tickCounter -= 1
            if (refShip.tickCounter == 0) {
                val fuelNeeded = targetShip.maxFuel - targetShip.fuel
                targetShip.fuel = targetShip.maxFuel
                refShip.currentRefuelingCapacity -= fuelNeeded
                targetShip.behaviour = Behaviour.DEFAULT
                Logger.logReuelingByShipEnd(refShip.id, targetShip.id)
                refShip.shipToRefuel = null
                refShip.activeRefueling = false
                refShip.tickCounter = refShip.getOriginalRefuelTime()
                targetShip.receivingRefuel = false
                targetShip.willBeRefueled = false
                // both ship cases are handled
            }
        } else {
            targetShip.receivingRefuel = false
            refShip.shipToRefuel = null
            refShip.tickCounter = refShip.getOriginalRefuelTime()
            refShip.activeRefueling = false
        }
    }

    /**
     * We attempt refueling now.
     */
    fun attemptRefueling(refShip: Ship, refShipTile: Tile) {
        val potentialTargets = oceanMap.getShipsOnTile(refShipTile)
            .filter { it.fuel < it.maxFuel / 2 }
            .filter { it.maxFuel - it.fuel <= refShip.currentRefuelingCapacity }
            .filter { !it.receivingRefuel } // could be our ship too} //TODO()
            .filter {
                !it.isRefueling() ||
                    (oceanMap.getShipTile(it) in oceanMap.getRefuelingStationHarborTiles() && it.returnToRefuel)
            }
            .filter { refShip.corporation.id == it.corporation.id && refShip.id != it.id }
        val targetShip = potentialTargets.minByOrNull { it.id }
        if (targetShip != null) {
            if (refShip.currentRefuelingCapacity < targetShip.maxFuel - targetShip.fuel) {
                return
            }
            refShip.velocity = 0
            refShip.activeRefueling = true
            refShip.tickCounter = refShip.getOriginalRefuelTime()
            refShip.shipToRefuel = targetShip
            // reset ship being refueled
            targetShip.velocity = 0 // TODO()
            targetShip.task = null
            targetShip.behaviour = Behaviour.DEFAULT
            targetShip.receivingRefuel = true
            Logger.logReuelingByShipStart(refShip.id, targetShip.id)
        }
    }

    /**
     * Refuel the given ship.
     */
    private fun refuelAndRefillShip(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val harbor = oceanMap.tileToHarbor.getValue(shipTile)
        val refuelingStation = harbor.refuelingStation ?: return
        if (ship.receivingRefuel) {
            return
        }
        if (!refuelingStation.stationNotClosed()) {
            return
        }
        if (ship.returnToRefuel) {
            ship.returnToRefuel = false
            // MOGGER METHODs
            val cost = refuelingStation.refuelCost
            /*if (refuelingShipPresentOnTileOFSHIP(ship)) {
                return
            }*/
            if (ship.corporation.credits >= cost) {
                ship.waitingAtARefuelingStation = true
                if (ship.type == ShipType.REFUELING && ship.isRefillingCapacity) {
                    Logger.logRefilling(ship.id, harbor.id, cost)
                } else {
                    Logger.logRefuelingShip(ship.id, harbor.id, cost)
                }
                ship.corporation.credits -= cost
            } else {
                Logger.logRefuelingFail(ship.id, harbor.id)
            }
        } else if (ship.isRefilling()) {
            ship.currentRefuelingCapacity = ship.getOriginalRefuelCap()
            Logger.logRefuelingFinished(ship.id, harbor.id)
            refuelingStation.incCount()
            ship.waitingAtARefuelingStation = false
            ship.isRefillingCapacity = false
            ship.behaviour = Behaviour.DEFAULT
            val unloadingStation = harbor.unloadingStation ?: return
            unloadCheck(ship, unloadingStation)
        } else if (ship.isRefueling()) {
            // val cost = harbor.refuelingStation.refuelCost
            ship.fuel = ship.maxFuel
            // ship.corporation.credits -= cost
            Logger.logRefuelingFinished(ship.id, harbor.id)
            refuelingStation.incCount()
            ship.waitingAtARefuelingStation = false
            ship.behaviour = Behaviour.DEFAULT
            val unloadingStation = harbor.unloadingStation ?: return
            unloadCheck(ship, unloadingStation)
        }
    }

    /**
     * Refueling Ship Is Present
     */
    private fun refuelingShipPresentOnTile(ship: Ship): Boolean {
        val shipTile = oceanMap.getShipTile(ship)
        val shipsOnTile = oceanMap.getShipsOnTile(shipTile)
        // forgot case of a ship being damaged it will never repair otherwis
        val shipFuelLessOrDamaged = ship.fuel < ship.maxFuel / 2
        // && ship.isDamaged
        // val shipFuelNeed = ship.maxFuel - ship.fuel
        if (shipFuelLessOrDamaged) {
            val refuelingShipExists = shipsOnTile
                .any { refShip ->
                    refShip.type == ShipType.REFUELING &&
                        refShip.id != ship.id &&
                        // refShip.currentRefuelingCapacity >= shipFuelNeed &&
                        !refShip.activeRefueling &&
                        !refShip.receivingRefuel && refShip.behaviour != Behaviour.ESCAPING
                }
            return refuelingShipExists
        }
        return false
    }

    /**
     * Unload Check
     */
    private fun unloadCheck(ship: Ship, unloadingStation: UnloadingStation) {
        if (ship.garbageCapacity.any { it.value == 0 && it.key in unloadingStation.garbageTypes }) {
            ship.waitingAtAUnloadingStation = true
            ship.behaviour = Behaviour.UNLOADING
        }
    }

    /**
     * Handles the unloading phase for the given corporation.
     */
    fun unloadingPhase(corporation: Corporation) {
        for (ship in corporation.ships) {
            val shipTile = oceanMap.getShipTile(ship)
            val harbor = oceanMap.tileToHarbor[shipTile] ?: continue
            val unloadingStation = harbor.unloadingStation ?: continue
            if (shipTile in corporation.harbors) {
                callUnload(ship, unloadingStation)
            }
        }
    }

    /**
     * Fun check harbor null and has unloadingStation
     */
    private fun callUnload(ship: Ship, unloadingStation: UnloadingStation) {
        /*if (ship.garbageCapacity.any { it.value == 0 }) {
            val needsToUnload = ship.garbageCapacity.filter { it.value == 0 }.keys.sortedBy { it.ordinal }
            if (needsToUnload.isNotEmpty()) {
                // val garbageToCheck = needsToUnload.first()
                // val shipOnUnloadTile = unloadingStation.garbageTypes.contains(garbageToCheck)*/
        val shipOnUnloadTile = ship.garbageCapacity
            .any { it.value == 0 && it.key in unloadingStation.garbageTypes }
        if ((ship.returnToUnload || ship.waitingAtAUnloadingStation) && shipOnUnloadTile) {
            unloadShip(ship)
        }
            /*}
        }*/
    }

    /**
     * Unload the given ship.
     */
    private fun unloadShip(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val harbor = oceanMap.tileToHarbor.getValue(shipTile)
        if (harbor.unloadingStation != null) {
            if (ship.returnToUnload) {
                ship.returnToUnload = false
                ship.waitingAtAUnloadingStation = true
            } else if (ship.isUnloading()) {
                unloadGarbageAtStation(ship, harbor.unloadingStation)
            }
        }
    }

    /**
     * Unloading function.
     */
    private fun unloadGarbageAtStation(ship: Ship, harborUnloadingStation: UnloadingStation) {
        val harborTile = oceanMap.getShipTile(ship)
        val harbor = oceanMap.tileToHarbor.getValue(harborTile)
        // val filterGarbagesWeCanUnload = ship.garbageCapacity
        //    .filter { it.value == 0 && it.key in harborUnloadingStation.garbageTypes }.keys.sortedBy { it.ordinal }
        for (garbageType in GarbageType.entries) {
            if (garbageType in harborUnloadingStation.garbageTypes) {
                if (ship.garbageCapacity[garbageType] == 0) {
                    val garbageTypeCapacity = ship.maxGarbageCapacity.getValue(garbageType)
                    ship.garbageCapacity[garbageType] = garbageTypeCapacity
                    val creditsEarned = garbageTypeCapacity * harborUnloadingStation.unloadReturn
                    ship.corporation.credits += creditsEarned
                    Logger.logUnload(ship.id, garbageTypeCapacity, garbageType, harbor.id, creditsEarned)
                }
            }
        }
        ship.behaviour = Behaviour.DEFAULT
        ship.waitingAtAUnloadingStation = false
    }

    /**
     * Handles the repairing phase for the given corporation.
     */
    fun repairingPhase(corporation: Corporation) {
        for (ship in corporation.ships) {
            val shipTile = oceanMap.getShipTile(ship)
            val shipOnRepairTile = shipTile in oceanMap.getRepairingStationHarborTiles()
            if ((ship.returnToRepair || ship.waitingAtAShipyard) && shipOnRepairTile) {
                repairShip(ship)
            }
        }
    }

    /**
     * Refuel the given ship.
     */
    private fun repairShip(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val harbor = oceanMap.tileToHarbor.getValue(shipTile)
        if (harbor.shipyardStation != null) {
            if (ship.returnToRepair) {
                ship.returnToRepair = false
                // MOGGER METHODs
                val cost = harbor.shipyardStation.repairCost
                if (ship.corporation.credits >= cost) {
                    ship.waitingAtAShipyard = true
                    Logger.logDamageRepairStart(ship.id, harbor.id, cost)
                    ship.corporation.credits -= cost
                }
            } else if (ship.isRepairing()) {
                // val cost = harbor.shipyardStation.repairCost
                ship.acceleration = ship.accelerationOriginal
                ship.maxVelocity = ship.maxVelocityOriginal
                // ship.corporation.credits -= cost
                ship.isDamaged = false
                Logger.logDamageRepairFinish(ship.id)
                ship.waitingAtAShipyard = false
                ship.behaviour = Behaviour.DEFAULT
                val unloadingStation = harbor.unloadingStation ?: return
                unloadCheck(ship, unloadingStation)
            }
        }
    }

    /**
     * Handles the purchasing phase for the given corporation.
     */
    fun purchasingPhase(corporation: Corporation) {
        if (corporation.purchaseCounter == 0 && !corporation.isBuyingShip) {
            deliverShip(corporation)
        } else if (!corporation.isBuyingShip) {
            orderShip(corporation)
        }
    }

    /**
     *
     */
    private fun orderShip(corporation: Corporation) {
        val ship = corporation.ships.find { it.id == corporation.assignedBuyingShipId }
        if (ship != null) {
            if (!ship.returnToPurchase) {
                return
            }
            val harbor = oceanMap.tileToHarbor[oceanMap.getShipTile(ship)]
            if (harbor != null && harbor.shipyardStation != null) {
                val shipYard = harbor.shipyardStation
                if (corporation.credits >= shipYard.shipCost) {
                    corporation.credits -= shipYard.shipCost
                    purchaseShip(ship, corporation, shipYard)
                } else {
                    purchaseHandler.clearAll(corporation)
                    ship.returnToPurchase = false
                }
            }
        }
    }

    /**
     * Purchase and create a ship
     */
    private fun purchaseShip(ship: Ship, corporation: Corporation, shipYard: ShipyardStation) {
        val shipTile = oceanMap.getShipTile(ship)
        val harbor = oceanMap.getHarborTile(shipTile)
        val maxVelocity = shipYard.maxVelocity
        val acceleration = shipYard.acceleration
        val maxFuel = shipYard.fuelCapacity
        val fuelConsumption = shipYard.fuelConsumption
        val refuelingCapacity = shipYard.refuelingCapacity
        val refuelingTime = shipYard.refuelingTime
        val counterDelivery = shipYard.deliveryTime
        val refuelingShip = Ship(
            oceanMap.maxShipID + 1,
            ShipType.REFUELING,
            corporation,
            maxVelocity,
            acceleration,
            maxFuel,
            fuelConsumption,
            visibilityRange = 0,
            maxGarbageCapacity = mutableMapOf()
        )
        oceanMap.maxShipID += 1
        refuelingShip.setOriginalRefuelCap(refuelingCapacity, refuelingTime)
        corporation.storeBoughtTileNdShip = Pair(shipTile, refuelingShip)
        corporation.purchaseCounter = counterDelivery
        corporation.isBuyingShip = true
        Logger.logPurchaseRefuelingShip(ship.id, refuelingShip.id, harbor.id, shipYard.shipCost)
        ship.returnToPurchase = false
        corporation.assignedBuyingShipId = -1
    }

    /**
     * Deliver ship
     */
    private fun deliverShip(corporation: Corporation) {
        if (corporation.purchaseCounter == 0 && !corporation.isBuyingShip) {
            val refuelingShipTile = corporation.storeBoughtTileNdShip.first
            val refuelingShip = corporation.storeBoughtTileNdShip.second
            if (refuelingShip != null && refuelingShipTile != null) {
                Logger.logPurchaseFinished(refuelingShip.id, corporation.id, refuelingShipTile.id)
                simulationData.ships[refuelingShip.id] = refuelingShip
                corporation.ships.add(refuelingShip)
                oceanMap.addShip(refuelingShip, refuelingShipTile)
                purchaseHandler.clearAll(corporation)
            }
        }
    }
}
