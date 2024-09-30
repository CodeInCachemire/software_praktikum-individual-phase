package de.unisaarland.cs.se.selab.enums

import de.unisaarland.cs.se.selab.Constants

/**
 * The direction enum
 */
enum class Direction {
    RIGHT,
    DOWN_RIGHT,
    DOWN_LEFT,
    LEFT,
    UP_LEFT,
    UP_RIGHT;

    /**
     * gives the direction
     */
    companion object {
        /**
         * creates a direction based on the input degree
         */
        fun createDirection(direction: Int): Direction {
            return when (direction) {
                Constants.DIRECTION_RIGHT -> RIGHT
                Constants.DIRECTION_DOWN_RIGHT -> DOWN_RIGHT
                Constants.DIRECTION_DOWN_LEFT -> DOWN_LEFT
                Constants.DIRECTION_LEFT -> LEFT
                Constants.DIRECTION_UP_LEFT -> UP_LEFT
                Constants.DIRECTION_UP_RIGHT -> UP_RIGHT
                else -> throw IllegalArgumentException("Invalid direction")
            }
        }
    }
}
