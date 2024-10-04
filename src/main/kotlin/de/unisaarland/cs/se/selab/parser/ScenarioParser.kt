package de.unisaarland.cs.se.selab.parser
import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.JsonValue
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.ValidationFailure
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Constants.MAX_STRENGTH
import de.unisaarland.cs.se.selab.Constants.MIN_STRENGTH
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.Reward
import de.unisaarland.cs.se.selab.data.TaskData
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.TaskType
import de.unisaarland.cs.se.selab.enums.TileType
import de.unisaarland.cs.se.selab.event.Event
import de.unisaarland.cs.se.selab.event.OilEvent
import de.unisaarland.cs.se.selab.event.PirateEvent
import de.unisaarland.cs.se.selab.event.RestrictionEvent
import de.unisaarland.cs.se.selab.event.StormEvent
import de.unisaarland.cs.se.selab.event.TyphoonEvent
import de.unisaarland.cs.se.selab.task.CollectingTask
import de.unisaarland.cs.se.selab.task.CooperationTask
import de.unisaarland.cs.se.selab.task.ExploreTask
import de.unisaarland.cs.se.selab.task.FindTask
import de.unisaarland.cs.se.selab.task.Task
import org.json.JSONArray
import org.json.JSONObject

/**
 * The ScenarioParser class that parses the scenario JSON data
 * and creates the data objects
 */
class ScenarioParser(private val simulationData: SimulationData) {
    private val oceanMap = simulationData.oceanMap ?: error("Ocean Map didn't exist when parsing corporations.")
    private val eventsMap: MutableMap<Int, Event> = mutableMapOf()
    private val tasksMap: MutableMap<Int, Task> = mutableMapOf()
    private val garbageMap: MutableMap<Int, Garbage> = mutableMapOf()
    private val rewardsMap: MutableMap<Int, Reward> = mutableMapOf()
    private val garbageToTile: MutableMap<Garbage, Tile> = mutableMapOf()

    /**
     * Parse the JSON file for the scenario data.
     */
    fun parse(jsonString: String): Result<Unit> {
        val schema = SchemaLoader.forURL("classpath://schema/scenario.schema").load()
        val validator = Validator.create(schema, ValidatorConfig(FormatValidationPolicy.ALWAYS))

        val jsonInstance: JsonValue = JsonParser(jsonString).parse()
        val failure: ValidationFailure? = validator.validate(jsonInstance)
        if (failure != null) {
            return Result.failure(ParserException(failure.message))
        }

        val json = JSONObject(jsonString)
        val events = json.getJSONArray(JsonKeys.EVENTS)
        val tasks = json.getJSONArray(JsonKeys.TASKS)
        val garbage = json.getJSONArray(JsonKeys.GARBAGE)
        val rewards = json.getJSONArray(JsonKeys.REWARDS)

        parseConfig(events, garbage, rewards, tasks).onFailure { return Result.failure(it) }

        // add garbage to map
        garbageToTile.forEach { (garbage, tile) -> oceanMap.addGarbage(garbage, tile) }
        validateOilAmount().onFailure { return Result.failure(it) }
        return Result.success(Unit)
    }

    /**
     * Calls sub parsing methods for each of the JSON arrays.
     * @return Success in case no validation failed, otherwise the first failure encountered.
     */
    private fun parseConfig(
        events: JSONArray,
        garbage: JSONArray,
        rewards: JSONArray,
        tasks: JSONArray
    ): Result<Unit> {
        parseEvents(events).onFailure { return Result.failure(it) }
        simulationData.events.putAll(eventsMap)
        parseGarbage(garbage).onFailure { return Result.failure(it) }
        simulationData.garbage.putAll(garbageMap)
        parseRewards(rewards).onFailure { return Result.failure(it) }
        simulationData.rewards.putAll(rewardsMap)
        parseTasks(tasks).onFailure { return Result.failure(it) }
        simulationData.tasks.putAll(tasksMap)
        return Result.success(Unit)
    }

    private fun parseEvents(events: JSONArray): Result<Unit> {
        events.forEach { event ->
            if (event is JSONObject) {
                val newEvent = Events.createEventFromJson(event, simulationData).getOrElse { return Result.failure(it) }
                // check if the event id is unique
                if (eventsMap.containsKey(newEvent.id)) {
                    return Result.failure(ParserException("Event id ${newEvent.id} is not unique"))
                }
                eventsMap[newEvent.id] = newEvent
            }
        }
        return Result.success(Unit)
    }

