package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.TaskType
import java.io.PrintWriter
import java.util.SortedSet

/**
 * The Logger object
 */
object Logger {
    private val collectedGarbage = mutableMapOf<Int, Int>()
    var printer = PrintWriter(System.out, true)
    private var plastic = 0
    private var oil = 0
    private var chemical = 0

    /**
     * Set path to output file, if specified. This will change output mode to "FILE".
     */

    fun setFilePath(filePathStr: String) {
        printer = PrintWriter(filePathStr)
    }

    /**
     * Log singular parsing result.
     */
    fun logParsingResult(success: Boolean, filename: String) {
        var outputLine = "Initialization Info:"
        outputLine += if (success) {
            " $filename successfully parsed and validated."
        } else {
            " $filename is invalid."
        }
        printer.println(outputLine)
        printer.flush()
    }

    /**
     * Initialize the Map for every corporation with amount 0
     */
    fun initializeCorpGarbage(corpIds: List<Int>) {
        for (corpId in corpIds) {
            collectedGarbage[corpId] = 0
        }
    }

    /**
     * Log start of simulation.
     */
    fun logSimStart() {
        printer.println("Simulation Info: Simulation started.")
        printer.flush()
    }

    /**
     * Log start of a tick.
     */
    fun logTickStart(tick: Int) {
        val outputLine = "Simulation Info: Tick $tick started."
        printer.println(outputLine)
        printer.flush()
    }

    /**
     * Log start of a move for corporation.
     */
    fun logStartMove(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId is starting to move its ships.")
        printer.flush()
    }

    /**
     * Log ship movement to certain tile.
     */
    fun logShipMove(shipId: Int, speed: Int, tileId: Int) {
        if (speed >= Constants.TILE_DISTANCE) {
            printer.println("Ship Movement: Ship $shipId moved with speed $speed to tile $tileId.")
            printer.flush()
        }
    }

    /**
     * Log start of garbage collection for a corporation
     */
    fun logCorpCollectGarbage(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId is starting to collect garbage.")
        printer.flush()
    }

    /**
     * Log use of tracker from a ship.
     */
    fun logAttachTracker(corporationId: Int, garbageId: Int, shipId: Int) {
        printer.println(
            "Corporation Action: Corporation $corporationId attached tracker to garbage $garbageId " +
                "with ship $shipId."
        )
        printer.flush()
    }

    /**
     * Log how much garbage collected by a ship.
     */
    fun logCollectGarbage(shipId: Int, amount: Int, garbageId: Int, garbageType: GarbageType, corporationId: Int) {
        when (garbageType) {
            GarbageType.PLASTIC -> plastic += amount
            GarbageType.OIL -> oil += amount
            GarbageType.CHEMICALS -> chemical += amount
        }
        collectedGarbage.compute(corporationId) { _, v -> v?.plus(amount) ?: amount }
        printer.println(
            "Garbage Collection: Ship $shipId collected $amount of garbage $garbageType " +
                "with $garbageId."
        )
        printer.flush()
    }

    /**
     * Log start of cooperation of a corporation
     */
    fun logCoopStart(corporationId: Int) {
        printer.println(
            "Corporation Action: Corporation $corporationId " +
                "is starting to cooperate with other corporations."
        )
        printer.flush()
    }

    /**
     * Log cooperation between two corporation according to their respective ship.
     */
    fun logCoopWith(corporationA: Int, corporationB: Int, shipA: Int, shipB: Int) {
        printer.println(
            "Cooperation: Corporation $corporationA cooperated with corporation $corporationB with ship $shipA " +
                "to ship $shipB."
        )
        printer.flush()
    }

    /**
     * Log start of refueling of a ship.
     */
    fun logRefuelStart(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId is starting to refuel.")
        printer.flush()
    }

    /**
     * Log start of refueling of a ship.
     */
    fun logRefueling(shipId: Int, harborId: Int) {
        printer.println("Refueling: Ship $shipId refueled at harbor $harborId.")
        printer.flush()
    }

    /**
     * Log unloading of garbage from a ship.
     */
    fun logUnload(shipId: Int, amount: Int, garbageType: GarbageType, harborId: Int) {
        printer.println(
            "Unload: Ship $shipId unloaded $amount of garbage $garbageType at harbor $harborId."
        )
        printer.flush()
    }

    /**
     * Log end of corporation action for that tick.
     */
    fun logFinishAction(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId finished its actions.")
        printer.flush()
    }

    /**
     * Log garbage drifting from currents.
     */
    fun logGarbageDrift(
        garbageType: GarbageType,
        garbageId: Int,
        amount: Int,
        startTileId: Int,
        endTileId: Int
    ) {
        printer.println(
            "Current Drift: $garbageType $garbageId with amount $amount drifted from tile " +
                "$startTileId to tile $endTileId."
        )
        printer.flush()
    }

    /**
     * Log ship drifting from currents.
     */
    fun logShipDrift(shipId: Int, startTileId: Int, endTileId: Int) {
        printer.println("Current Drift: Ship $shipId drifted from tile $startTileId to tile $endTileId.")
        printer.flush()
    }

    /**
     * Log event trigger.
     */
    fun logEvent(eventId: Int, eventType: String) {
        printer.println("Event: Event $eventId of type $eventType happened.")
        printer.flush()
    }

    /**
     *
     */
    fun logTyphoon(eventId: Int, radius: Int, tileID: Int, sortedShips: SortedSet<Ship>) {
        val sortedShipsIDs = sortedShips.map { it.id }.joinToString(", ")
        printer.println(
            "Event: Typhoon $eventId at tile $tileID with radius $radius affected" +
                " ships: $sortedShipsIDs."
        )
    }

    /**
     * Log task assignment.
     */
    fun logTask(taskId: Int, taskType: TaskType, shipId: Int, destinationId: Int) {
        printer.println(
            "Task: Task $taskId of type $taskType with ship $shipId is added with destination " +
                "$destinationId."
        )
        printer.flush()
    }

    /**
     * Log reward distribution.
     */
    fun logReward(taskId: Int, shipId: Int, reward: RewardType) {
        printer.println("Reward: Task $taskId: Ship $shipId received reward of type $reward.")
        printer.flush()
    }

    /**
     * Log end of simulation.
     */
    fun logSimEnd() {
        printer.println("Simulation Info: Simulation ended.")
        printer.flush()
    }

    /**
     * Log statistics.
     */
    fun logStatistics(leftoverGarbage: Int) {
        printer.println("Simulation Info: Simulation statistics are calculated.")
        printer.flush()
        for (corporationId in collectedGarbage.keys.sorted()) {
            printer.println(
                "Simulation Statistics: Corporation $corporationId " +
                    "collected ${collectedGarbage.getValue(corporationId)} of garbage."
            )
            printer.flush()
        }
        printer.flush()
        printer.println("Simulation Statistics: Total amount of plastic collected: $plastic.")
        printer.flush()
        printer.println("Simulation Statistics: Total amount of oil collected: $oil.")
        printer.flush()
        printer.println("Simulation Statistics: Total amount of chemicals collected: $chemical.")
        printer.flush()
        printer.println("Simulation Statistics: Total amount of garbage still in the ocean: $leftoverGarbage.")
        printer.flush()
    }
}
