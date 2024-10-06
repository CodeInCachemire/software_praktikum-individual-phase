package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.event.DurationEvent
import de.unisaarland.cs.se.selab.event.Event
import de.unisaarland.cs.se.selab.event.OilEvent
import de.unisaarland.cs.se.selab.event.StormEvent
import de.unisaarland.cs.se.selab.event.TyphoonEvent
import de.unisaarland.cs.se.selab.parser.SimulationData

/**
 * Handles the execution and removals of the events.
 */
class EventHandler(
    simulationData: SimulationData,
    private val oceanMap: OceanMap,
    private val visibilityHandler: VisibilityHandler,
) {
    private val events: ArrayDeque<Event> = ArrayDeque(
        simulationData.events.values.sortedWith(compareBy<Event> { it.tick }.thenBy { it.id })
    )
    private val activeEvents = mutableSetOf<DurationEvent>()

    /**
     * this function is called by simulation and checks if an events starts and updates the current active events.
     * @param tick the current tick
     */
    fun updateEvents(tick: Int) {
        while (events.isNotEmpty() && events.first().tick == tick) {
            val event = events.removeFirst()
            event.execute(oceanMap)
            when (event) {
                is StormEvent -> {
                    visibilityHandler.globalUpdateCorpInformation(event.affectedGarbage)
                }
                is OilEvent -> {
                    visibilityHandler.globalUpdateCorpInformation(event.createdGarbage)
                }
                is DurationEvent -> {
                    activeEvents.add(event) // TODO()
                }
                is TyphoonEvent -> {
                    visibilityHandler.globalUpdateCorpInformation(event.createdGarbage)
                }
            }
        }

        // update active events and removes them from the set if they are finished.
        activeEvents.filter { it.tick < tick }.filter { it.update(oceanMap) }.forEach {
            activeEvents.remove(it)
        }
    }
}