    private fun parseTasks(tasks: JSONArray): Result<Unit> {
        tasks.forEach { task ->
            if (task is JSONObject) {
                val newTask = validateAndCreateTask(task).getOrElse { return Result.failure(it) }
                // check if the task id is unique
                if (tasksMap.containsKey(newTask.id)) {
                    return Result.failure(ParserException("Task id ${newTask.id} is not unique"))
                }
                tasksMap[newTask.id] = newTask
            }
        }
        // cross validate rewards and tasks
        crossValidateRewards().onFailure { return Result.failure(it) }

        return Result.success(Unit)
    }

    private fun parseGarbage(garbage: JSONArray): Result<Unit> {
        garbage.forEach { garbageObject ->
            if (garbageObject is JSONObject) {
                val newGarbage = validateAndCreateGarbage(garbageObject)
                    .getOrElse { return Result.failure(it) }
                // check if the garbage id is unique
                if (garbageMap.containsKey(newGarbage.id)) {
                    return Result.failure(ParserException("Garbage id ${newGarbage.id} is not unique"))
                }
                garbageMap[newGarbage.id] = newGarbage
            }
        }
        return Result.success(Unit)
    }

    private fun parseRewards(rewards: JSONArray): Result<Unit> {
        rewards.forEach { reward ->
            if (reward is JSONObject) {
                val newReward = validateAndCreateRewards(reward).getOrElse { return Result.failure(it) }
                // check if the reward id is unique
                if (rewardsMap.containsKey(newReward.id)) {
                    return Result.failure(ParserException("Reward id ${newReward.id} is not unique"))
                }
                rewardsMap[newReward.id] = newReward
            }
        }
        return Result.success(Unit)
    }

    private fun validateAndCreateTask(task: JSONObject): Result<Task> {
        val allowedKeys = setOf(
            JsonKeys.ID,
            JsonKeys.TYPE,
            JsonKeys.REWARD_SHIP,
            JsonKeys.TICK,
            JsonKeys.SHIP_ID,
            JsonKeys.TARGET_TILE,
            JsonKeys.REWARD_ID
        )
        // check if the keys are valid for the task
        if (task.keySet() != allowedKeys) {
            return Result.failure(ParserException("Invalid keys for task: ${task.keySet()}"))
        }

        val id = task.getInt(JsonKeys.ID)
        val rewardId = task.getInt(JsonKeys.REWARD_ID)
        val targetTileId = task.getInt(JsonKeys.TARGET_TILE)
        val shipId = task.getInt(JsonKeys.SHIP_ID)
        val rewardShip = task.getInt(JsonKeys.REWARD_SHIP)

        validateCommonTaskProperties(id, rewardId, shipId, rewardShip, targetTileId)
            .onFailure { return Result.failure(it) }

        val reward = simulationData.rewards.getValue(rewardId)
        val tile = simulationData.tiles.getValue(targetTileId)

        val taskData = TaskData(
            id,
            task.getInt(JsonKeys.TICK),
            shipId,
            reward,
            rewardShip,
            tile
        )

        val type = TaskType.createTaskType(task.getString(JsonKeys.TYPE))
        return when (type) {
            TaskType.COLLECT -> Result.success(CollectingTask(taskData))
            TaskType.EXPLORE -> Result.success(ExploreTask(taskData))
            TaskType.FIND -> Result.success(FindTask(taskData))
            TaskType.COOPERATE -> createAndValidateCoordinatingTask(taskData)
        }
    }

    private fun validateCommonTaskProperties(
        taskID: Int,
        rewardID: Int,
        shipID: Int,
        rewardShipID: Int,
        targetTileID: Int
    ): Result<Unit> {
        if (!simulationData.rewards.containsKey(rewardID)) {
            return Result.failure(
                ParserException("Task ID $taskID specified reward ID $rewardShipID, which does not exist")
            )
        } else if (!simulationData.ships.containsKey(shipID)) {
            return Result.failure(ParserException("Task ID $taskID specified ship ID $shipID, which does not exist"))
        } else if (!simulationData.ships.containsKey(rewardShipID)) {
            return Result.failure(ParserException("Task ID $taskID specified ship ID $shipID, which does not exist"))
        } else if (!simulationData.tiles.containsKey(targetTileID) ||
            simulationData.tiles.getValue(targetTileID).type == TileType.LAND
        ) {
            return Result.failure(ParserException("Task ID $taskID specified invalid tile with ID $targetTileID"))
        }

        return Result.success(Unit)
    }

