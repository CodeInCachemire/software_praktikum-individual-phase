package de.unisaarland.cs.se.selab.data

/**
 * This is the shipyardStation modeled as a class
 */
data class ShipyardStation(
    val repairCost: Int,
    val shipCost: Int,
    val deliveryTime: Int,
    val maxVelocity: Int,
    val acceleration: Int,
    val fuelConsumption: Int,
    val fuelCapacity: Int,
    val refuelingCapacity: Int,
    val refuelingTime: Int
)
