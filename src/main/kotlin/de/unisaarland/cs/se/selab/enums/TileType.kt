package de.unisaarland.cs.se.selab.enums

import de.unisaarland.cs.se.selab.parser.JsonKeys

/**
 * The TileType enum class
 */
enum class TileType {
    LAND,
    SHORE,
    SHALLOW_OCEAN,
    DEEP_OCEAN;

    /**
     * Creates a TileType based on the input category
     */

    companion object {
        /**
         * creates a direction based on the input degree
         */
        fun createTileType(category: String): TileType {
            return when (category) {
                JsonKeys.DEEP_OCEAN -> DEEP_OCEAN
                JsonKeys.SHALLOW_OCEAN -> SHALLOW_OCEAN
                JsonKeys.SHORE -> SHORE
                JsonKeys.LAND -> LAND
                else -> throw IllegalArgumentException("Invalid direction")
            }
        }
    }
}
