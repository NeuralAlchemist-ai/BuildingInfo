package pl.put.poznan.buildinginfo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entire building — the root of the location hierarchy.
 *
 * <p>A building aggregates the measurements of all its {@link Level levels}:
 * area and cube are summed across all levels; light-per-area and
 * heating-per-cube are averaged across the whole building.</p>
 */
public class Building implements Location {

    /** Unique building identifier. */
    private int id;

    /** Optional building name (e.g. "Main Campus Block A"). May be {@code null}. */
    private String name;

    /** Floors (levels) that belong to this building. */
    private List<Level> levels = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────

    public Building() {}

    public Building(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // ── Location interface ────────────────────────────────────

    @Override
    public int getId() { return id; }

    @Override
    public String getName() { return name; }

    /**
     * Returns the total floor area of the building in m²,
     * calculated as the sum of areas of all levels.
     */
    @Override
    public double getArea() {
        return levels.stream().mapToDouble(Level::getArea).sum();
    }

    /**
     * Returns the total volume of the building in m³,
     * calculated as the sum of volumes of all levels.
     */
    @Override
    public double getCube() {
        return levels.stream().mapToDouble(Level::getCube).sum();
    }

    /**
     * Returns the total lighting power of the building in watts,
     * calculated as the sum across all levels.
     */
    @Override
    public double getLight() {
        return levels.stream().mapToDouble(Level::getLight).sum();
    }

    /**
     * Returns the total heating energy consumption of the building,
     * calculated as the sum across all levels.
     */
    @Override
    public double getHeating() {
        return levels.stream().mapToDouble(Level::getHeating).sum();
    }

    /**
     * Returns the average lighting power per m² for the whole building.
     *
     * @return average light per m²; {@code 0} if total area is zero
     */
    @Override
    public double getLightPerArea() {
        double area = getArea();
        return area == 0 ? 0 : getLight() / area;
    }

    /**
     * Returns the average heating energy consumption per m³ for the whole building.
     *
     * @return average heating per m³; {@code 0} if total cube is zero
     */
    @Override
    public double getHeatingPerCube() {
        double cube = getCube();
        return cube == 0 ? 0 : getHeating() / cube;
    }

    // ── Accessors ─────────────────────────────────────────────

    public List<Level> getLevels() { return levels; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLevels(List<Level> levels) { this.levels = levels; }

    public void addLevel(Level level) { this.levels.add(level); }
}
