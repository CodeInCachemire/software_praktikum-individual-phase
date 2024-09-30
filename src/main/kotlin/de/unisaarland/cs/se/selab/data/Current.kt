package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.Direction

/**
 * This class represent the currents
 */
class Current(direction: Int, val speed: Int, val intensity: Int) {
    val direction = Direction.createDirection(direction)
}
