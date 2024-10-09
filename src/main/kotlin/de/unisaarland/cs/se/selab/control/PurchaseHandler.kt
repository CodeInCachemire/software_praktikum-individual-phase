package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.ShipType

/**
 * BurchaseHandler hehe is handling za order and za burchase.
 */
class PurchaseHandler(
    private val oceanMap: OceanMap,
    private val pathFinder: PathFinder
) {
    /**
     * It is za burchase phase, where we will see if we can burchase anything or not.
     */
    fun purchasePhase(corporation: Corporation) {
        if (corporation.isBuyingShip) {
            updateOrderStatus(corporation)
            return
        }
        if (oceanMap.getPurchasingStations(corporation).isEmpty()) {
            return
        }
        val cheapestCost = oceanMap.getPurchasingStationCheapestCost(corporation)
        if (corporation.credits < cheapestCost) {
            return
        }
        if (corporation.ships.none { it.type == ShipType.COORDINATING }) {
            return
        }
        sendAndChooseShip(corporation)
    }

    /**
     * We get the cheapest shipyard station we can go to that is the closest.
     */
    fun cheapeastShipyardStation(corporation: Corporation): Tile? {
        val cheapestCost = oceanMap.getPurchasingStationCheapestCost(corporation)
        val stationsWithLowestCost = oceanMap.getPurchasingStations(corporation)
            .filter { it.shipyardStation != null && it.shipyardStation.shipCost == cheapestCost }.sortedBy { it.id }
        val firstHarbor = stationsWithLowestCost.firstOrNull() ?: return null
        return oceanMap.harborToTile[firstHarbor]
    }

    /**
     * We send and choose if we even have any.
     */
    fun sendAndChooseShip(corporation: Corporation) {
        val listOfCordShips = corporation.ships.filter { it.type == ShipType.COORDINATING }
        val theChosenOne = listOfCordShips.filter { findingTheChosenOne(it) }.sortedBy { it.id }.firstOrNull()
        if (theChosenOne == null) {
            return
        }
        corporation.assignedBuyingShipId = theChosenOne.id
    }

    /**
     * Update za order and leave the shawarma station, sorry I mean za shipyard station.
     */
    fun updateOrderStatus(corporation: Corporation) {
        if (corporation.purchaseCounter != 0 && corporation.purchaseCounter > 0) {
            corporation.purchaseCounter -= 1
            if (corporation.purchaseCounter == 0) {
                corporation.isBuyingShip = false
            }
        }
    }

    /**
     * valid ships we can choose
     */
    private fun findingTheChosenOne(ship: Ship): Boolean {
        // ship with the lowest id that is not currently leaving a restricted area, doing
        // a task, or on the way to refuel and has an existing path to the harbor with the shipyard
        // station.
        if (ship.behaviour == Behaviour.ESCAPING ||
            ship.task != null || ship.behaviour == Behaviour.REFUELING
        ) {
            return false
        }
        // ship is considered valid if it is not restricted nor has a task and has a valid path.
        val stationTile = cheapeastShipyardStation(ship.corporation) ?: return false
        return pathFinder.isReachable(oceanMap.getShipTile(ship), setOf(stationTile))
    }

    /**
     * Clear all purchase related information
     */
    fun clearAll(corporation: Corporation) {
        corporation.isBuyingShip = false
        corporation.purchaseCounter = -1
        corporation.assignedBuyingShipId = -1
        corporation.storeBoughtTileNdShip = Pair(null, null)
    }
}
