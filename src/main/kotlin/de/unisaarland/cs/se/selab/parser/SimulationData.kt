package de.unisaarland.cs.se.selab.parser

import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Reward
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.event.Event
import de.unisaarland.cs.se.selab.task.Task

/**
 * The SimulationData class collects all the data objects
 * created by the parser
 */
data class SimulationData(
    var oceanMap: OceanMap? = null,
) {
    val tiles: MutableMap<Int, Tile> = mutableMapOf()
    val rewards: MutableMap<Int, Reward> = mutableMapOf()
    val ships: MutableMap<Int, Ship> = mutableMapOf()
    val tasks: MutableMap<Int, Task> = mutableMapOf()
    val events: MutableMap<Int, Event> = mutableMapOf()
    val garbage: MutableMap<Int, Garbage> = mutableMapOf()
    val corporations: MutableMap<Int, Corporation> = mutableMapOf()
    val harborTiles: MutableSet<Tile> = mutableSetOf()
}