    private fun createAndValidateCoordinatingTask(taskData: TaskData): Result<Task> {
        // get the corporation's ID the ship this task is assigned to
        val corpID = simulationData.corporations.values.first {
                corp ->
            corp.ships.any { ship -> ship.id == taskData.shipId }
        }.id

        val tile = taskData.destination

        // see if the specified destination tile is a harbor hosting a different corporation
        val targetIsHarborOfDifferentCorp = simulationData.corporations.values
            .filter { it.id != corpID }
            .any { it.harbors.contains(tile) }

        return if (!targetIsHarborOfDifferentCorp) {
            Result.failure(
                ParserException(
                    "Cooperating Task ${taskData.id} specified a target that isn't a harbor hosting a different " +
                        "corporation."
                )
            )
        } else {
            Result.success(CooperationTask(taskData))
        }
    }

    private fun validateAndCreateRewards(reward: JSONObject): Result<Reward> {
        var visibility: Int? = null
        var capacity: Int? = null
        var garbageType: GarbageType? = null
        val defaultKeys = setOf(JsonKeys.ID, JsonKeys.TYPE)
        val type = RewardType.createRewardType(reward.getString(JsonKeys.TYPE))
        // match the type with enum values to check for the further keys based on the type
        when (type) {
            RewardType.TELESCOPE -> {
                val allowedKeys = defaultKeys + setOf(JsonKeys.VISIBILITY_RANGE)
                if (reward.keySet() != allowedKeys) {
                    return Result.failure(ParserException("Invalid keys for reward telescope: ${reward.keySet()}"))
                }
                visibility = reward.getInt(JsonKeys.VISIBILITY_RANGE)
            }

            RewardType.CONTAINER -> {
                val allowedKeys = defaultKeys + setOf(JsonKeys.CAPACITY, JsonKeys.GARBAGE_TYPE)
                val garbage = reward.getString(JsonKeys.GARBAGE_TYPE)
                if (reward.keySet() != allowedKeys) {
                    return Result.failure(ParserException("Invalid keys for reward container: ${reward.keySet()}"))
                }
                capacity = reward.getInt(JsonKeys.CAPACITY)
                // check if the garbage type is of a proper type
                if (GarbageType.entries.toTypedArray().none { it.name == garbage }) {
                    return Result.failure(ParserException("Invalid garbage type for container reward: $garbage"))
                }
                garbageType = GarbageType.createGarbageType(garbage)
            }
            // default case only default keys are allowed
            else -> {
                if (reward.keySet() != defaultKeys) {
                    return Result.failure(ParserException("Invalid keys for reward: ${reward.keySet()}"))
                }
            }
        }
        return Result.success(
            Reward(
                reward.getInt(JsonKeys.ID),
                type,
                visibility,
                capacity,
                garbageType
            )
        )
    }

    /**
     * Checks task-reward assignment.
     */
    private fun crossValidateRewards(): Result<Unit> {
        // check if a reward is only assigned to one task
        val rewards = mutableSetOf<Reward>()
        for (task in tasksMap.values) {
            if (task.reward in rewards) {
                return Result.failure(ParserException("Reward ${task.reward.id} is assigned to two tasks."))
            }
            rewards.add(task.reward)
        }

        // check if reward type corresponds to the task type
        tasksMap.values
            .firstOrNull { task ->
                task.getType() != RewardType.correspondingTaskType(task.reward.type)
            }
            ?.let { return Result.failure(ParserException("Task ${it.id} has incompatible reward ${it.reward.id}")) }

        return Result.success(Unit)
    }

