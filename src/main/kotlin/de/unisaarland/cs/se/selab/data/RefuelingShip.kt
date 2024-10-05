package de.unisaarland.cs.se.selab.data
import de.unisaarland.cs.se.selab.enums.ShipType

/**
 * RefuelingShipClass
 */
class RefuelingShip(
    id: Int,
    type: ShipType,
    corporation: Corporation,
    maxVelocity: Int,
    acceleration: Int,
    maxFuel: Int,
    fuelConsumption: Int,
    val refuelingCapacity: Int,
    val refuelingTime: Int

) : Ship(
    id,
    type,
    corporation,
    maxVelocity,
    acceleration,
    maxFuel,
    fuelConsumption,
    visibilityRange = 0,
    maxGarbageCapacity = mutableMapOf()
) {
    init {
        reward.clear()
    }
}
