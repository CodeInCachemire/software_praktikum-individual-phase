package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.Direction

/***
 * The coordinate class
 */
data class Coordinate(val x: Int, val y: Int) {
    /**
     * gets the direct neighbours based on current coordinate
     */
    fun getNeighbours(): List<Coordinate> {
        return Direction.entries.map { getNeighbourCoordinate(it) }
    }

    /**
     * gets the neighbours within the radius of the current coordinate
     */
    fun getNeighbours(radius: Int): List<Coordinate> {
        var neighbors: List<Coordinate> = listOf(this)
        var neighborsLastLevel: List<Coordinate> = emptyList()
        var neighborsThisLevel: List<Coordinate> = emptyList()
        for (i in 1..radius) {
            if (i == 1) {
                neighbors = neighbors + getNeighbours()
                neighborsLastLevel = neighbors
            } else {
                for (coordinate in neighborsLastLevel) {
                    neighborsThisLevel =
                        (neighborsThisLevel + coordinate.getNeighbours()).distinct()
                }
                neighborsLastLevel = neighborsThisLevel
                neighbors = (neighbors + neighborsThisLevel).distinct()
            }
        }
        return neighbors
    }

    /**
     * Get the neighbouring coordinate in the given direction.
     */
    fun getNeighbourCoordinate(direction: Direction): Coordinate {
        return if (y % 2 == 0) {
            when (direction) {
                Direction.RIGHT -> Coordinate(x + 1, y)
                Direction.DOWN_RIGHT -> Coordinate(x + 1, y + 1)
                Direction.DOWN_LEFT -> Coordinate(x, y + 1)
                Direction.LEFT -> Coordinate(x - 1, y)
                Direction.UP_LEFT -> Coordinate(x, y - 1)
                Direction.UP_RIGHT -> Coordinate(x + 1, y - 1)
            }
        } else {
            when (direction) {
                Direction.RIGHT -> Coordinate(x + 1, y)
                Direction.DOWN_RIGHT -> Coordinate(x, y + 1)
                Direction.DOWN_LEFT -> Coordinate(x - 1, y + 1)
                Direction.LEFT -> Coordinate(x - 1, y)
                Direction.UP_LEFT -> Coordinate(x - 1, y - 1)
                Direction.UP_RIGHT -> Coordinate(x, y - 1)
            }
        }
    }
}
