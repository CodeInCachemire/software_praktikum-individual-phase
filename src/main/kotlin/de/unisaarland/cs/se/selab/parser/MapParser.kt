package de.unisaarland.cs.se.selab.parser

import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.JsonValue
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.ValidationFailure
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.data.Coordinate
import de.unisaarland.cs.se.selab.data.Current
import de.unisaarland.cs.se.selab.data.Harbor
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.RefuelingStation
import de.unisaarland.cs.se.selab.data.ShipyardStation
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.data.UnloadingStation
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.TileType
import org.json.JSONArray
import org.json.JSONObject

/**
 * The MapParser class is responsible for parsing the map from the JSON file.
 */
class MapParser(private var simulationData: SimulationData) {
    private val tilesMap: MutableMap<Int, Tile> = mutableMapOf()
    private val tileCoordinates: MutableMap<Coordinate, Tile> = mutableMapOf()
    private val harborsMap: MutableMap<Int, Harbor> = mutableMapOf()
    private val tileToHarbor = mutableMapOf<Tile, Harbor>()
    private val harborToTile = mutableMapOf<Harbor, Tile>()
    private val harborTiles: MutableSet<Tile> = mutableSetOf()

    /**
     * Parses the map from the JSON file.
     */
    fun parse(jsonString: String): Result<Unit> {
        val schema = SchemaLoader.forURL("classpath://schema/map.schema").load()
        // Validate the JSON data with the schema
        val validator = Validator.create(schema, ValidatorConfig(FormatValidationPolicy.ALWAYS))
        val jsonInstance: JsonValue = JsonParser(jsonString).parse()
        val failure: ValidationFailure? = validator.validate(jsonInstance)
        if (failure != null) {
            return Result.failure(ParserException(failure.message))
        }

        val json = JSONObject(jsonString)
        val tiles = json.getJSONArray(JsonKeys.TILES)
        val harbors = json.getJSONArray(JsonKeys.HARBORS)
        parseTiles(tiles).onFailure { return Result.failure(it) }
        parseHarbors(harbors).onFailure { return Result.failure(it) }
        validateEachStationPresent()
        createMaps()
        simulationData.tiles.putAll(tilesMap)
        simulationData.harborMap.putAll(harborsMap)
        val oceanMap = OceanMap(tileCoordinates)
        oceanMap.harborsMap.putAll(harborsMap)
        oceanMap.tileToHarbor.putAll(tileToHarbor)
        oceanMap.harborToTile.putAll(harborToTile)
        oceanMap.harborTiles.addAll(harborTiles)
        simulationData.oceanMap = oceanMap
        return Result.success(Unit)
    }

    private fun parseTiles(tiles: JSONArray): Result<Unit> {
        tiles.forEach { tile ->
            if (tile is JSONObject) {
                val tileObject = validateAndCreateTiles(tile).getOrElse { return Result.failure(it) }
                // check if the tile id is already present
                if (tileObject.id in this.tilesMap.keys) {
                    return Result.failure(ParserException("Tile with id ${tileObject.id} already exists."))
                }
                tileCoordinates[tileObject.coordinate] = tileObject // add the tile to the coordinates map
                tilesMap[tileObject.id] = tileObject // add the tile to the map
            } else {
                return Result.failure(ParserException("Tile is not a JSONObject."))
            }
        }
        // validate neighbours of the tiles in the map
        if (!validateAdjacentTiles()) {
            return Result.failure(ParserException("Invalid neighbors in the map."))
        }

        // add harbors to the simulation data
        for (t in tilesMap.values) {
            if (t.harbor) {
                simulationData.harborTiles.add(t)
                harborTiles.add(t)
            }
        }

        return Result.success(Unit)
    }

    private fun parseHarbors(harbors: JSONArray): Result<Unit> {
        harbors.forEach { harbor ->
            if (harbor is JSONObject) {
                val harborID = harbor.getInt(JsonKeys.ID)
                val harborObject = validateAndCreateHarbors(harbor, harborID).getOrElse { return Result.failure(it) }
                if (harborObject.id in this.harborsMap.keys) {
                    return Result.failure(ParserException("Harbor with id $harborID already exists."))
                }
                // check if the tile id is already present
                harborsMap[harborObject.id] = harborObject // add the tile to the map
            } else {
                return Result.failure(ParserException("Harbor is not a JSONObject."))
            }
        }
        // add harbors to the simulation data
        return Result.success(Unit)
    }

