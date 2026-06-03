package pl.put.poznan.buildinginfo.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.put.poznan.buildinginfo.logic.visitor.HeatingThresholdVisitor;
import pl.put.poznan.buildinginfo.model.Building;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Room;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BuildingInfoTransformer} and {@link HeatingThresholdVisitor}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Delegation to model methods (area, cube, lightPerArea, heatingPerCube)</li>
 *   <li>findLevel / findRoom lookup helpers</li>
 *   <li>getRoomsExceedingHeatingThreshold – happy path, boundary, empty result</li>
 *   <li>HeatingThresholdVisitor in isolation</li>
 * </ul>
 */
class BuildingInfoTransformerTest {

    // ── shared fixtures ──────────────────────────────────────────────────────

    private BuildingInfoTransformer transformer;

    /** coolRoom: heatingPerCube = 100/50 = 2.0  (below most thresholds) */
    private Room coolRoom;

    /** hotRoom:  heatingPerCube = 600/60 = 10.0 (above most thresholds) */
    private Room hotRoom;

    /** borderRoom: heatingPerCube = 500/100 = 5.0 (exactly on boundary) */
    private Room borderRoom;

    private Level levelA;   // contains coolRoom
    private Level levelB;   // contains hotRoom + borderRoom
    private Building building;

    @BeforeEach
    void setUp() {
        transformer  = new BuildingInfoTransformer();

        coolRoom   = new Room(1, "Cool Room",   20.0,  50.0,  100.0, 200.0);
        hotRoom    = new Room(2, "Hot Room",    30.0,  60.0,  600.0, 300.0);
        borderRoom = new Room(3, "Border Room", 40.0, 100.0,  500.0, 400.0);

        levelA = new Level(10, "Level A");
        levelA.addRoom(coolRoom);

        levelB = new Level(20, "Level B");
        levelB.addRoom(hotRoom);
        levelB.addRoom(borderRoom);

        building = new Building(1, "Test Building");
        building.addLevel(levelA);
        building.addLevel(levelB);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Basic delegation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Delegation to Location")
    class DelegationTests {

        @Test
        @DisplayName("getArea delegates to building.getArea()")
        void getArea_delegatesToBuilding() {
            // 20 + 30 + 40 = 90
            assertEquals(90.0, transformer.getArea(building), 1e-9);
        }

        @Test
        @DisplayName("getCube delegates to building.getCube()")
        void getCube_delegatesToBuilding() {
            // 50 + 60 + 100 = 210
            assertEquals(210.0, transformer.getCube(building), 1e-9);
        }

        @Test
        @DisplayName("getLightPerArea delegates to building.getLightPerArea()")
        void getLightPerArea_delegatesToBuilding() {
            // (200+300+400) / (20+30+40) = 900 / 90 = 10.0
            assertEquals(10.0, transformer.getLightPerArea(building), 1e-9);
        }

        @Test
        @DisplayName("getHeatingPerCube delegates to building.getHeatingPerCube()")
        void getHeatingPerCube_delegatesToBuilding() {
            // (100+600+500) / (50+60+100) = 1200 / 210 ≈ 5.714
            double expected = 1200.0 / 210.0;
            assertEquals(expected, transformer.getHeatingPerCube(building), 1e-9);
        }

        @Test
        @DisplayName("getArea works on a single Room (leaf node)")
        void getArea_onRoom() {
            assertEquals(20.0, transformer.getArea(coolRoom), 1e-9);
        }

        @Test
        @DisplayName("getLightPerArea works on a single Level")
        void getLightPerArea_onLevel() {
            // levelA has only coolRoom: 200 / 20 = 10.0
            assertEquals(10.0, transformer.getLightPerArea(levelA), 1e-9);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findLevel
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findLevel")
    class FindLevelTests {

        @Test
        @DisplayName("Returns the correct level when it exists")
        void findLevel_found() {
            Optional<Level> result = transformer.findLevel(building, 10);
            assertTrue(result.isPresent());
            assertEquals("Level A", result.get().getName());
        }

        @Test
        @DisplayName("Returns empty when level id is not in the building")
        void findLevel_notFound_returnsEmpty() {
            Optional<Level> result = transformer.findLevel(building, 999);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Finds the second level correctly")
        void findLevel_secondLevel() {
            Optional<Level> result = transformer.findLevel(building, 20);
            assertTrue(result.isPresent());
            assertEquals(20, result.get().getId());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findRoom
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findRoom")
    class FindRoomTests {

        @Test
        @DisplayName("Returns the correct room when it exists in the first level")
        void findRoom_found_inFirstLevel() {
            Optional<Room> result = transformer.findRoom(building, 1);
            assertTrue(result.isPresent());
            assertEquals("Cool Room", result.get().getName());
        }

        @Test
        @DisplayName("Returns the correct room when it exists in a deeper level")
        void findRoom_found_inSecondLevel() {
            Optional<Room> result = transformer.findRoom(building, 2);
            assertTrue(result.isPresent());
            assertEquals("Hot Room", result.get().getName());
        }

        @Test
        @DisplayName("Returns empty when room id is not anywhere in the building")
        void findRoom_notFound_returnsEmpty() {
            Optional<Room> result = transformer.findRoom(building, 999);
            assertFalse(result.isPresent());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getRoomsExceedingHeatingThreshold
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRoomsExceedingHeatingThreshold")
    class HeatingThresholdTests {

        @Test
        @DisplayName("Returns only the room that strictly exceeds the threshold")
        void threshold_strictlyAbove_onlyHotRoom() {
            // coolRoom = 2.0, borderRoom = 5.0, hotRoom = 10.0  → threshold 5.0 keeps only hotRoom
            List<Room> result = transformer.getRoomsExceedingHeatingThreshold(building, 5.0);
            assertEquals(1, result.size());
            assertEquals(2, result.get(0).getId());
        }

        @Test
        @DisplayName("Boundary room (heatingPerCube == threshold) is NOT included (strict >)")
        void threshold_boundary_roomExcluded() {
            List<Room> result = transformer.getRoomsExceedingHeatingThreshold(building, 5.0);
            boolean containsBorder = result.stream().anyMatch(r -> r.getId() == 3);
            assertFalse(containsBorder, "Border room with heatingPerCube == threshold must not be included");
        }

        @Test
        @DisplayName("All rooms included when threshold is very low")
        void threshold_veryLow_allRoomsIncluded() {
            List<Room> result = transformer.getRoomsExceedingHeatingThreshold(building, 0.0);
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("No rooms included when threshold is very high")
        void threshold_veryHigh_noRoomsIncluded() {
            List<Room> result = transformer.getRoomsExceedingHeatingThreshold(building, 100.0);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Empty building returns an empty list")
        void threshold_emptyBuilding_emptyList() {
            Building empty = new Building(99, "Empty");
            List<Room> result = transformer.getRoomsExceedingHeatingThreshold(empty, 5.0);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Result list is unmodifiable")
        void threshold_resultIsUnmodifiable() {
            List<Room> result = transformer.getRoomsExceedingHeatingThreshold(building, 5.0);
            assertThrows(UnsupportedOperationException.class, () -> result.add(coolRoom));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HeatingThresholdVisitor in isolation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HeatingThresholdVisitor (direct)")
    class VisitorDirectTests {

        @Test
        @DisplayName("visit(Room) adds room when heatingPerCube > threshold")
        void visitRoom_above_addsToResult() {
            HeatingThresholdVisitor v = new HeatingThresholdVisitor(5.0);
            v.visit(hotRoom);  // 10.0 > 5.0
            assertEquals(1, v.getResult().size());
        }

        @Test
        @DisplayName("visit(Room) skips room when heatingPerCube == threshold")
        void visitRoom_equal_notAdded() {
            HeatingThresholdVisitor v = new HeatingThresholdVisitor(5.0);
            v.visit(borderRoom);  // 5.0 == 5.0 → not strictly greater
            assertTrue(v.getResult().isEmpty());
        }

        @Test
        @DisplayName("visit(Room) skips room when heatingPerCube < threshold")
        void visitRoom_below_notAdded() {
            HeatingThresholdVisitor v = new HeatingThresholdVisitor(5.0);
            v.visit(coolRoom);  // 2.0 < 5.0
            assertTrue(v.getResult().isEmpty());
        }

        @Test
        @DisplayName("visit(Level) is a no-op (does not add anything)")
        void visitLevel_noOp() {
            HeatingThresholdVisitor v = new HeatingThresholdVisitor(0.0);
            v.visit(levelB);  // directly calling visit(Level), no children traversed
            assertTrue(v.getResult().isEmpty());
        }

        @Test
        @DisplayName("visit(Building) is a no-op (does not add anything)")
        void visitBuilding_noOp() {
            HeatingThresholdVisitor v = new HeatingThresholdVisitor(0.0);
            v.visit(building); // directly calling visit(Building), no children traversed
            assertTrue(v.getResult().isEmpty());
        }
    }
}