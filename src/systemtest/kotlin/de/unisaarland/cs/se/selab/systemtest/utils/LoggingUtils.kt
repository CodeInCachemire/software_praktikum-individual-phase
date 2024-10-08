package de.unisaarland.cs.se.selab.systemtest.utils

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
}
