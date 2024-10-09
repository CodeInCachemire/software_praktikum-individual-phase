package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Logger
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
}
