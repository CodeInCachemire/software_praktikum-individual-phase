package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.TileType

/***
 * The PathFinder class is used to compute the shortest paths
 * to target tiles and objects on the map.
 */
class PathFinder(private val oceanMap: OceanMap) {

    /**
     * A small helper class that is passed as an argument to sub-methods to avoid long parameter lists.
     * It is also used to specify the scenario in which the PathFinder is used.
     * Depending on the conditions and the tiles or objects you are looking for,
     * you pass in different selector functions.
     *
     * @property distances A map that contains the distance (in tiles) for every tile we have reached so far.
     * @property selector The selector function is used to define what the PathFinder is looking for and
     * which tile should be selected if there are multiple goal tile candidates with the same distance.
     * The selector function should return null if a tile should not be considered as a goal tile candidate
     * (for example, a tile with no garbage, if we are looking for the closest garbage).
     * If a tile should be considered a goal tile candidate, it should return an Int,
     * which defines an order among the goal tile candidates (for example, if we are looking for the closest
     * garbage and there are multiple closest tiles with garbage on them, we should choose the tile
     * that has the garbage with the lowest id on it, so the selector should return the lowest garbage id
     * for every tile).
     * @property frontier A queue that contains all tiles that should be expanded next.
     * @property parents A map that contains the parent tile for every tile we have reached so far
     * (excluding the starting tile).
     */
    private data class Context(
        val distances: MutableMap<Tile, Int> = mutableMapOf(),
        val selector: (Tile) -> Int?
    ) {
        val frontier = ArrayDeque<Tile>()
        val parents = mutableMapOf<Tile, Tile>()
    }

    /**
     * Checks if any tile in the goal tiles set is reachable from the starting tile.
     *
     * @param start The tile from which to start the path finding.
     * @param goalTiles The set of tiles that should be considered as goal tile candidates.
     */
    fun isReachable(start: Tile, goalTiles: Set<Tile>): Boolean {
        val context = Context { tile -> tile.takeIf { it in goalTiles }?.id }
        val goalTile = findNearestGoalTile(start, context)
        return goalTile != null
    }

    /**
     * Checks if any tile in the goal tiles set is reachable from the starting tile
     * within the given distance (in tiles).
     *
     * @param start The tile from which to start the path finding.
     * @param goalTiles The set of tiles that should be considered as goal tile candidates.
     * @param distance The maximum distance reachable tiles can have.
     */
    fun isReachableWithinDistance(start: Tile, goalTiles: Set<Tile>, distance: Int): Boolean {
        // The selector function will return null if the tile is not in goal tiles
        // and the tile id if it is.
        val context = Context { tile -> tile.takeIf { it in goalTiles }?.id }
        val goalTile = findNearestGoalTile(start, context) ?: return false
        return context.distances.getValue(goalTile) <= distance
    }

    /**
     * Returns the shortest path to the closest tile in the goal tiles set or an empty list if no path exists.
     * In case there are multiple closest goal tile candidates, the one with the lower tile id is chosen.
     *
     * @param start The tile from which to start the path finding.
     * @param goalTiles The set of tiles that should be considered as goal tile candidates.
     */
    fun getShortestPathToTile(start: Tile, goalTiles: Set<Tile>): List<Tile> {
        // The selector function will return null if the tile is not in goal tiles
        // and the tile id if it is.
        val context = Context { tile -> tile.takeIf { it in goalTiles }?.id }
        val goalTile = findNearestGoalTile(start, context)
        return reconstructPath(goalTile, context)
    }

    /**
     * You move to the relevant harbor
     */
    fun getShortesPathToHarbor(start: Tile, goalTiles: Set<Tile>): List<Tile> {
        // The selector function will return null if the tile is not in goal tiles
        // and the tile id if it is.
        val context = Context { tile -> tile.takeIf { it in goalTiles }?.harborID }
        val goalTile = findNearestGoalTile(start, context)
        return reconstructPath(goalTile, context)
    }

    /**
     * Returns the shortest path to the closest tile containing garbage or an empty list if no path exists.
     * In case there are multiple closest tiles containing garbage, the one containing the garbage
     * with the lowest id is chosen.
     *
     * @param start The tile from which to start the path finding.
     * @param tileToGarbage A map containing an entry for every tile which has garbage on it that should be considered
     * as well as a sorted collection of the garbage that should be considered (for example, for a collecting ship,
     * that can only collect chemicals, tileToGarbage should just contain an entry for every tile with chemicals on it
     * as well as a sorted collection of all chemical garbage on that tile).
     */
    fun getShortestPathToGarbage(start: Tile, tileToGarbage: Map<Tile, Collection<Garbage>>): List<Tile> {
        // The selector function will return null if the tile has no garbage
        // and the lowest garbage id on the tile if it has.
        val context = Context { tile -> tileToGarbage[tile]?.firstOrNull()?.id }
        val goalTile = findNearestGoalTile(start, context)
        return reconstructPath(goalTile, context)
    }

