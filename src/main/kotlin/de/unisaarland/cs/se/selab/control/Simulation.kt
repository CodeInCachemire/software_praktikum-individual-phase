package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.enums.ShipType
import de.unisaarland.cs.se.selab.enums.TileType
import de.unisaarland.cs.se.selab.parser.SimulationData

/**
 * The Simulation class
 */
class Simulation(
    simulationData: SimulationData,
    private val maxTicks: Int
) {
    private val oceanMap =
        simulationData.oceanMap ?: throw IllegalArgumentException("Map wasn't present after parsing.")

    private val corporations = simulationData.corporations.values.sortedBy { it.id }

    private val visibilityHandler = VisibilityHandler(oceanMap, corporations)
    private val pathFinder = PathFinder(oceanMap)
    private val eventHandler = EventHandler(simulationData, oceanMap, visibilityHandler)
    private val taskHandler = TaskHandler(simulationData, oceanMap, pathFinder)
    private val driftHandler = DriftHandler(oceanMap)
    private val purchaseHandler = PurchaseHandler(oceanMap, pathFinder)
    private val shipHandler = ShipHandler(oceanMap, visibilityHandler, corporations, purchaseHandler, simulationData)
    private val movementHandler = MovementHandler(pathFinder, oceanMap, visibilityHandler, purchaseHandler)

    init {

        oceanMap.tiles.values.removeIf { it.type == TileType.LAND }

        Logger.initializeCorpGarbage(simulationData.corporations.keys.toList())
        Logger.logSimStart()
        run()
    }

    /**
     * run the simulation
     */
    private fun run() {
        for (currentTick in 0 until maxTicks) {
            Logger.logTickStart(currentTick)

            for (corporation in corporations) {
                corporation.ships.removeIf { !oceanMap.getShipExists(it) }
                visibilityHandler.updateCorpInformation(corporation)
                corporation.assignedShipsPerTile.clear()
                clearRefuelingShip(corporation)
            }

            for (corporation in corporations) {
                purchaseHandler.purchasePhase(corporation)
                movementHandler.movementPhase(corporation)
                shipHandler.attachTracker(corporation)
                shipHandler.collectionPhase(corporation)
                shipHandler.cooperationPhase(corporation)
                shipHandler.refuelingPhase(corporation)
                shipHandler.unloadingPhase(corporation)
                shipHandler.repairingPhase(corporation)
                shipHandler.purchasingPhase(corporation)
                Logger.logFinishAction(corporation.id)
            }

            driftHandler.driftGarbage()
            driftHandler.driftShips()
            eventHandler.updateEvents(currentTick)
            taskHandler.updateTasks(currentTick)
        }
        Logger.logSimEnd()
        Logger.logStatistics(oceanMap.garbageToTile.keys.sumOf { it.amount })
    }

    /**
     * Clears refueling assignments.
     */
    fun clearRefuelingShip(corporation: Corporation) {
        val ships = corporation.ships
        val refuelingShips = ships.filter { it.refuelingShipCurrently && it.type == ShipType.REFUELING }
        for (refShip in refuelingShips) {
            val refShipTile = oceanMap.getShipTile(refShip)
            val shipID = refShip.iDOFSHIPBEINGREFUELED
            if (shipID != -1) {
                val shipBeingRefueled = ships[shipID]
                val shipRefueledTile = oceanMap.getShipTile(shipBeingRefueled)
                if (shipRefueledTile != refShipTile && shipBeingRefueled.beingRefueledByShip) {
                    refShip.refuelingShipCurrently = false
                    shipBeingRefueled.refuelingShipCurrently = false
                    refShip.iDOFSHIPBEINGREFUELED = -1
                    refShip.refuelingTime = refShip.getOriginalRefuelTime()
                }
            }
        }
    }
}