    private fun validateAndCreateGarbage(garbage: JSONObject): Result<Garbage> {
        val allowedKeys = setOf(JsonKeys.ID, JsonKeys.TYPE, JsonKeys.AMOUNT, JsonKeys.LOCATION)
        // check if the keys are valid for the garbage
        if (garbage.keySet() != allowedKeys) {
            return Result.failure(
                ParserException("Invalid keys for garbage: ${garbage.keySet()}")
            )
        }
        // create a garbage type
        val type = GarbageType.createGarbageType(garbage.getString(JsonKeys.TYPE))
        val location = garbage.getInt(JsonKeys.LOCATION)
        // check if the tile id exists in the map
        if (!simulationData.tiles.containsKey(location)) {
            return Result.failure(
                ParserException("Tile id ${garbage.getInt(JsonKeys.LOCATION)} does not exist for garbage")
            )
        }
        // validate garbage
        if (!validateGarbageType(garbage, type, location)) {
            return Result.failure(ParserException("Invalid garbage type for tile $location"))
        }

        val garb = Garbage(garbage.getInt(JsonKeys.ID), type, garbage.getInt(JsonKeys.AMOUNT))
        val tile = oceanMap.getTileByID(location)
        val locationTile = tile.getOrElse { return Result.failure(ParserException("Garbage location does not exist.")) }
        garbageToTile[garb] = locationTile
        return Result.success(garb)
    }

    private fun validateGarbageType(garbage: JSONObject, type: GarbageType, loc: Int): Boolean {
        val tileType = simulationData.tiles.getValue(loc).type
        val amount = garbage.getInt(JsonKeys.AMOUNT)
        return when (type) {
            GarbageType.PLASTIC -> {
                tileType != TileType.LAND
            }

            GarbageType.OIL -> {
                !(tileType == TileType.LAND || amount !in Constants.MIN_OIL_AMOUNT..Constants.MAX_OIL_AMOUNT)
            }

            GarbageType.CHEMICALS -> {
                !(tileType == TileType.LAND || tileType == TileType.DEEP_OCEAN)
            }
        }
    }

    /**
     * Validates that the amount of oil per tile does not exceed the upper limit.
     * Assumes that garbage has been parsed first and added to the tileToGarbage map of the oceanMap.
     * @return Result.failure(ParserException) if a tile exceeds the maximum oil amount.
     */
    private fun validateOilAmount(): Result<Unit> {
        for ((tile, garbageOnTile) in oceanMap.tileToGarbage) {
            val oilSumOnTile = garbageOnTile.filter { it.type == GarbageType.OIL }.sumOf { it.amount }
            if (oilSumOnTile > Constants.MAX_OIL_AMOUNT) {
                return Result.failure(ParserException("Tile with ID ${tile.id} has too much OIL."))
            }
        }
        return Result.success(Unit)
    }

