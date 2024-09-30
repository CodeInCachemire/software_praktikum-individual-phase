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
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.TileType
import org.json.JSONArray
import org.json.JSONObject

/**
 * The MapParser class is responsible for parsing the map from the JSON file.
 */
class MapParser(private var simulationData: SimulationData) {
    private val tilesMap: MutableMap<Int, Tile> = mutableMapOf()
    private val tileCoordinates: MutableMap<Coordinate, Tile> = mutableMapOf()

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
        parseTiles(tiles).onFailure { return Result.failure(it) }
        simulationData.tiles.putAll(tilesMap)
        simulationData.oceanMap = OceanMap(tileCoordinates)
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
            }
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
        return Result.success(
            Tile(
                tile.getInt(JsonKeys.ID),
                TileType.createTileType(type),
                drift,
                coordinate,
                harbor
            )
        )
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
                setOf(
                    JsonKeys.ID,
                    JsonKeys.COORDINATES,
                    JsonKeys.CATEGORY,
                    JsonKeys.HARBOR
                )
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
