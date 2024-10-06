package de.unisaarland.cs.se.selab.data

/**
 * This is the refuelingStation Class
 */
data class RefuelingStation(
    val refuelCost: Int,
    val refuelTimes: Int
) {
    var refuelCount = 0

    /**
     * Increase the count of the station
     */
    fun incCount() {
        refuelCount++
    }

    /**
     * Station cannot be used anymore
     */
    fun stationNotClosed(): Boolean {
        return refuelTimes != refuelCount
    }
}
