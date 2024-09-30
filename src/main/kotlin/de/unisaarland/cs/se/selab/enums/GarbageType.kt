package de.unisaarland.cs.se.selab.enums

import de.unisaarland.cs.se.selab.parser.JsonKeys

/**
 * The GarbageType enum class
 */
enum class GarbageType {
    PLASTIC,
    OIL,
    CHEMICALS;

    /**
     * creates a direction based on the input degree
     */

    companion object {
        /**
         * creates a direction based on the input degree
         */
        fun createGarbageType(type: String): GarbageType {
            return when (type) {
                JsonKeys.PLASTIC -> PLASTIC
                JsonKeys.OIL -> OIL
                JsonKeys.CHEMICALS -> CHEMICALS
                else -> throw IllegalArgumentException("Invalid garbage type")
            }
        }
    }
}
