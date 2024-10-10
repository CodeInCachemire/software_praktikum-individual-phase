package de.unisaarland.cs.se.selab.systemtest.utils

import de.unisaarland.cs.se.selab.enums.GarbageType
import java.util.*

/**
 * Utils object for providing strings for expected logs.
 * Not complete, extend this for more log messages as you see fit!
 */
object LoggingUtils {
    /**
     * Returns string for invalid config file [fileName].
     */
    fun initializationInfoInvalidAssertion(fileName: String): String {
        return "Initialization Info: $fileName is invalid."
    }

    /**
     * Returns string for corporation [corporationId] collected [amount] of garbage.
     */
    fun simulationStatisticsCorporation(corporationId: Int, amount: Int): String {
        return "Simulation Statistics: Corporation $corporationId collected $amount of garbage."
    }

    fun eventTyphoon(eventId: Int, eventType: String): String {
        return "Event: Event $eventId of type $eventType happened."
    }

    /**
     * Log event typhoon.
     */
    fun typhoonEvent(eventId: Int, radius: Int, tileID: Int, sortedShips: SortedSet<Int>): String {
        val sortedShipsIDs = sortedShips.joinToString(", ")
        return "Event: Typhoon $eventId at tile $tileID with radius $radius affected" +
            " ships: $sortedShipsIDs."
    }

    /**
     * Purchase event
     */
    fun purchaseRefueling(shipID: Int, refuelShipID: Int, harborID: Int, cost: Int): String {
        return "Purchase: Ship $shipID ordered a refueling ship with id $refuelShipID" +
            " at harbor $harborID for $cost credits."
    }

    /**
     * Delivered Ship
     */
    fun deliveredRefueling(refuelShipID: Int, corporationId: Int, tileID: Int): String {
        return "Purchase: Ship $refuelShipID delivered to corporation $corporationId at" +
            " $tileID."
    }

    /**
     * Repair start
     */
    fun logDamageRepairStart(shipID: Int, harborID: Int, amount: Int): String {
        return "Repair: Ship $shipID is being repaired at harbor $harborID for $amount credits."
    }

    /**
     * Damage repair finish
     */
    fun logDamageRepairFinish(shipID: Int): String {
        return "Repair: Ship $shipID is repaired."
    }

    fun logUnload(shipID: Int, amount: Int, garbageType: GarbageType, harborID: Int, creditAmount: Int): String {
        return "Unload: Ship $shipID unloaded $amount of garbage $garbageType at harbor" +
            " $harborID and received $creditAmount credits."
    }

    /**
     * for refueling
     */
    fun logRefueling(shipID: Int, harborID: Int, amount: Int): String {
        return "Refueling: Ship $shipID starts to refuel at harbor $harborID and paid $amount credits."
    }

    /**
     */
    fun logRefuelingFail(shipID: Int, harborID: Int): String {
        return "Refueling: Ship $shipID cannot refuel at harbor $harborID."
    }

    /**
     * Log corporation moving it's ships
     */
    fun logStartMove(corporationId: Int): String {
        return "Corporation Action: Corporation $corporationId is starting to move its ships."
    }

    /**
     * Log start of garbage collection for a corporation
     */
    fun logCorpCollectGarbage(corporationId: Int): String {
        return "Corporation Action: Corporation $corporationId is starting to collect garbage."
    }

    /**
     *
     */
    /**
     * Log start of a tick.
     */
    fun logTickStart(tick: Int): String {
        return "Simulation Info: Tick $tick started."
    }
}
