package pl.put.poznan.buildinginfo.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.put.poznan.buildinginfo.logic.visitor.HeatingThresholdVisitor;
import pl.put.poznan.buildinginfo.model.Building;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Location;
import pl.put.poznan.buildinginfo.model.Room;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BuildingInfoTransformer Mock Interaction Tests (JDK 21)")
class BuildingInfoTransformerMockTest {

    private BuildingInfoTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new BuildingInfoTransformer();
    }

    @Test
    @DisplayName("Verify interaction with Location.getArea()")
    void testGetAreaInteractions() {
        Location location = mock(Location.class);
        when(location.getArea()).thenReturn(150.0);

        double area = transformer.getArea(location);

        assertEquals(150.0, area, 1e-9);
        // [Call 1] Verifying method call on external class: Location
        verify(location, times(1)).getArea();
    }

    @Test
    @DisplayName("Verify interaction with Location.getCube()")
    void testGetCubeInteractions() {
        Location location = mock(Location.class);
        when(location.getCube()).thenReturn(450.0);

        double cube = transformer.getCube(location);

        assertEquals(450.0, cube, 1e-9);
        // [Call 2] Verifying method call on external class: Location
        verify(location, times(1)).getCube();
    }

    @Test
    @DisplayName("Verify interaction with Location.getLightPerArea()")
    void testGetLightPerAreaInteractions() {
        Location location = mock(Location.class);
        when(location.getLightPerArea()).thenReturn(3.5);

        double light = transformer.getLightPerArea(location);

        assertEquals(3.5, light, 1e-9);
        // [Call 3] Verifying method call on external class: Location
        verify(location, times(1)).getLightPerArea();
    }

    @Test
    @DisplayName("Verify interaction with Location.getHeatingPerCube()")
    void testGetHeatingPerCubeInteractions() {
        Location location = mock(Location.class);
        when(location.getHeatingPerCube()).thenReturn(1.2);

        double heating = transformer.getHeatingPerCube(location);

        assertEquals(1.2, heating, 1e-9);
        // [Call 4] Verifying method call on external class: Location
        verify(location, times(1)).getHeatingPerCube();
    }

    @Test
    @DisplayName("Verify interaction with Building.accept() during visitor aggregation")
    void testGetRoomsExceedingHeatingThresholdInteractions() {
        Building building = mock(Building.class);

        List<Room> result = transformer.getRoomsExceedingHeatingThreshold(building, 25.0);

        assertNotNull(result);
        // [Call 5] Verifying method call on external class: Building
        verify(building, times(1)).accept(any(HeatingThresholdVisitor.class));
    }

    @Test
    @DisplayName("Verify multiple cascading mock interactions when searching for a Level")
    void testFindLevelInteractions() {
        Building building = mock(Building.class);
        Level level1 = mock(Level.class);
        Level level2 = mock(Level.class);

        // Uses modern JDK 21 List.of() initialization
        when(building.getLevels()).thenReturn(List.of(level1, level2));
        when(level1.getId()).thenReturn(10);
        when(level2.getId()).thenReturn(20);

        Optional<Level> foundLevel = transformer.findLevel(building, 20);

        assertTrue(foundLevel.isPresent());
        assertEquals(level2, foundLevel.get());

        // [Call 6] Verifying method call on external class: Building
        verify(building, times(1)).getLevels();

        // [Call 7] Verifying method call on external class: Level (First element checked by Stream filter)
        verify(level1, times(1)).getId();

        // [Call 8] Verifying method call on external class: Level (Second element checked by Stream filter)
        verify(level2, times(1)).getId();
    }

    @Test
    @DisplayName("Verify multiple cascading mock interactions when searching deeply for a Room")
    void testFindRoomInteractions() {
        Building building = mock(Building.class);
        Level level = mock(Level.class);
        Room room1 = mock(Room.class);
        Room room2 = mock(Room.class);

        // Uses modern JDK 21 List.of() initialization
        when(building.getLevels()).thenReturn(List.of(level));
        when(level.getRooms()).thenReturn(List.of(room1, room2));
        when(room1.getId()).thenReturn(101);
        when(room2.getId()).thenReturn(102);

        Optional<Room> foundRoom = transformer.findRoom(building, 102);

        assertTrue(foundRoom.isPresent());
        assertEquals(room2, foundRoom.get());

        // [Call 9] Verifying method call on external class: Building
        verify(building, times(1)).getLevels();

        // [Call 10] Verifying method call on external class: Level
        verify(level, times(1)).getRooms();

        // [Call 11] Verifying method call on external class: Room (First element checked by Stream filter)
        verify(room1, times(1)).getId();

        // [Call 12] Verifying method call on external class: Room (Second element checked by Stream filter)
        verify(room2, times(1)).getId();
    }
}