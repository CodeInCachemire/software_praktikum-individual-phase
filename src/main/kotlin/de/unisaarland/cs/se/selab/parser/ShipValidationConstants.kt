package de.unisaarland.cs.se.selab.parser

import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType

/**
 * Constants for validating Ships.
 */
object ShipValidationConstants {
    const val SCOUTINGMAXVELMIN = 10
    const val SCOUTINGMAXVELMAX = 100
    const val SCOUTINGACCELMIN = 5
    const val SCOUTINGACCELMAX = 25
    const val SCOUTINGFUELCAPMIN = 3000
    const val SCOUTINGFUELCAPMAX = 10000
    const val SCOUTINGFUELCONSUMPMIN = 7
    const val SCOUTINGFUELCONSUMPMAX = 10
    const val SCOUTINGVISRANGEMIN = 2
    const val SCOUTINGVISRANGEMAX = 5
    const val COORDINATINGMAXVELMIN = 10
    const val COORDINATINGMAXVELMAX = 50
    const val COORDINATINGACCELMIN = 5
    const val COORDINATINGACCELMAX = 15
    const val COORDINATINGFUELCAPMIN = 3000
    const val COORDINATINGFUELCAPMAX = 5000
    const val COORDINATINGFUELCONSUMPMIN = 5
    const val COORDINATINGFUELCONSUMPMAX = 7
    const val COORDINATINGVISRANGEMIN = 1
    const val COORDINATINGVISRANGEMAX = 1
    const val COLLECTINGMAXVELMIN = 10
    const val COLLECTINGMAXVELMAX = 50
    const val COLLECTINGACCELMIN = 5
    const val COLLECTINGACCELMAX = 10
    const val COLLECTINGFUELCAPMIN = 3000
    const val COLLECTINGFUELCAPMAX = 5000
    const val COLLECTINGFUELCONSUMPMIN = 5
    const val COLLECTINGFUELCONSUMPMAX = 9
    const val COLLECTINGVISRANGEMIN = 0
    const val COLLECTINGVISRANGEMAX = 0

    const val PLASTICCAPMIN = 1000
    const val PLASTICCAPMAX = 5000
    const val OILCAPMIN = 50000
    const val OILCAPMAX = 100000
    const val CHEMCAPMIN = 1000
    const val CHEMCAPMAX = 10000
}

/*
the following extends ShipType and GarbageType with fields that have the validation constants
 */

internal val ShipType.maxVelMin: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGMAXVELMIN
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGMAXVELMIN
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGMAXVELMIN
    }

internal val ShipType.maxVelMax: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGMAXVELMAX
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGMAXVELMAX
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGMAXVELMAX
    }

internal val ShipType.accelMin: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGACCELMIN
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGACCELMIN
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGACCELMIN
    }

internal val ShipType.accelMax: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGACCELMAX
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGACCELMAX
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGACCELMAX
    }

internal val ShipType.fuelCapMin: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGFUELCAPMIN
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGFUELCAPMIN
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGFUELCAPMIN
    }
internal val ShipType.fuelCapMax: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGFUELCAPMAX
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGFUELCAPMAX
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGFUELCAPMAX
    }

internal val ShipType.fuelConsumpMin: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGFUELCONSUMPMIN
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGFUELCONSUMPMIN
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGFUELCONSUMPMIN
    }

internal val ShipType.fuelConsumpMax: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGFUELCONSUMPMAX
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGFUELCONSUMPMAX
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGFUELCONSUMPMAX
    }

internal val ShipType.visRangeMin: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGVISRANGEMIN
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGVISRANGEMIN
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGVISRANGEMIN
    }

internal val ShipType.visRangeMax: Int
    get() = when (this) {
        ShipType.SCOUTING -> ShipValidationConstants.SCOUTINGVISRANGEMAX
        ShipType.COORDINATING -> ShipValidationConstants.COORDINATINGVISRANGEMAX
        ShipType.COLLECTING -> ShipValidationConstants.COLLECTINGVISRANGEMAX
    }

internal val GarbageType.minCap: Int
    get() = when (this) {
        GarbageType.PLASTIC -> ShipValidationConstants.PLASTICCAPMIN
        GarbageType.OIL -> ShipValidationConstants.OILCAPMIN
        GarbageType.CHEMICALS -> ShipValidationConstants.CHEMCAPMIN
    }

internal val GarbageType.maxCap: Int
    get() = when (this) {
        GarbageType.PLASTIC -> ShipValidationConstants.PLASTICCAPMAX
        GarbageType.OIL -> ShipValidationConstants.OILCAPMAX
        GarbageType.CHEMICALS -> ShipValidationConstants.CHEMCAPMAX
    }
