package pl.put.poznan.buildinginfo.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Location hierarchy: {@link Room}, {@link Level}, and {@link Building}.
 *
 * <p>Tests cover area/cube/light/heating computations at all three levels,
 * edge cases (empty containers, zero-area/cube rooms), and the Visitor double-dispatch.</p>
 */
class LocationModelTest {

    // ── shared fixtures ──────────────────────────────────────────────────────

    /** Room A: area=20, cube=50, heating=500, light=200 */
    private Room roomA;

    /** Room B: area=30, cube=90, heating=900, light=300 */
    private Room roomB;

    /** Room with zero area and zero cube to test guard clauses */
    private Room zeroRoom;

    @BeforeEach
    void setUp() {
        roomA    = new Room(1, "Room A", 20.0, 50.0, 500.0, 200.0);
        roomB    = new Room(2, "Room B", 30.0, 90.0, 900.0, 300.0);
        zeroRoom = new Room(3, "Zero Room", 0.0, 0.0, 0.0, 0.0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Room tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Room")
    class RoomTests {

        @Test
        @DisplayName("getArea returns the directly stored area")
        void getArea_returnsStoredValue() {
            assertEquals(20.0, roomA.getArea(), 1e-9);
        }

        @Test
        @DisplayName("getCube returns the directly stored volume")
        void getCube_returnsStoredValue() {
            assertEquals(50.0, roomA.getCube(), 1e-9);
        }

        @Test
        @DisplayName("getLight returns the directly stored lighting power")
        void getLight_returnsStoredValue() {
            assertEquals(200.0, roomA.getLight(), 1e-9);
        }

        @Test
        @DisplayName("getHeating returns the directly stored heating consumption")
        void getHeating_returnsStoredValue() {
            assertEquals(500.0, roomA.getHeating(), 1e-9);
        }

        @Test
        @DisplayName("getLightPerArea returns light / area")
        void getLightPerArea_correctRatio() {
            // 200 / 20 = 10.0
            assertEquals(10.0, roomA.getLightPerArea(), 1e-9);
        }

        @Test
        @DisplayName("getHeatingPerCube returns heating / cube")
        void getHeatingPerCube_correctRatio() {
            // 500 / 50 = 10.0
            assertEquals(10.0, roomA.getHeatingPerCube(), 1e-9);
        }

        @Test
        @DisplayName("getLightPerArea returns 0 when area is zero (no division by zero)")
        void getLightPerArea_zeroArea_returnsZero() {
            assertEquals(0.0, zeroRoom.getLightPerArea(), 1e-9);
        }

        @Test
        @DisplayName("getHeatingPerCube returns 0 when cube is zero (no division by zero)")
        void getHeatingPerCube_zeroCube_returnsZero() {
            assertEquals(0.0, zeroRoom.getHeatingPerCube(), 1e-9);
        }

        @Test
        @DisplayName("getId / getName return the values passed to the constructor")
        void idAndName_returnConstructorValues() {
            assertEquals(1, roomA.getId());
            assertEquals("Room A", roomA.getName());
        }

        @Test
        @DisplayName("Setters update fields that getters then return")
        void setters_updateFields() {
            Room r = new Room();
            r.setId(99);
            r.setName("Updated");
            r.setArea(55.0);
            r.setCube(110.0);
            r.setHeating(800.0);
            r.setLight(400.0);

            assertEquals(99, r.getId());
            assertEquals("Updated", r.getName());
            assertEquals(55.0, r.getArea(), 1e-9);
            assertEquals(110.0, r.getCube(), 1e-9);
            assertEquals(800.0, r.getHeating(), 1e-9);
            assertEquals(400.0, r.getLight(), 1e-9);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Level tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Level")
    class LevelTests {

        private Level level;

        @BeforeEach
        void buildLevel() {
            level = new Level(10, "Ground Floor");
            level.addRoom(roomA);
            level.addRoom(roomB);
        }

        @Test
        @DisplayName("getArea sums areas of all rooms")
        void getArea_sumOfRoomAreas() {
            assertEquals(50.0, level.getArea(), 1e-9);  // 20 + 30
        }

        @Test
        @DisplayName("getCube sums cubes of all rooms")
        void getCube_sumOfRoomCubes() {
            assertEquals(140.0, level.getCube(), 1e-9); // 50 + 90
        }

        @Test
        @DisplayName("getLight sums lighting of all rooms")
        void getLight_sumOfRoomLights() {
            assertEquals(500.0, level.getLight(), 1e-9); // 200 + 300
        }

        @Test
        @DisplayName("getHeating sums heating of all rooms")
        void getHeating_sumOfRoomHeatings() {
            assertEquals(1400.0, level.getHeating(), 1e-9); // 500 + 900
        }

        @Test
        @DisplayName("getLightPerArea = total light / total area")
        void getLightPerArea_totalLightOverTotalArea() {
            // 500 / 50 = 10.0
            assertEquals(10.0, level.getLightPerArea(), 1e-9);
        }

        @Test
        @DisplayName("getHeatingPerCube = total heating / total cube")
        void getHeatingPerCube_totalHeatingOverTotalCube() {
            // 1400 / 140 = 10.0
            assertEquals(10.0, level.getHeatingPerCube(), 1e-9);
        }

        @Test
        @DisplayName("Empty level: getArea returns 0")
        void emptyLevel_area_returnsZero() {
            Level empty = new Level(99, "Empty");
            assertEquals(0.0, empty.getArea(), 1e-9);
        }

        @Test
        @DisplayName("Empty level: getLightPerArea returns 0 (no division by zero)")
        void emptyLevel_lightPerArea_returnsZero() {
            Level empty = new Level(99, "Empty");
            assertEquals(0.0, empty.getLightPerArea(), 1e-9);
        }

        @Test
        @DisplayName("Empty level: getHeatingPerCube returns 0 (no division by zero)")
        void emptyLevel_heatingPerCube_returnsZero() {
            Level empty = new Level(99, "Empty");
            assertEquals(0.0, empty.getHeatingPerCube(), 1e-9);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Building tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Building")
    class BuildingTests {

        private Building building;
        private Level level1;
        private Level level2;

        @BeforeEach
        void buildBuilding() {
            // Level 1: roomA (area=20, cube=50, heating=500, light=200)
            level1 = new Level(10, "Floor 1");
            level1.addRoom(roomA);

            // Level 2: roomB (area=30, cube=90, heating=900, light=300)
            level2 = new Level(20, "Floor 2");
            level2.addRoom(roomB);

            building = new Building(1, "Main Building");
            building.addLevel(level1);
            building.addLevel(level2);
        }

        @Test
        @DisplayName("getArea sums areas across all levels")
        void getArea_sumAcrossLevels() {
            assertEquals(50.0, building.getArea(), 1e-9); // 20 + 30
        }

        @Test
        @DisplayName("getCube sums cubes across all levels")
        void getCube_sumAcrossLevels() {
            assertEquals(140.0, building.getCube(), 1e-9); // 50 + 90
        }

        @Test
        @DisplayName("getLightPerArea = total light / total area for whole building")
        void getLightPerArea_wholeBuilding() {
            // (200+300) / (20+30) = 500/50 = 10.0
            assertEquals(10.0, building.getLightPerArea(), 1e-9);
        }

        @Test
        @DisplayName("getHeatingPerCube = total heating / total cube for whole building")
        void getHeatingPerCube_wholeBuilding() {
            // (500+900) / (50+90) = 1400/140 = 10.0
            assertEquals(10.0, building.getHeatingPerCube(), 1e-9);
        }

        @Test
        @DisplayName("Empty building: getLightPerArea returns 0 (no division by zero)")
        void emptyBuilding_lightPerArea_returnsZero() {
            Building empty = new Building(99, "Empty");
            assertEquals(0.0, empty.getLightPerArea(), 1e-9);
        }

        @Test
        @DisplayName("Empty building: getHeatingPerCube returns 0 (no division by zero)")
        void emptyBuilding_heatingPerCube_returnsZero() {
            Building empty = new Building(99, "Empty");
            assertEquals(0.0, empty.getHeatingPerCube(), 1e-9);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Visitor dispatch tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Visitor accept / double dispatch")
    class VisitorDispatchTests {

        /** Simple counting visitor that records how many times each visit overload is called. */
        private static class CountingVisitor implements LocationVisitor {
            int rooms = 0, levels = 0, buildings = 0;

            @Override public void visit(Room     r) { rooms++;     }
            @Override public void visit(Level    l) { levels++;    }
            @Override public void visit(Building b) { buildings++; }
        }

        @Test
        @DisplayName("Room.accept calls visitor.visit(Room) exactly once")
        void roomAccept_callsVisitRoom() {
            CountingVisitor v = new CountingVisitor();
            roomA.accept(v);
            assertEquals(1, v.rooms);
            assertEquals(0, v.levels);
            assertEquals(0, v.buildings);
        }

        @Test
        @DisplayName("Level.accept visits all rooms then the level itself (post-order)")
        void levelAccept_visitsRoomsThenLevel() {
            Level level = new Level(10, "F");
            level.addRoom(roomA);
            level.addRoom(roomB);

            CountingVisitor v = new CountingVisitor();
            level.accept(v);

            assertEquals(2, v.rooms);
            assertEquals(1, v.levels);
            assertEquals(0, v.buildings);
        }

        @Test
        @DisplayName("Building.accept visits all rooms, all levels, then the building (post-order)")
        void buildingAccept_visitsEverythingInOrder() {
            Level l1 = new Level(1, "F1");
            l1.addRoom(roomA);
            Level l2 = new Level(2, "F2");
            l2.addRoom(roomB);

            Building b = new Building(1, "B");
            b.addLevel(l1);
            b.addLevel(l2);

            CountingVisitor v = new CountingVisitor();
            b.accept(v);

            assertEquals(2, v.rooms);
            assertEquals(2, v.levels);
            assertEquals(1, v.buildings);
        }
    }
}