    private fun validateAndCreateHarbors(harbor: JSONObject, harborID: Int): Result<Harbor> {
        // check if the keys are valid
        if (!validateKeySetOfHarbor(harbor)) {
            return Result.failure(ParserException("Invalid keys in harbor $harbor."))
        }
        validateHarborWithTile(harbor).getOrElse { return Result.failure(it) }
        val harborCorporationIntIds = harbor.getJSONArray(JsonKeys.CORPORATIONS)
            .map { (it ?: error("Garbage type cannot be null")) as Int }
        if (harborCorporationIntIds.isEmpty()) {
            return Result.failure(ParserException("Harbor $harborID has no corporations, must have atleast one corp"))
        }
        // This is some messed up shit, someone help me, I hope my parser is not constricting too much.
        val shipyardStation = if (harbor.has(JsonKeys.SHIPYARD_STATION)) {
            createShipyardStation(harbor)
        } else {
            null
        }
        val refuelStation = if (harbor.has(JsonKeys.REFUELING_STATION)) {
            val refuelCost = harbor.getJSONObject(JsonKeys.REFUELING_STATION).getInt(JsonKeys.REFUEL_COST)
            val refuelTimes = harbor.getJSONObject(JsonKeys.REFUELING_STATION).getInt(JsonKeys.REFUEL_TIMES)
            RefuelingStation(refuelCost, refuelTimes)
        } else {
            null
        }
        val unloadStation = if (harbor.has(JsonKeys.UNLOADING_STATION)) {
            val unloadReturn = harbor.getJSONObject(JsonKeys.UNLOADING_STATION).getInt(JsonKeys.UNLOAD_RETURN)
            val garbageTypesUnload = harbor.getJSONObject(
                JsonKeys.UNLOADING_STATION
            ).getJSONArray(JsonKeys.GARBAGE_TYPES)
            val garbageTypesUnloadValues = garbageTypesUnload
                .map { GarbageType.valueOf((it ?: error("Garbage type cannot be null")) as String) }.toSet()
            UnloadingStation(unloadReturn, garbageTypesUnloadValues)
        } else {
            null
        }
        val numOfStations = listOfNotNull(refuelStation, shipyardStation, unloadStation).size
        if (numOfStations > 2) {
            return Result.failure(ParserException("Harbor $harborID has 3 stations instead of 2."))
        }
        return Result.success(
            Harbor(
                harbor.getInt(JsonKeys.ID),
                harbor.getInt(JsonKeys.LOCATION),
                harborCorporationIntIds.toSet(),
                shipyardStation,
                refuelStation,
                unloadStation
            )
        )
    }

    private fun createMaps() {
        for (harbor in harborsMap.values) {
            val tile = tilesMap.getValue(harbor.location)
            tileToHarbor[tile] = harbor
            harborToTile[harbor] = tile
        }
    }

    private fun createShipyardStation(harbor: JSONObject): ShipyardStation {
        val repairCost = harbor.getJSONObject(JsonKeys.SHIPYARD_STATION).getInt(JsonKeys.REPAIR_COST)
        val shipCost = harbor.getJSONObject(JsonKeys.SHIPYARD_STATION).getInt(JsonKeys.SHIP_COST)
        val deliveryTime = harbor.getJSONObject(JsonKeys.SHIPYARD_STATION).getInt(JsonKeys.DELIVERY_TIME)
        val shiPropsJson = harbor.getJSONObject(JsonKeys.SHIPYARD_STATION).getJSONObject(JsonKeys.SHIP_PROPERTIES)
        val maxVelocity = shiPropsJson.getInt(JsonKeys.MAX_VELOCITY)
        val acceleration = shiPropsJson.getInt(JsonKeys.ACCELERATION)
        val fuelConsumption = shiPropsJson.getInt(JsonKeys.FUEL_CONSUMPTION)
        val fuelCapacity = shiPropsJson.getInt(JsonKeys.FUEL_CAPACITY)
        val refuelingCapacity = shiPropsJson.getInt(JsonKeys.REFUELING_CAPACITY)
        val refuelingTime = shiPropsJson.getInt(JsonKeys.REFUELING_TIME)
        return ShipyardStation(
            repairCost, shipCost, deliveryTime, maxVelocity,
            acceleration, fuelConsumption, fuelCapacity, refuelingCapacity, refuelingTime
        )
    }

    private fun validateHarborWithTile(harbor: JSONObject): Result<Unit> {
        val harborID = harbor.getInt(JsonKeys.ID)
        val location = harbor.getInt(JsonKeys.LOCATION)
        if (!tilesMap.containsKey(location)) {
            return Result.failure(ParserException("Harbor $harborID has an invalid location on tile $location."))
        }
        if (tilesMap.getValue(location).type != TileType.SHORE) {
            return Result.failure(ParserException("Harbor $harborID on tile $location which is not shore."))
        }
        if (!tilesMap.getValue(location).harbor) {
            return Result.failure(ParserException("Harbor $harborID is on tile $location which has a false harbor."))
        }
        if (tilesMap.getValue(location).harborID != harborID) {
            return Result.failure(
                ParserException("Harbor $harborID is on tile $location which has a harbor with a different ID.")
            )
        }
        return Result.success(Unit)
    }

