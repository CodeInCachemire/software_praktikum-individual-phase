package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType

/**
 * Handles the movement logic for ships during the movement phase.
 * Manages ship behavior, such as when to refuel, default behaviour and escaping restrictions.
 */
class MovementHandler(
    private val pathFinder: PathFinder,
    private val oceanMap: OceanMap,
    private val visibilityHandler: VisibilityHandler,
    private val purchaseHandler: PurchaseHandler
) {

    /**
     * Handles the movement phase for the given corporation.
     */
    fun movementPhase(corporation: Corporation) {
        Logger.logStartMove(corporation.id)
        for (ship in corporation.ships) {
            val shipTile = oceanMap.getShipTile(ship)
            if (shipTile in oceanMap.harborTiles) { // ////////THIS WILL CHANGE MUST CHANGE TO CORRECT HARBOR
                // checkWhichHarbors(ship, shipTile) // if set to true then we execute in this phase
                ship.waitingAtHarbor = true // TODO() CHECK THIS AGAIN
                unloadingCheck(ship)
            }
            ship.accelerate()
            moveShip(ship, shipTile)

            behaviourChecks(ship)

            if (ship.type != ShipType.REFUELING) {
                ship.task?.update(ship, oceanMap, movementPhase = true)
            }
            /*
            if (ship.type == ShipType.REFUELING && ship.behaviour == Behaviour.DEFAULT) {
                val currentTile = oceanMap.getShipTile(ship)
                val shipsOnTile = oceanMap.getShipsOnTile(currentTile)
                val foundShip = shipsOnTile.filter {
                    !currentTile.restricted && it.id != ship.id && allowedToRefuel(ship, it)
                }.minByOrNull { it.id }
                if (foundShip != null) {
                    // foundShip.shipIsClaimed = true
                    ship.isClaimedShip = foundShip
                }
            }*/
        }
    }

    /**
     * Unloading Test Report
     */
    fun unloadingCheck(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val harbor = oceanMap.tileToHarbor[shipTile]
        if (harbor != null) {
            if (harbor.hasUnloadingStation) {
                ship.waitingAtAUnloadingStation = true
            }
        }
    }

    /**
     * Behaviour checks
     */
    fun behaviourChecks(ship: Ship) {
        if (ship.behaviour != Behaviour.REFUELING &&
            ship.behaviour != Behaviour.UNLOADING && // TODO() CHECK THIS AGAIN
            ship.behaviour != Behaviour.REPAIRING
        ) {
            ship.waitingAtHarbor = false // TODO() we set to false here
        }
        if (ship.behaviour != Behaviour.REFUELING) {
            ship.waitingAtARefuelingStation = false
        }
        if (ship.behaviour != Behaviour.UNLOADING) {
            ship.waitingAtAUnloadingStation = false
        }
        if (ship.behaviour != Behaviour.REPAIRING) {
            ship.waitingAtAShipyard = false
        }
    }

    /**
     * Determines the behaviour and moves the ship for this tick.
     */
    private fun moveShip(ship: Ship, shipTile: Tile) {
        val onRestriction = shipTile.restricted
        val needsToRefuel = ship.behaviour == Behaviour.REFUELING
        val needsToRepair = ship.isDamaged
        val hasTask = ship.task != null
        val needsToUnload = ship.garbageCapacity.any { it.value == 0 }

        // Conditions are prioritized top to bottom,
        // with higher priority cases listed first.
        when { // TODO()
            onRestriction -> escapeRestriction(ship)
            refuelingShipPresentOnTile(ship) -> moveNoWhere(ship)
            ship.receivingRefuel -> moveNoWhere(ship)
            needsToRefuel -> moveToRefuel(ship)
            needsToRepair -> moveToRepair(ship)
            hasTask -> moveToTask(ship)
            needsToUnload -> moveToUnload(ship)
            else -> moveShipDefault(ship)
        }
    }

    /**
     * Do not move anywhere this tick since refueling ship is on tile
     */
    private fun moveNoWhere(ship: Ship) {
        ship.task = null
        moveAlongPath(ship, emptyList())
    }

    /**
     * Moves the ship along the path by determining how far it can travel
     * and what the intermediate destination would be.
     * Also logs the ship movement and adjusts velocity if the destination is reached or no path is found.
     */
    // Redirects to a harbor if refueling is needed.
    private fun moveAlongPath(ship: Ship, path: List<Tile>) {
        if (path.isEmpty()) {
            ship.velocity = 0
            return
        }

        // Determine how far we can go this tick and
        // what the intermediate destination on our path would be.
        val distance = minOf(
            ship.getDistanceWithVelocity(),
            ship.getDistanceWithFuel(),
            path.size - 1
        )

        val intermediateDestination = path[distance]
        val fuelConsumed = distance * ship.fuelConsumptionPerTile
        val fuelNextTick = ship.fuel - fuelConsumed
        val reachableDistance = ship.getDistanceWithFuel(fuelNextTick)
        val refuelingHarbors = oceanMap.getRefuelingStationHarborTiles()
        // If we are not either on the way to refueling or escaping,
        // then we have to check if we can make it back from the intermediate destination
        // to the harbor in the next tick and if not we will go refueling instead.
        if (ship.behaviour != Behaviour.REFUELING &&
            ship.behaviour != Behaviour.ESCAPING && // YOU WOULD ADD damaged condition here so if damaged return
            !pathFinder.isReachableWithinDistance(
                intermediateDestination,
                // ship.corporation.harbors,
                // TODO() THIS SHOULD BE LIST OF REFUELING HARBORS, and then we should move by lowest harbor ID
                refuelingHarbors,
                reachableDistance
            )
        ) {
            return moveToRefuel(ship)
        }
        // Moves a ship simply
        if (distance > 0) {
            ship.fuel = fuelNextTick
            oceanMap.moveShip(ship, intermediateDestination)
            Logger.logShipMove(ship.id, ship.velocity, intermediateDestination.id)
        }
        val reachedDestination = intermediateDestination == path.lastOrNull()
        if (reachedDestination && ship.behaviour != Behaviour.EXPLORING) {
            ship.velocity = 0
        }
    }

    /**
     * Moves the ship to the closest tile without restriction if possible.
     */
    private fun escapeRestriction(ship: Ship) {
        ship.behaviour = Behaviour.ESCAPING
        val shipTile = oceanMap.getShipTile(ship)
        val path = pathFinder.escapeRestriction(shipTile)
        moveAlongPath(ship, path)
    }

    /**
     * Moves the ship towards the closest reachable harbour and sets the behaviour to refueling.
     * If the ship has a task, it immediately fails and is removed from the ship.
     */
    private fun moveToRefuel(ship: Ship) {
        ship.behaviour = Behaviour.REFUELING
        ship.task = null
        ship.returnToPurchase = false // TODO()
        if (ship.isRefueling()) {
            ship.returnToRefuel = false
        } else {
            ship.returnToRefuel = true
        }

        moveToRefuelHarbor(ship)
    }

    /**
     * Moves the ship towards the closest reachable harbour and sets the behaviour to unloading.
     */
    private fun moveToUnload(ship: Ship) {
        ship.behaviour = Behaviour.UNLOADING
        if (ship.isUnloading()) {
            ship.returnToUnload = false
        } else {
            ship.returnToUnload = true
            // you just have one flag that you can finish unloading.
            // go through the garbage types you want to unload,
            // for each type you check for each one, am I currently on a harbor that can unload this
            // refueling phase
            // if you start the tick at a harbor you can unload, then you can unload.
        }
        moveToUnloadHarbor(ship)
    }

    /**
     * Moves the ship towards the closest reachable harbour and sets the behaviour to repairing.
     * If the ship has a task, it immediately fails and is removed from the ship.
     */
    private fun moveToRepair(ship: Ship) {
        ship.behaviour = Behaviour.REPAIRING
        ship.task = null
        ship.returnToPurchase = false
        if (ship.isRepairing()) {
            ship.returnToRepair = false
        } else {
            ship.returnToRepair = true
        }
        moveToRepairHarbor(ship)
    }

    /**
     * Move to refuel harbor
     */
    private fun moveToRefuelHarbor(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        /*if(refuelingShipPresentOnTile(ship,shipTile))
            return*/
        val tilesWithRefuelingStation = oceanMap.getRefuelingStationHarborTiles()
        val path = pathFinder.getShortesPathToHarbor(shipTile, tilesWithRefuelingStation)
        moveAlongPath(ship, path)
    }

    /**
     * Refueling Ship Is Present
     */
    private fun refuelingShipPresentOnTile(ship: Ship): Boolean {
        val shipTile = oceanMap.getShipTile(ship)
        val shipsOnTile = oceanMap.getShipsOnTile(shipTile)
        // forgot case of a ship being damaged it will never repair otherwis
        val shipFuelLessOrDamaged = ship.fuel < ship.maxFuel / 2 && !ship.isDamaged
        val shipFuelNeed = ship.maxFuel - ship.fuel
        val refuelingShipExists = shipsOnTile
            .any { refShip ->
                refShip.type == ShipType.REFUELING &&
                    refShip.id != ship.id &&
                    refShip.currentRefuelingCapacity >= shipFuelNeed &&
                    shipFuelLessOrDamaged
                !refShip.activeRefueling &&
                    !refShip.receivingRefuel
            }
        return refuelingShipExists
    }

    /**
     * Move to unload harbor
     */
    private fun moveToUnloadHarbor(ship: Ship) { // //TODO()))
        val shipTile = oceanMap.getShipTile(ship)
        val needsToUnload = ship.garbageCapacity.filter { it.value == 0 }.keys
        // var path = emptyList<Tile>()
        // WE only move if we can find a path to it
        for (garbageType in GarbageType.entries) {
            if (garbageType in needsToUnload) {
                val tilesWithUnloadingStation = oceanMap.getUnloadingHarborTiles(ship.corporation.id, garbageType)
                // if (tilesWithUnloadingStation.isNotEmpty()) {
                val path = pathFinder.getShortesPathToHarbor(shipTile, tilesWithUnloadingStation)
                moveAlongPath(ship, path)
                return
                    /*if (path.isNotEmpty()) {
                        break
                    }
                }*/
            }
        }
    }

    /**
     * Move to shipyard to Repair
     */
    private fun moveToRepairHarbor(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val tilesWithRepairingStation = oceanMap.getRepairingStationHarborTiles()
        val path = pathFinder.getShortesPathToHarbor(shipTile, tilesWithRepairingStation)
        moveAlongPath(ship, path)
    }

    /**
     * Moves the ship towards the target destination of its task.
     * @throws IllegalArgumentException If the ship has no task.
     */
    private fun moveToTask(ship: Ship) {
        ship.returnToPurchase = false
        val shipTile = oceanMap.getShipTile(ship)
        val shipTask = ship.task ?: throw IllegalArgumentException("Ship has no task")
        val path = pathFinder.getShortestPathToTile(shipTile, setOf(shipTask.destination))
        if (path.isEmpty()) {
            // This means that the task destination was not reachable.
            // In this case the task failed, and we remove it from the ship.
            ship.task = null
            // The ship will move according to its default behaviour instead.
            // Since this includes unloading, we call moveShip() instead of moveShipDefault().
            return moveShip(ship, shipTile)
        }
        moveAlongPath(ship, path)
    }

    /**
     * Moves the ship according to its default behaviour.
     */
    private fun moveShipDefault(ship: Ship) {
        ship.behaviour = Behaviour.DEFAULT
        val path = when (ship.type) {
            ShipType.SCOUTING -> getScoutingPathDefault(ship)
            ShipType.COORDINATING -> getCoordinatingPathDefault(ship)
            ShipType.COLLECTING -> getCollectingPathDefault(ship)
            ShipType.REFUELING -> getRefuelingPathDefault(ship)
        }
        moveAlongPath(ship, path)
        // If a collecting ship actually moves towards the selected garbage
        // and does not go refueling instead, we assign it to the tile of the selected garbage.
        if (ship.type == ShipType.COLLECTING &&
            ship.behaviour == Behaviour.DEFAULT &&
            path.isNotEmpty()
        ) {
            // path.last() is the location of the selected garbage
            assignShipToGarbageTile(ship, path.last())
        }
    }

    /**
     * Returns a path according to the default behaviour of the given scouting ship.
     */
    private fun getScoutingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)
        // First look for garbage in the ship's visibility range and
        // return the shortest path to the closest reachable garbage if there is one.
        val garbageInShipVisibility = visibilityHandler.getGarbageInShipVisibility(ship)
        if (garbageInShipVisibility.isNotEmpty()) {
            val path = pathFinder.getShortestPathToGarbage(shipTile, garbageInShipVisibility)
            if (path.isNotEmpty()) return path
        }
        // Then look for garbage in corporation's information and
        // return the shortest path to the closest reachable garbage if there is one.
        val garbageInCorpInformation = visibilityHandler.getGarbageInCorpInformation(ship)
        if (garbageInCorpInformation.isNotEmpty()) {
            val path = pathFinder.getShortestPathToGarbage(shipTile, garbageInCorpInformation)
            if (path.isNotEmpty()) return path
        }
        // Otherwise return a path according to the ship's exploring behaviour.
        ship.behaviour = Behaviour.EXPLORING
        return pathFinder.explore(shipTile, ship.getDistanceWithVelocity())
    }

    /**
     * Returns a path according to the default behaviour of the given coordinating ship.
     */
    private fun getCoordinatingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)
        // Look for ships the given ship can cooperate with in the corporation's visibility range and
        // return the shortest path to the closest reachable ship if there is one.
        // easiest way to do this since we would like to move the ship to purchase without creating
        // a new move to function
        // addition of flag for verification
        if (ship.corporation.assignedBuyingShipId == ship.id && ship.corporation.assignedBuyingShipId != -1) {
            val stationTile = purchaseHandler.cheapeastShipyardStation(ship.corporation) ?: return emptyList()
            ship.returnToPurchase = true
            return pathFinder.getShortestPathToTile(oceanMap.getShipTile(ship), setOf(stationTile))
        }
        val relevantShips = visibilityHandler.getShipsInCorpVisibility(ship)
            .mapValues { it.value.filter { otherShip -> ship.canCooperateWith(otherShip) } }
            .filter { it.value.isNotEmpty() }

        if (relevantShips.isNotEmpty()) {
            val path = pathFinder.getShortestPathToShip(shipTile, relevantShips)
            if (path.isNotEmpty()) return path
        }
        // Otherwise return a path according to the ship's exploring behaviour.
        ship.behaviour = Behaviour.EXPLORING
        return pathFinder.explore(shipTile, ship.getDistanceWithVelocity())
    }

    /**
     * Returns a path according to the default behaviour of the given collecting ship.
     */
    private fun getCollectingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)
        // Look for garbage in the corporation's visibility range.
        val garbageInCorpVisibility = visibilityHandler.getGarbageInCorpVisibility(ship)
        // Filter for garbage types the given ship can collect.
        val garbageWithMatchingTypes = garbageInCorpVisibility
            .mapValues { it.value.filter { garbage -> garbage.type in ship.maxGarbageCapacity.keys } }
            .filter { it.value.isNotEmpty() }
        // Filter for tiles where the corporation hasn't already sent enough ships
        // to collect all the garbage on that tile.
        val assignedShipsPerTile = ship.corporation.assignedShipsPerTile
        val relevantGarbage = garbageWithMatchingTypes
            .filter { notEnoughShipsAssigned(it.value, assignedShipsPerTile[it.key].orEmpty()) }
        // If there is relevant garbage, return the shortest path to the closest reachable garbage.
        if (relevantGarbage.isNotEmpty()) {
            return pathFinder.getShortestPathToGarbage(shipTile, relevantGarbage)
        }
        // Otherwise don't move.
        return emptyList()
    }

    /**
     * Returns a path according to the default behaviour of the given refueling ship.
     */
    private fun getRefuelingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)
        if (ship.activeRefueling) {
            return emptyList()
        }
        // PARTCHECKED
        if (ship.currentRefuelingCapacity == 0) {
            ship.behaviour = Behaviour.REFUELING
            ship.returnToRefuel = true
            ship.isRefillingCapacity = true
            val tilesWithRefuelingStation = oceanMap.getRefuelingStationHarborTiles()
            return pathFinder.getShortesPathToHarbor(shipTile, tilesWithRefuelingStation)
        }
        // ENDCHECK
        // MISSING CHECK THAT REFUELING SHIP SHOULD ITSELF REFUEL
        val shipsToRefuelEmpty = shipsToRefuel(ship).isEmpty()
        if (shipsToRefuelEmpty || refuelingShipPresentOnTile(ship)) { // refueling ship present on tile check
            return emptyList()
        }
        // ENDCHECKED

        if (shipsToRefuel(ship).isNotEmpty()) {
            val shipsToRefuelResult = shipsToRefuel(ship)
            val shipTileMap = shipsToRefuelResult.entries.groupBy({ it.key }, { it.value })
            val path =
                pathFinder.getShortestPathToShip(shipTile, shipTileMap)
            return path
        }

        // Otherwise don't move.
        return emptyList()
    }

    /**
     * Ships we can refuel as a refueling ship
     */
    fun shipsToRefuel(refship: Ship): Map<Tile, Ship> {
        val emptyTileMap = emptyMap<Tile, Ship>()
        val shipsWhichWeCanRefuel =
            refship.corporation.ships
                .filter { it.fuel < it.maxFuel / 2 && it.id != refship.id && !it.receivingRefuel }
                .filter { refship.currentRefuelingCapacity >= it.maxFuel - it.fuel }
                .filter { !it.activeRefueling } // TODO() ALSO CHECK
                .filter { !it.isRefueling() } // TODO() RECHECK AFTER PUSH
        // path finder requries this since it does not accept a normal list rather a
        // Map of tile and ships on those tiles
        val shipTileMap = shipsWhichWeCanRefuel.associateBy { oceanMap.getShipTile(it) }
        if (shipTileMap.isNotEmpty()) {
            return shipTileMap
        }
        return emptyTileMap
    } // TODO() FOR THE TILE

    /**
     * Checks if corporation hasn't already sent enough ships
     * to collect all the garbage on the tile.
     */
    private fun notEnoughShipsAssigned(garbageOnTile: Collection<Garbage>, ships: Collection<Ship>): Boolean {
        // Sums of garbage amounts per garbage type on the tile
        val garbageSumsPerType = SumsPerTypeUtil.getGarbageSumsPerType(garbageOnTile)
        // Sums of ship capacities per garbage type on the tile
        val capacitySumsPerType = SumsPerTypeUtil.getCapacitySumsPerType(ships)
        // Return true if any garbage type doesn't already have enough ships assigned
        return garbageSumsPerType.any { (garbageType, garbageSum) ->
            garbageSum > (capacitySumsPerType[garbageType] ?: 0)
        }
    }

    /**
     * Assigns the ship to the given tile in the corporation's assignedShipsPerTile map.
     */
    private fun assignShipToGarbageTile(ship: Ship, tile: Tile) {
        ship.corporation.assignedShipsPerTile
            .getOrPut(tile) { mutableListOf() }
            .add(ship)
    }
}
