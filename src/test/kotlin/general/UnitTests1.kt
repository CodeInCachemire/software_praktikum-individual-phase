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
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.ShipyardStation
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.data.UnloadingStation
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType
import de.unisaarland.cs.se.selab.enums.TileType
import de.unisaarland.cs.se.selab.parser.SimulationData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UnitTests1 {

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
            null,
            UnloadingStation(100, setOf(GarbageType.PLASTIC, GarbageType.CHEMICALS))
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
        oceanMap.addShip(ship1, tile7)
        oceanMap.addShip(ship4, tile7)
        oceanMap.addShip(ship5, tile7)
        oceanMap.addShip(refShip7, tile7)
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
    fun testAssign() {
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1 else false")
        purchaseHandler.sendAndChooseShip(corporation)
        assertEquals(5, corporation.assignedBuyingShipId, "Hey it should be 5")
    }

    @Test
    fun testCheckPurchase() {
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1 else false")
        purchaseHandler.purchasePhase(corporation)
        assertEquals(5, corporation.assignedBuyingShipId, "Hey it should be 5")
    }

    @Test
    fun testFoundAShip() {
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1 else false")
        corporation.isBuyingShip = true
        corporation.purchaseCounter = 1
        purchaseHandler.updateOrderStatus(corporation)
        assertEquals(false, corporation.isBuyingShip, "It should be false")
    }

    @Test
    fun testPurchaseClearAll() {
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1 else false")
        corporation.isBuyingShip = true
        corporation.purchaseCounter = 1
        corporation.storeBoughtTileNdShip = Pair(tile7, null)
        corporation.assignedBuyingShipId = 6
        purchaseHandler.clearAll(corporation)
        assertEquals(false, corporation.isBuyingShip, "It should be false")
        assertEquals(-1, corporation.purchaseCounter, "It should be -1")
        assertEquals(Pair(null, null), corporation.storeBoughtTileNdShip, "Should be a null pair")
        assertEquals(-1, corporation.assignedBuyingShipId, "ID should be -1")
    }

    @Test
    fun testCheapeatShipyardStation() {
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1 else false")
        corporation.isBuyingShip = true
        corporation.purchaseCounter = 1
        val tileCheap = purchaseHandler.cheapeastShipyardStation(corporation)
        assertEquals(tile7, tileCheap, "It should be be equal to tile7")
    }

    @Test
    fun testBuyingLessCredits() {
        corporation.credits = 1999
        purchaseHandler.purchasePhase(corporation)
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1")
    }

    @Test
    fun testNotHavingACordinating() {
        corporation.ships.remove(ship5)
        purchaseHandler.purchasePhase(corporation)
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1")
    }

    @Test
    fun testHavingLowerIdCordShip() {
        val ship2 = Ship(2, ShipType.COORDINATING, corporation, 20, 10, 3000, 7, 1, mutableMapOf())
        corporation.ships.add(ship2)
        oceanMap.addShip(ship2, tile7)
        purchaseHandler.purchasePhase(corporation)
        assertEquals(2, corporation.assignedBuyingShipId, "Hey it should be -1")
    }

    @Test
    fun testCOODSHIPESCAPING() {
        ship5.behaviour = Behaviour.ESCAPING
        purchaseHandler.purchasePhase(corporation)
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1")
    }

    @Test
    fun testCOODREFUELING() {
        ship5.behaviour = Behaviour.REFUELING
        purchaseHandler.purchasePhase(corporation)
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1")
    }

    @Test
    fun testCOODRETASK() {
        ship5.receivingRefuel = true
        purchaseHandler.purchasePhase(corporation)
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1")
    }

    @Test
    fun testBuyindIdAfterPurchase() {
        ship5.returnToPurchase = true
        corporation.assignedBuyingShipId = 5
        shipHandler.purchasingPhase(corporation)
        assertEquals(-1, corporation.assignedBuyingShipId, "Hey it should be -1")
        assertEquals(false, ship5.returnToPurchase, "Hey it should be true")
        assertEquals(8, corporation.storeBoughtTileNdShip.second?.id, "NewShipId is 8")
    }

    @Test
    fun testShipHandlerBuying() {
        purchaseHandler.purchasePhase(corporation)
        shipHandler.purchasingPhase(corporation)
        corporation.purchaseCounter = 0
        corporation.isBuyingShip = false
        val refShip8 = Ship(8, ShipType.REFUELING, corporation, 40, 10, 3000, 10, 2, mutableMapOf())
        corporation.storeBoughtTileNdShip = Pair(tile7, refShip8)
        shipHandler.purchasingPhase(corporation)
        assertEquals(8, oceanMap.maxShipID, "New ship added to map")
    }

    @Test
    fun testDeliveryShip() {
        purchaseHandler.purchasePhase(corporation)
        ship5.receivingRefuel = true
        shipHandler.purchasingPhase(corporation)
        assertEquals(2400, corporation.credits, "Reefueling so never used")
    }

    @Test
    fun repairShip() {
        oceanMap.shipToTile[ship1] = tile7
        ship1.returnToRepair = true
        ship1.isDamaged = true
        ship1.behaviour = Behaviour.REPAIRING
        shipHandler.repairingPhase(corporation)
        assertEquals(true, ship1.isDamaged, "Ship should be damaged true")
        assertEquals(true, ship1.waitingAtAShipyard, "Ship should bwaiting")
        ship1.waitingAtHarbor = true
        shipHandler.repairingPhase(corporation)
        assertEquals(false, ship1.isDamaged, "Ship should be damaged true")
        assertEquals(2200, corporation.credits, "Ship should be damaged true")
    }

    @Test
    fun repairShipRefueling() {
        oceanMap.shipToTile[refShip7] = tile7
        refShip7.returnToRepair = true
        refShip7.isDamaged = true
        refShip7.behaviour = Behaviour.REPAIRING
        shipHandler.repairingPhase(corporation)
        assertEquals(true, refShip7.isDamaged, "Ship should be damaged true")
        assertEquals(true, refShip7.waitingAtAShipyard, "Ship should bwaiting")
        refShip7.waitingAtHarbor = true
        shipHandler.repairingPhase(corporation)
        assertEquals(false, refShip7.isDamaged, "Ship should be damaged true")
        assertEquals(2200, corporation.credits, "Ship should be damaged true")
    }

    @Test
    fun repairShipRefuelinFail() {
        corporation.credits = 100
        oceanMap.shipToTile[refShip7] = tile7
        refShip7.returnToRepair = true
        refShip7.isDamaged = true
        refShip7.behaviour = Behaviour.REPAIRING
        shipHandler.repairingPhase(corporation)
        assertEquals(true, refShip7.isDamaged, "Ship should be damaged true")
        assertEquals(false, refShip7.waitingAtAShipyard, "Ship should bwaiting")
    }

    @Test
    fun repairShipFixesDamage() {
        oceanMap.shipToTile[refShip7] = tile7
        refShip7.returnToRepair = true
        refShip7.isDamaged = true
        refShip7.behaviour = Behaviour.REPAIRING
        refShip7.maxVelocity = 40
        refShip7.acceleration = 3
        shipHandler.repairingPhase(corporation)
        assertEquals(true, refShip7.isDamaged, "Ship should be damaged true")
        assertEquals(true, refShip7.waitingAtAShipyard, "Ship should bwaiting")
        assertEquals(40, refShip7.maxVelocity, "Ship should bwaiting")
        assertEquals(3, refShip7.acceleration, "Ship should bwaiting")
        refShip7.waitingAtHarbor = true
        shipHandler.repairingPhase(corporation)
        assertEquals(false, refShip7.isDamaged, "Ship should be damaged true")
        assertEquals(60, refShip7.maxVelocity, "Ship should be damaged true")
        assertEquals(10, refShip7.acceleration, "Ship should be damaged true")
    }

    @Test
    fun unloadingAtAStation() {
        oceanMap.shipToTile[ship4] = tile7
        ship4.returnToUnload = true
        ship4.garbageCapacity[GarbageType.PLASTIC] = 0
        ship4.garbageCapacity[GarbageType.OIL] = 0
        ship4.garbageCapacity[GarbageType.CHEMICALS] = 0
        ship4.behaviour = Behaviour.UNLOADING
        shipHandler.unloadingPhase(corporation)
        assertEquals(Behaviour.UNLOADING, ship4.behaviour, "Ship should be damaged true")
        assertEquals(false, ship4.returnToUnload, "Ship should bwaiting")
        assertEquals(true, ship4.waitingAtAUnloadingStation, "Ship should bwaiting")
        ship4.waitingAtHarbor = true
        shipHandler.unloadingPhase(corporation)
        val plastic = ship4.garbageCapacity[GarbageType.PLASTIC] == 2000
        val chemicals = ship4.garbageCapacity[GarbageType.CHEMICALS] == 2000
        val oil = ship4.garbageCapacity[GarbageType.OIL] == 0
        assertEquals(true, plastic && chemicals && oil, "Both should be unloaded")
    }

    @Test
    fun unloadingAtAStationHEere() {
        oceanMap.shipToTile[ship4] = tile7
        ship4.returnToUnload = true
        ship4.garbageCapacity[GarbageType.CHEMICALS] = 0
        ship4.behaviour = Behaviour.UNLOADING
        shipHandler.unloadingPhase(corporation)
        assertEquals(Behaviour.UNLOADING, ship4.behaviour, "Ship should be damaged true")
        assertEquals(false, ship4.returnToUnload, "Ship should bwaiting")
        assertEquals(true, ship4.waitingAtAUnloadingStation, "Ship should bwaiting")
        ship4.waitingAtHarbor = true
        shipHandler.unloadingPhase(corporation)
        val chemicals = ship4.garbageCapacity[GarbageType.CHEMICALS] == 2000
        assertEquals(true, chemicals, "Both should be unloaded")
    }

    @Test
    fun unloadingAtAStationHEerAndRefuel() {
        oceanMap.shipToTile[ship4] = tile7
        ship4.returnToUnload = true
        ship4.garbageCapacity[GarbageType.CHEMICALS] = 0
        ship4.behaviour = Behaviour.UNLOADING
        shipHandler.unloadingPhase(corporation)
        assertEquals(Behaviour.UNLOADING, ship4.behaviour, "Ship should be damaged true")
        assertEquals(false, ship4.returnToUnload, "Ship should bwaiting")
        assertEquals(true, ship4.waitingAtAUnloadingStation, "Ship should bwaiting")
        ship4.waitingAtHarbor = true
        shipHandler.unloadingPhase(corporation)
        val chemicals = ship4.garbageCapacity[GarbageType.CHEMICALS] == 2000
        assertEquals(true, chemicals, "Both should be unloaded")
    }
}
