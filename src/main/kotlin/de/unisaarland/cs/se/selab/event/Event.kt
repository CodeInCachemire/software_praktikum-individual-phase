package de.unisaarland.cs.se.selab.event
import de.unisaarland.cs.se.selab.data.OceanMap

/**
 * Abstract class for all events.
 * The events work in tandem with the EventHandler.
 */
abstract class Event(val id: Int, val tick: Int) {

    /**
     * Apply the effect of the event to the OceanMap.
     * This method will be called from the EventHandler.
     */
    abstract fun execute(oceanMap: OceanMap)
}