    private fun validateEachStationPresent(): Result<Unit> {
        val shipYardNum = harborsMap.values.fold(
            0
        ) { acc, harbor -> if (harbor.shipyardStation != null) acc + 1 else acc }
        val refuelNum = harborsMap.values.fold(
            0
        ) { acc, harbor -> if (harbor.refuelingStation != null) acc + 1 else acc }
        val unloadNum = harborsMap.values.fold(
            0
        ) { acc, harbor -> if (harbor.shipyardStation != null) acc + 1 else acc }
        if (shipYardNum < 1 || refuelNum < 1 || unloadNum < 1) {
            return Result.failure(
                ParserException("One of each station is not present")
            )
        }
        return Result.success(Unit)
    }

    private fun validateAndCreateTiles(tile: JSONObject): Result<Tile> {
        // check if the keys are valid
        if (!validateKeySet(tile)) {
            return Result.failure(ParserException("Invalid keys in tile $tile."))
        }

        // validate and instantiate coordinates if they are valid
        if (!validateCoordinates(tile.getJSONObject(JsonKeys.COORDINATES))) {
            return Result.failure(ParserException("Invalid coordinates in tile $tile."))
        }
        val coordinate =
            Coordinate(
                tile.getJSONObject(JsonKeys.COORDINATES).getInt(JsonKeys.X),
                tile.getJSONObject(JsonKeys.COORDINATES).getInt(JsonKeys.Y)
            )
        if (coordinate in tileCoordinates.keys) {
            return Result.failure(ParserException("Coordinate $coordinate already exists."))
        }

        val type = tile.getString(JsonKeys.CATEGORY)
        // get the drift of the tile
        val (harbor, drift) = validateSpecialTiles(tile).getOrElse { return Result.failure(it) }
        // create tile
        val createdTile: Tile
        if (harbor) {
            val harborID = tile.getInt(JsonKeys.HARBORID)
            createdTile =
                Tile(
                    tile.getInt(JsonKeys.ID),
                    TileType.createTileType(type),
                    drift,
                    coordinate,
                    harbor,
                    harborID
                )
        } else {
            createdTile =
                Tile(
                    tile.getInt(JsonKeys.ID),
                    TileType.createTileType(type),
                    drift,
                    coordinate,
                    harbor,
                    null
                )
        }
        return Result.success(createdTile)
    }

    /**
     *
     */
    private fun validateKeySetOfHarbor(harbor: JSONObject): Boolean {
        val mandatoryKeys = setOf(
            JsonKeys.ID,
            JsonKeys.LOCATION,
            JsonKeys.CORPORATIONS
        )
        val shipyardKeys = setOf(
            JsonKeys.REPAIR_COST,
            JsonKeys.SHIP_COST,
            JsonKeys.DELIVERY_TIME,
            JsonKeys.SHIP_PROPERTIES
        )
        val refuelingKeys = setOf(
            JsonKeys.REFUEL_COST,
            JsonKeys.REFUEL_TIMES
        )
        val unloadingKeys = setOf(
            JsonKeys.UNLOAD_RETURN,
            JsonKeys.GARBAGE_TYPES
        )
        val shipPropKeys = setOf(
            JsonKeys.MAX_VELOCITY,
            JsonKeys.ACCELERATION,
            JsonKeys.FUEL_CONSUMPTION,
            JsonKeys.FUEL_CAPACITY,
            JsonKeys.REFUELING_CAPACITY,
            JsonKeys.REFUELING_TIME
        )

        if (!harbor.keySet().containsAll(mandatoryKeys)) {
            return false
        }

        if (harbor.has(JsonKeys.SHIPYARD_STATION)) {
            val shipyardStation = harbor.getJSONObject(JsonKeys.SHIPYARD_STATION)
            if (!shipyardStation.keySet().containsAll(shipyardKeys) ||
                !shipyardStation.getJSONObject(JsonKeys.SHIP_PROPERTIES).keySet().containsAll(shipPropKeys)
            ) {
                return false
            }
        }
        if (harbor.has(JsonKeys.REFUELING_STATION)) {
            if (!harbor.getJSONObject(JsonKeys.REFUELING_STATION).keySet().containsAll(refuelingKeys)) {
                return false
            }
        }
        if (harbor.has(JsonKeys.UNLOADING_STATION)) {
            if (!harbor.getJSONObject(JsonKeys.UNLOADING_STATION).keySet().containsAll(unloadingKeys)) {
                return false
            }
        }
        return true
    }

