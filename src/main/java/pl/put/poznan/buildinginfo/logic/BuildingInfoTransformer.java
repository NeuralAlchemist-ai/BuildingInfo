package pl.put.poznan.buildinginfo.logic;

import pl.put.poznan.buildinginfo.logic.visitor.HeatingThresholdVisitor;
import pl.put.poznan.buildinginfo.model.Building;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Location;
import pl.put.poznan.buildinginfo.model.Room;

import java.util.List;
import java.util.Optional;

/**
 * Core business logic service for the Building Info application.
 *
 * <p>Provides methods to query and compute statistics for any {@link Location}
 * (building, level, or room) within a {@link Building} structure.
 * This class is the primary entry point for all domain calculations.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     BuildingInfoTransformer service = new BuildingInfoTransformer();
 *     double area = service.getArea(building);
 *     List&lt;Room&gt; hotRooms = service.getRoomsExceedingHeatingThreshold(building, 200.0);
 * </pre>
 */
public class BuildingInfoTransformer {

    // ── Area ──────────────────────────────────────────────────

    /**
     * Returns the total floor area of the given location in m².
     *
     * @param location any location (building, level, or room)
     * @return total area in m²
     */
    public double getArea(Location location) {
        return location.getArea();
    }

    // ── Cubature ──────────────────────────────────────────────

    /**
     * Returns the total volume (cubature) of the given location in m³.
     *
     * @param location any location (building, level, or room)
     * @return total volume in m³
     */
    public double getCube(Location location) {
        return location.getCube();
    }

    // ── Lighting ──────────────────────────────────────────────

    /**
     * Returns the average lighting power per m² of floor area for the given location.
     *
     * @param location any location (building, level, or room)
     * @return average light per m²; {@code 0} if area is zero
     */
    public double getLightPerArea(Location location) {
        return location.getLightPerArea();
    }

    // ── Heating ───────────────────────────────────────────────

    /**
     * Returns the average heating energy consumption per m³ of volume for the given location.
     *
     * @param location any location (building, level, or room)
     * @return average heating per m³; {@code 0} if volume is zero
     */
    public double getHeatingPerCube(Location location) {
        return location.getHeatingPerCube();
    }

    // ── Threshold query ───────────────────────────────────────

    /**
     * Returns all rooms within a building whose heating energy consumption per m³
     * exceeds the given threshold.
     *
     * <p>Delegates to {@link HeatingThresholdVisitor}, which traverses the
     * Composite hierarchy via {@code accept} / {@code visit} double dispatch.</p>
     *
     * @param building  the building to search
     * @param threshold the maximum acceptable heating consumption per m³
     * @return list of rooms exceeding the threshold; never {@code null}
     */
    public List<Room> getRoomsExceedingHeatingThreshold(Building building, double threshold) {
        HeatingThresholdVisitor visitor = new HeatingThresholdVisitor(threshold);
        building.accept(visitor);
        return visitor.getResult();
    }

    // ── Location lookup helpers ───────────────────────────────

    /**
     * Finds a {@link Level} within a building by its id.
     *
     * @param building the building to search
     * @param id       the level id to look for
     * @return an {@link Optional} containing the level if found, or empty
     */
    public Optional<Level> findLevel(Building building, int id) {
        return building.getLevels().stream()
                .filter(l -> l.getId() == id)
                .findFirst();
    }

    /**
     * Finds a {@link Room} anywhere within a building by its id.
     *
     * @param building the building to search
     * @param id       the room id to look for
     * @return an {@link Optional} containing the room if found, or empty
     */
    public Optional<Room> findRoom(Building building, int id) {
        return building.getLevels().stream()
                .flatMap(l -> l.getRooms().stream())
                .filter(r -> r.getId() == id)
                .findFirst();
    }
}
