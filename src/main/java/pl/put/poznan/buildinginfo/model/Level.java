package pl.put.poznan.buildinginfo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single floor (level) within a {@link Building}.
 *
 * <p>A level aggregates the measurements of all its {@link Room rooms}:
 * area and cube are summed; light-per-area and heating-per-cube are averaged.</p>
 */
public class Level implements Location {

    /** Unique level identifier. */
    private int id;

    /** Optional level name (e.g. "Ground Floor"). May be {@code null}. */
    private String name;

    /** Rooms that belong to this level. */
    private List<Room> rooms = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────

    public Level() {}

    public Level(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // ── Location interface ────────────────────────────────────

    @Override
    public int getId() { return id; }

    @Override
    public String getName() { return name; }

    /**
     * Returns the total floor area of this level in m²,
     * calculated as the sum of areas of all rooms on the level.
     */
    @Override
    public double getArea() {
        return rooms.stream().mapToDouble(Room::getArea).sum();
    }

    /**
     * Returns the total volume of this level in m³,
     * calculated as the sum of volumes of all rooms on the level.
     */
    @Override
    public double getCube() {
        return rooms.stream().mapToDouble(Room::getCube).sum();
    }

    /**
     * Returns the total lighting power of this level in watts,
     * calculated as the sum of lighting power of all rooms on the level.
     */
    @Override
    public double getLight() {
        return rooms.stream().mapToDouble(Room::getLight).sum();
    }

    /**
     * Returns the total heating energy consumption of this level,
     * calculated as the sum of all rooms on the level.
     */
    @Override
    public double getHeating() {
        return rooms.stream().mapToDouble(Room::getHeating).sum();
    }

    /**
     * Returns the average lighting power per m² for this level.
     *
     * @return average light per m² across all rooms; {@code 0} if area is zero
     */
    @Override
    public double getLightPerArea() {
        double area = getArea();
        return area == 0 ? 0 : getLight() / area;
    }

    /**
     * Returns the average heating energy consumption per m³ for this level.
     *
     * @return average heating per m³ across all rooms; {@code 0} if cube is zero
     */
    @Override
    public double getHeatingPerCube() {
        double cube = getCube();
        return cube == 0 ? 0 : getHeating() / cube;
    }

    // ── Accessors ─────────────────────────────────────────────

    public List<Room> getRooms() { return rooms; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }

    public void addRoom(Room room) { this.rooms.add(room); }
}