    /**
     * Validate the JSON key set of the tile.
     */
    private fun validateKeySet(tile: JSONObject): Boolean {
        if (!tile.keySet().contains(JsonKeys.CATEGORY)) {
            return false
        }
        val allowedKeys = when (tile.getString(JsonKeys.CATEGORY)) {
            JsonKeys.LAND, JsonKeys.SHALLOW_OCEAN -> {
                setOf(
                    JsonKeys.ID,
                    JsonKeys.COORDINATES,
                    JsonKeys.CATEGORY
                )
            }

            JsonKeys.SHORE -> {
                if (tile.getBoolean(JsonKeys.HARBOR)) {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.CATEGORY,
                        JsonKeys.HARBOR,
                        JsonKeys.HARBORID
                    )
                } else {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.CATEGORY,
                        JsonKeys.HARBOR,
                    )
                }
            }

            JsonKeys.DEEP_OCEAN -> {
                if (tile.getBoolean(JsonKeys.CURRENT)) {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.DIRECTION,
                        JsonKeys.SPEED,
                        JsonKeys.INTENSITY,
                        JsonKeys.CURRENT,
                        JsonKeys.CATEGORY
                    )
                } else {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.CURRENT,
                        JsonKeys.CATEGORY
                    )
                }
            }

            else -> { return false }
        }
        return tile.keySet() == allowedKeys
    }

    /**
     * Validate compatibility of adjacent tiles.
     */
    private fun validateAdjacentTiles(): Boolean {
        // go through the coordinates that we collected & check if the neighbours are valid
        for ((c, t) in tileCoordinates) {
            val neighborsCoord = c.getNeighbours()
            val neighborsTiles = neighborsCoord.mapNotNull { tileCoordinates[it] }
            if (!isCompatibleType(t.type, neighborsTiles)) {
                return false
            }
        }
        return true
    }

    /**
     * Check if tile type is compatible with other tile types.
     */
    private fun isCompatibleType(type: TileType, neighbors: List<Tile>): Boolean {
        return when (type) {
            TileType.LAND -> { neighbors.all { it.type == TileType.LAND || it.type == TileType.SHORE } }
            TileType.SHORE -> {
                neighbors.all {
                    it.type == TileType.LAND || it.type == TileType.SHORE ||
                        it.type == TileType.SHALLOW_OCEAN
                }
            }
            TileType.SHALLOW_OCEAN -> {
                neighbors.all {
                    it.type == TileType.SHORE || it.type == TileType.SHALLOW_OCEAN ||
                        it.type == TileType.DEEP_OCEAN
                }
            }
            TileType.DEEP_OCEAN -> {
                neighbors.all { it.type == TileType.SHALLOW_OCEAN || it.type == TileType.DEEP_OCEAN }
            }
        }
    }

    /**
     * Validate the JSON key set coordinates.
     */
    private fun validateCoordinates(coordinates: JSONObject): Boolean {
        return coordinates.keySet() == setOf(JsonKeys.X, JsonKeys.Y)
    }

    /**
     * Validate tiles with special properties (Deep Ocean with current or Shore with harbor)
     * @param tile the JSON object of the tile
     * @return true if the keys are valid, false otherwise
     */
    private fun validateSpecialTiles(tile: JSONObject): Result<Pair<Boolean, Current?>> {
        var drift: Current? = null
        var harbor = false
        val type = tile.getString(JsonKeys.CATEGORY)

        // get the drift of the tile
        if (type == JsonKeys.DEEP_OCEAN && tile.getBoolean(JsonKeys.CURRENT)) {
            val direction = if (tile.get(JsonKeys.DIRECTION) is Int) {
                tile.getInt(JsonKeys.DIRECTION)
            } else {
                return Result.failure(ParserException("Invalid direction."))
            }
            try {
                drift = Current(direction, tile.getInt(JsonKeys.SPEED), tile.getInt(JsonKeys.INTENSITY))
            } catch (_: IllegalArgumentException) {
                return Result.failure(ParserException("Invalid direction."))
            }
            if (
                drift.speed !in Constants.MIN_DRIFT_SPEED..Constants.MAX_DRIFT_SPEED ||
                drift.intensity !in Constants.MIN_DRIFT_INTENSITY..Constants.MAX_DRIFT_INTENSITY
            ) {
                return Result.failure(ParserException("Invalid current speed/intensity values in tile $tile."))
            }
        } else if (type == JsonKeys.SHORE && tile.getBoolean(JsonKeys.HARBOR)) {
            harbor = tile.getBoolean(JsonKeys.HARBOR)
        }
        return Result.success(Pair(harbor, drift))
    }
}
