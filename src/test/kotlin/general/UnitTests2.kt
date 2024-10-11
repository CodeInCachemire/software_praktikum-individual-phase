package general

import de.unisaarland.cs.se.selab.control.MovementHandler
import de.unisaarland.cs.se.selab.control.PathFinder
import de.unisaarland.cs.se.selab.control.PurchaseHandler
import de.unisaarland.cs.se.selab.control.ShipHandler
import de.unisaarland.cs.se.selab.control.VisibilityHandler
import de.unisaarland.cs.se.selab.data.Coordinate
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.Harbor
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.RefuelingStation
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.ShipyardStation
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType
import de.unisaarland.cs.se.selab.enums.TileType
import de.unisaarland.cs.se.selab.event.PirateEvent
import de.unisaarland.cs.se.selab.event.RestrictionEvent
import de.unisaarland.cs.se.selab.event.TyphoonEvent
import de.unisaarland.cs.se.selab.parser.SimulationData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UnitTests2 {

    private lateinit var corporation: Corporation
    private lateinit var visibilityHandler: VisibilityHandler
    private lateinit var purchaseHandler: PurchaseHandler
    private lateinit var simulationData: SimulationData
    private lateinit var pathFinder: PathFinder

    private lateinit var refShip7: Ship
    private lateinit var ship5: Ship
    private lateinit var ship4: Ship
    private lateinit var ship1: Ship
    private lateinit var garbage: Garbage
    private lateinit var movementHandler: MovementHandler
    private lateinit var shipHandler: ShipHandler
    private lateinit var corporations: List<Corporation>

    val harbor =
        Harbor(
            0,
            7,
            setOf(1),
            ShipyardStation(
                200, 2000, 2, 60,
                25, 10, 25000, 3000, 2
            ),
            RefuelingStation(450, 10),
            null
        )

    val tile1 = Tile(1, TileType.SHORE, null, Coordinate(0, 0), false, null)
    val tile2 = Tile(2, TileType.SHORE, null, Coordinate(0, 1), false, null)
    val tile3 = Tile(3, TileType.SHORE, null, Coordinate(0, 2), false, null)
    val tile4 = Tile(4, TileType.SHORE, null, Coordinate(0, 3), false, null)
    val tile5 = Tile(5, TileType.SHORE, null, Coordinate(0, 4), false, null)
    val tile6 = Tile(6, TileType.SHORE, null, Coordinate(0, 5), false, null)
    val tile11 = Tile(11, TileType.SHALLOW_OCEAN, null, Coordinate(1, 0), false, null)
    val tile12 = Tile(12, TileType.SHALLOW_OCEAN, null, Coordinate(1, 1), false, null)
    val tile13 = Tile(13, TileType.SHALLOW_OCEAN, null, Coordinate(1, 2), false, null)
    val tile14 = Tile(14, TileType.SHALLOW_OCEAN, null, Coordinate(1, 3), false, null)
    val tile15 = Tile(15, TileType.SHALLOW_OCEAN, null, Coordinate(1, 4), false, null)
    val tile7 = Tile(7, TileType.SHALLOW_OCEAN, null, Coordinate(1, 5), true, 0)

    val cordToTile = mutableMapOf(
        Coordinate(0, 0) to tile1,
        Coordinate(0, 1) to tile2,
        Coordinate(0, 2) to tile3,
        Coordinate(0, 3) to tile4,
        Coordinate(0, 4) to tile5,
        Coordinate(0, 5) to tile6,
        Coordinate(1, 0) to tile11,
        Coordinate(1, 1) to tile12,
        Coordinate(1, 2) to tile13,
        Coordinate(1, 3) to tile14,
        Coordinate(1, 4) to tile15,
        Coordinate(1, 5) to tile7
    )

    val oceanMap = OceanMap(cordToTile)

    @BeforeEach
    fun setUp() {
        corporation = Corporation(
            1, setOf(tile7),
            listOf(GarbageType.OIL, GarbageType.CHEMICALS, GarbageType.PLASTIC), 2400
        )
        ship1 = Ship(1, ShipType.SCOUTING, corporation, 40, 10, 3000, 10, 2, mutableMapOf())
        ship4 = Ship(
            4, ShipType.COLLECTING, corporation, 40, 10,
            3000, 10, 2,
            mutableMapOf(
                Pair(GarbageType.PLASTIC, 2000),
                Pair(GarbageType.CHEMICALS, 2000), Pair(GarbageType.OIL, 2000)
            )
        )
        ship5 = Ship(5, ShipType.COORDINATING, corporation, 20, 10, 3000, 7, 1, mutableMapOf())
        refShip7 = Ship(7, ShipType.REFUELING, corporation, 60, 10, 3000, 10, 2, mutableMapOf())
        oceanMap.tileToHarbor[tile7] = harbor
        oceanMap.harborToTile[harbor] = tile7
        oceanMap.harborsMap[harbor.id] = harbor
        oceanMap.harborTiles.add(tile7)
        oceanMap.addShip(ship1, tile1)
        oceanMap.addShip(ship4, tile1)
        oceanMap.addShip(ship5, tile1)
        oceanMap.addShip(refShip7, tile1)
        corporation.ships.add(ship1)
        corporation.ships.add(ship4)
        corporation.ships.add(ship5)
        corporation.ships.add(refShip7)
        garbage = Garbage(1, GarbageType.CHEMICALS, 2000)
        oceanMap.addGarbage(garbage, tile7)
        corporations = listOf(corporation)
        simulationData = SimulationData(oceanMap)
        pathFinder = PathFinder(oceanMap)
        visibilityHandler = VisibilityHandler(oceanMap, corporations)
        purchaseHandler = PurchaseHandler(oceanMap, pathFinder)
        movementHandler = MovementHandler(pathFinder, oceanMap, visibilityHandler, purchaseHandler)
        shipHandler = ShipHandler(oceanMap, visibilityHandler, corporations, purchaseHandler, simulationData)
    }

    @Test
    fun move() {
        oceanMap.shipToTile[ship5] = tile5
        oceanMap.tileToShip[tile5] = sortedSetOf(ship5)
        ship5.fuel = 800
        movementHandler.movementPhase(corporation)
        assertNotEquals(tile5, oceanMap.getShipTile(ship5), "Same tile as harbor")
        movementHandler.movementPhase(corporation)
        assertNotEquals(tile5, oceanMap.getShipTile(ship5), "Same tile as harbor")
    }

    @Test
    fun refuelingNormal() {
        oceanMap.shipToTile[ship5] = tile5
        oceanMap.tileToShip[tile5] = sortedSetOf(ship5)
        ship5.fuel = 200
        ship5.returnToRefuel = true
        ship5.behaviour = Behaviour.REFUELING
        movementHandler.movementPhase(corporation)
        assertEquals(tile7, oceanMap.getShipTile(ship5), "MvoingNormal")
        shipHandler.refuelingPhase(corporation)
        ship5.waitingAtHarbor = true
        ship5.waitingAtARefuelingStation = true
        assertEquals(1950, corporation.credits)
        shipHandler.refuelingPhase(corporation)
        assertEquals(3000, ship5.fuel)
    }

    @Test
    fun moveRefuelingShip() {
        oceanMap.addShip(ship5, tile1)
        oceanMap.addShip(refShip7, tile2)
        ship5.fuel = 800
        movementHandler.movementPhase(corporation)
        assertEquals(tile1, oceanMap.getShipTile(refShip7), "MvoingNormal")
    }

    @Test
    fun movePathFinder() {
        val path = pathFinder.getShortesPathToHarbor(tile7, setOf(tile7))
        val tilesList = listOf(
            Tile(
                id = 7,
                type = TileType.SHALLOW_OCEAN,
                current = null,
                coordinate = Coordinate(x = 1, y = 5),
                harbor = true,
                harborID = 0
            )
        )
        assertEquals(tilesList, path)
    }

    @Test
    fun movePathFinder2() {
        val path = pathFinder.getShortesPathToHarbor(tile6, setOf(tile7))
        val tilesList = mutableListOf(
            Tile(
                id = 6,
                type = TileType.SHORE,
                current = null,
                coordinate = Coordinate(x = 0, y = 5),
                harbor = false,
                harborID = null
            ),
            Tile(
                id = 7,
                type = TileType.SHALLOW_OCEAN,
                current = null,
                coordinate = Coordinate(x = 1, y = 5),
                harbor = true,
                harborID = 0
            )
        )

        assertEquals(tilesList, path)
    }

    @Test
    fun typhoon() {
        val typhoon = TyphoonEvent(1, 4, tile1, 1, 1)
        ship4.garbageCapacity[GarbageType.PLASTIC] = 200
        ship4.garbageCapacity[GarbageType.OIL] = 600
        ship4.garbageCapacity[GarbageType.CHEMICALS] = 900

        typhoon.execute(oceanMap)
        assertEquals(true, ship1.isDamaged)
        assertEquals(true, ship4.isDamaged)
        assertEquals(true, ship5.isDamaged)
        assertEquals(true, refShip7.isDamaged)
        shipHandler.collectionPhase(corporation)
        assertEquals(200, ship4.garbageCapacity[GarbageType.PLASTIC])
        assertEquals(600, ship4.garbageCapacity[GarbageType.OIL])
        assertEquals(900, ship4.garbageCapacity[GarbageType.CHEMICALS])
    }

    @Test
    fun PirateEventAtttack() {
        ship5.setOriginalRefuelCap(3000, 2)
        ship5.activeRefueling = true
        ship5.shipToRefuel = ship1
        ship1.receivingRefuel = true
        ship5.tickCounter = 1
        val pirateEvent = PirateEvent(ship1.id, 1, 0)
        pirateEvent.execute(oceanMap)
        assertEquals(false, ship5.activeRefueling)
        assertEquals(2, ship5.tickCounter)
        assertEquals(null, ship5.shipToRefuel)
    }

    @Test
    fun restriction() {
        ship5.setOriginalRefuelCap(3000, 2)
        ship5.activeRefueling = true
        ship5.shipToRefuel = ship1
        ship1.receivingRefuel = true
        ship5.tickCounter = 1
        val pirateEvent = RestrictionEvent(1, 1, tile1, 1, 1)
        pirateEvent.execute(oceanMap)
        assertEquals(true, ship5.activeRefueling)
        assertEquals(1, ship5.tickCounter)
        assertEquals(ship1, ship5.shipToRefuel)
    }
}