    /**
     * The Events enum class that represents the different event types
     * used to create the event objects
     * @param keys the allowed keys for the event type
     */
    enum class Events(private val keys: Set<String>) {
        STORM(setOf(JsonKeys.LOCATION, JsonKeys.RADIUS, JsonKeys.SPEED, JsonKeys.DIRECTION)) {
            override fun createEventFromJson(json: JSONObject, simulationData: SimulationData): Result<Event> {
                // check if the tile id exists in the map
                if (!simulationData.tiles.containsKey(json.getInt(JsonKeys.LOCATION))) {
                    return Result.failure(
                        ParserException("Tile id ${json.getInt(JsonKeys.LOCATION)} does not exist on map")
                    )
                }
                if (!validateTileType(simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION)))) {
                    return Result.failure(ParserException("Invalid tile type for the event storm"))
                }
                val tile = simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION))
                return try {
                    Result.success(
                        StormEvent(
                            json.getInt(JsonKeys.RADIUS),
                            json.getInt(JsonKeys.SPEED),
                            json.getInt(JsonKeys.DIRECTION),
                            tile,
                            json.getInt(JsonKeys.ID),
                            json.getInt(JsonKeys.TICK),
                        )
                    )
                } catch (_: IllegalArgumentException) {
                    Result.failure(ParserException("Invalid direction of Storm."))
                }
            }
        },
        RESTRICTION(setOf(JsonKeys.DURATION, JsonKeys.LOCATION, JsonKeys.RADIUS)) {
            override fun createEventFromJson(json: JSONObject, simulationData: SimulationData): Result<Event> {
                // check if the tile id exists in the map
                if (!simulationData.tiles.containsKey(json.getInt(JsonKeys.LOCATION))) {
                    return Result.failure(
                        ParserException("Tile id ${json.getInt(JsonKeys.LOCATION)} does not exist")
                    )
                }
                if (!validateTileType(simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION)))) {
                    return Result.failure(
                        ParserException("Invalid tile type for the event restriction")
                    )
                }
                val tile = simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION))
                return Result.success(
                    RestrictionEvent(
                        json.getInt(JsonKeys.RADIUS),
                        json.getInt(JsonKeys.DURATION),
                        tile,
                        json.getInt(JsonKeys.ID),
                        json.getInt(JsonKeys.TICK)
                    )
                )
            }
        },
        OIL_SPILL(setOf(JsonKeys.LOCATION, JsonKeys.AMOUNT, JsonKeys.RADIUS)) {
            override fun createEventFromJson(json: JSONObject, simulationData: SimulationData): Result<Event> {
                // check if the tile id exists in the map
                if (!simulationData.tiles.containsKey(json.getInt(JsonKeys.LOCATION))) {
                    return Result.failure(
                        ParserException("Tile id ${json.getInt(JsonKeys.LOCATION)} does not exist on map.")
                    )
                }
                if (!validateTileType(simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION)))) {
                    return Result.failure(ParserException("Invalid tile type for the event oil spill"))
                }
                val tile = simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION))
                return Result.success(
                    OilEvent(
                        json.getInt(JsonKeys.RADIUS),
                        json.getInt(JsonKeys.AMOUNT),
                        tile,
                        json.getInt(JsonKeys.ID),
                        json.getInt(JsonKeys.TICK)
                    )
                )
            }
        },
        PIRATE_ATTACK(setOf(JsonKeys.SHIP_ID)) {
            override fun createEventFromJson(json: JSONObject, simulationData: SimulationData): Result<Event> {
                val shipId = json.getInt(JsonKeys.SHIP_ID)
                if (!simulationData.ships.containsKey(shipId)) {
                    return Result.failure(ParserException("Ship id $shipId does not exist."))
                }
                return Result.success(
                    PirateEvent(
                        json.getInt(JsonKeys.SHIP_ID),
                        json.getInt(JsonKeys.ID),
                        json.getInt(JsonKeys.TICK)
                    )
                )
            }
        },
        TYPHOON(setOf(JsonKeys.LOCATION, JsonKeys.STRENGTH, JsonKeys.RADIUS)) {
            override fun createEventFromJson(json: JSONObject, simulationData: SimulationData): Result<Event> {
                if (!simulationData.tiles.containsKey(json.getInt(JsonKeys.LOCATION))) {
                    return Result.failure(
                        ParserException("Tile id ${json.getInt(JsonKeys.LOCATION)} does not exist on the map")
                    )
                }
                if (!validateTileType(simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION)))) {
                    return Result.failure(ParserException("Invalid tile type for the event typhoon"))
                }

                if (json.getInt(JsonKeys.STRENGTH) !in MIN_STRENGTH..MAX_STRENGTH) {
                    return Result.failure(ParserException("Invalid strength value for the event typhoon"))
                }
                val location = simulationData.tiles.getValue(json.getInt(JsonKeys.LOCATION))
                return Result.success(
                    TyphoonEvent(
                        json.getInt(JsonKeys.RADIUS),
                        json.getInt(JsonKeys.STRENGTH),
                        location,
                        json.getInt(JsonKeys.ID),
                        json.getInt(JsonKeys.TICK)
                    )
                )
            }
        }
        ;

        /**
         * Returns the allowed keys for this event type
         */
        fun allowedKeysForEvents(): Set<String> {
            return setOf(JsonKeys.ID, JsonKeys.TYPE, JsonKeys.TICK) + keys
        }

        /**
         * Validate the tile type for the event
         */
        fun validateTileType(tile: Tile): Boolean {
            return tile.type != TileType.LAND
        }

        /**
         * Create an event from a JSON object
         * @param json the JSON object of the event
         * @param simulationData the simulation data
         * @return the event object
         */
        abstract fun createEventFromJson(json: JSONObject, simulationData: SimulationData): Result<Event>

        /**
         * Create an event from a JSON object
         */
        companion object {
            /**
             * Create an event from a JSON object
             */
            fun createEventFromJson(event: JSONObject, simulationData: SimulationData): Result<Event> {
                val eventType = Events.valueOf(event.getString(JsonKeys.TYPE))
                return if (eventType.allowedKeysForEvents() == event.keySet()) {
                    eventType.createEventFromJson(event, simulationData)
                } else {
                    return Result.failure(ParserException("Invalid keys for event: ${event.keySet()}"))
                }
            }
        }
    }
}
