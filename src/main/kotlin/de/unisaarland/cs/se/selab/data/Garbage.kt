package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.GarbageType

/**
 *  The garbage class
 */
class Garbage(
    val id: Int,
    val type: GarbageType,
    var amount: Int
) : Comparable<Garbage> {

    override fun compareTo(other: Garbage): Int {
        return id.compareTo(other.id)
    }
}