    /**
     * Returns the shortest path to the closest tile containing ships or an empty list if no path exists.
     * In case there are multiple closest tiles containing ships, the one containing the ship
     * with the lowest id is chosen.
     *
     * @param start The tile from which to start the path finding.
     * @param tileToShips A map containing an entry for every tile which has ships on it that should be considered
     * as well as a sorted collection of the ships that should be considered (for example, for a coordinating ship
     * tileToShip should just contain an entry for every tile with ships that the coordinating ship
     * can actually cooperate with as well as a sorted collection of all the ships on that tile that
     * the coordinating ship can cooperate with).
     */
    fun getShortestPathToShip(start: Tile, tileToShips: Map<Tile, Collection<Ship>>): List<Tile> {
        // The selector function will return null if the tile has no ships
        // and the lowest ship id on the tile if it has.
        val context = Context { tile -> tileToShips[tile]?.firstOrNull()?.id }
        val goalTile = findNearestGoalTile(start, context)
        return reconstructPath(goalTile, context)
    }

    /**
     * Returns the shortest path to the closest tile without a restriction or an empty list if no path exists.
     *
     * @param start The tile from which to start the path finding.
     */
    fun escapeRestriction(start: Tile): List<Tile> {
        // The selector function will return null if the tile is restricted
        // and the tile id if the tile is not restricted.
        val context = Context { tile -> tile.takeIf { !it.restricted }?.id }
        val goalTile = findNearestGoalTile(start, context)
        return reconstructPath(goalTile, context)
    }

    /**
     * Returns the shortest path to the furthest reachable tile within maxDistance.
     *
     * @param start The tile from which to start the path finding.
     * @param maxDistance The maximum distance reachable tiles can have.
     */
    fun explore(start: Tile, maxDistance: Int): List<Tile> {
        // The selector function will return null if the distance to the tile is smaller
        // than the given maxDistance and the tile id otherwise.
        val distances = mutableMapOf<Tile, Int>()
        val context = Context(distances) { tile -> tile.takeIf { distances.getValue(it) == maxDistance }?.id }
        // If we did not reach any tile with maxDistance, we will get the reachable tile
        // with the largest distance instead.
        val goalTile = findNearestGoalTile(start, context) ?: getMaxReachableTile(context)
        return reconstructPath(goalTile, context)
    }

    /**
     * From all tiles that were reached according to the context, return the one with maximum distance.
     * In case there are multiple tiles with maximum distance, the one with the lowest tile id is chosen.
     */
    private fun getMaxReachableTile(context: Context): Tile? {
        val comparator = compareBy<Map.Entry<Tile, Int>> { it.value }
            .thenByDescending { it.key.id }
        return context.distances.maxWithOrNull(comparator)?.key
    }

    /**
     * Returns the nearest goal tile according to the selector in the given context
     * or null if no path to any goal tile candidate exists.
     */
    private fun findNearestGoalTile(start: Tile, context: Context): Tile? {
        context.distances[start] = 0
        context.frontier.add(start)

        while (context.frontier.isNotEmpty()) {
            val tile = context.frontier.first()
            // As soon as we find the first goal tile candidate,
            // all possible goal tile candidates with equal distance
            // should already be inserted in the frontier,
            // so we call the selectGoalTile method.
            if (context.selector(tile) != null) {
                return selectGoalTile(tile, context)
            }
            context.frontier.removeFirst()
            expand(tile, context)
        }
        return null
    }

    /**
     * Returns the goal tile (according to the selector) out of all closest goal tile candidates
     * that where reached in the given context.
     */
    private fun selectGoalTile(goalCandidate: Tile, context: Context): Tile? {
        val goalTile = context.frontier
            .takeWhile { context.distances[it] == context.distances[goalCandidate] }
            .minByOrNull { context.selector(it) ?: Int.MAX_VALUE }

        return goalTile
    }

    /**
     * Adds all valid neighbours of the given tile to the frontier and determines their distance.
     * The parent of the neighbour will be the expanded tile.
     */
    private fun expand(tile: Tile, context: Context) {
        // The neighbours will be inserted in the frontier in order of tile id.
        // Tiles will be taken out of the queue in insertion order.
        // This ensures that the correct shortest path is constructed according to the Specification:
        // "Additionally, in case of multiple shortest paths, on every tile for each next
        // possible tile on a shortest path the tile with the lowest ID is selected."
        val sortedNeighbouringTiles = oceanMap
            .getNeighbouringTiles(tile)
            .sorted()

        for (neighbour in sortedNeighbouringTiles) {
            if (isValidNeighbour(tile, neighbour, context)) {
                context.parents[neighbour] = tile
                context.distances[neighbour] = context.distances.getValue(tile) + 1
                context.frontier.add(neighbour)
            }
        }
    }

    /**
     * Checks if a given neighbour expanded from a given tile is valid and should be added to the frontier.
     */
    private fun isValidNeighbour(tile: Tile, neighbour: Tile, context: Context): Boolean {
        val notLand = neighbour.type != TileType.LAND
        val notVisited = neighbour !in context.distances
        // To also cover the case where we want to escape a restriction
        // we check that we never go from an unrestricted to a restricted tile
        // instead of just checking that the neighbour tile is not restricted.
        val notFreeToRestricted = !(!tile.restricted && neighbour.restricted)
        return notLand && notVisited && notFreeToRestricted
    }

    /**
     * Returns the shortest path to the furthest reachable tile within maxDistance.
     */
    private fun reconstructPath(tile: Tile?, context: Context): List<Tile> {
        // Goes backwards from the given tile, reconstructing the path by always going to the next parent
        // until context.parents[it] returns null which should be the starting tile.
        return generateSequence(tile) { context.parents[it] }.toList().asReversed()
    }
}
