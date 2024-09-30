package de.unisaarland.cs.se.selab.event

import de.unisaarland.cs.se.selab.data.OceanMap

/**
 * Abstract class for events with a duration
 */
abstract class DurationEvent(id: Int, tick: Int) : Event(id, tick) {
    /**
     * This function will be executed from the EventHandler.
     * @return Returns true if the event has been ended, returns false if
     * the event is still ongoing and should be updated.
     */
    abstract fun update(oceanMap: OceanMap): Boolean
}
