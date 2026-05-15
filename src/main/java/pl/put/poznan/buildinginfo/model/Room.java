package pl.put.poznan.buildinginfo.model;

/**
 * Represents a single room within a {@link Level}.
 *
 * <p>A room is the leaf node of the building hierarchy and holds
 * the raw measurements: area, cube, heating energy consumption, and lighting power.</p>
 */
public class Room implements Location {

    /** Unique room identifier. */
    private int id;

    /** Optional room name (e.g. "Conference Room A"). May be {@code null}. */
    private String name;

    /** Floor area of the room in m². */
    private double area;

    /** Volume (cubature) of the room in m³. */
    private double cube;

    /** Heating energy consumption level of the room (float). */
    private double heating;

    /** Total installed lighting power of the room in watts. */
    private double light;

    // ── Constructors ──────────────────────────────────────────

    /** Creates a new room with default values. */
    public Room() {}

    /**
     * Creates a new room with the given measurements.
     *
     * @param id      unique room identifier
     * @param name    optional room name (may be null)
     * @param area    floor area in m²
     * @param cube    volume in m³
     * @param heating heating energy consumption
     * @param light   total installed lighting power in watts
     */
    public Room(int id, String name, double area, double cube, double heating, double light) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.cube = cube;
        this.heating = heating;
        this.light = light;
    }

    // ── Location interface ────────────────────────────────────

    @Override
    public int getId() { return id; }

    @Override
    public String getName() { return name; }

    /** Returns the room's own floor area in m². */
    @Override
    public double getArea() { return area; }

    /** Returns the room's own volume in m³. */
    @Override
    public double getCube() { return cube; }

    /** Returns the room's total lighting power in watts. */
    @Override
    public double getLight() { return light; }

    /** Returns the room's heating energy consumption. */
    @Override
    public double getHeating() { return heating; }

    /**
     * Returns the lighting power per unit area for this room.
     *
     * @return {@code light / area}, or {@code 0} if area is zero
     */
    @Override
    public double getLightPerArea() {
        return area == 0 ? 0 : light / area;
    }

    /**
     * Returns the heating energy consumption per unit volume for this room.
     *
     * @return {@code heating / cube}, or {@code 0} if cube is zero
     */
    @Override
    public double getHeatingPerCube() {
        return cube == 0 ? 0 : heating / cube;
    }

    // ── Setters (for JSON deserialization) ────────────────────

    /** Sets the room id. */
    public void setId(int id) { this.id = id; }
    /** Sets the room name. */
    public void setName(String name) { this.name = name; }
    /** Sets the floor area in m². */
    public void setArea(double area) { this.area = area; }
    /** Sets the volume in m³. */
    public void setCube(double cube) { this.cube = cube; }
    /** Sets the heating energy consumption. */
    public void setHeating(double heating) { this.heating = heating; }
    /** Sets the total installed lighting power in watts. */
    public void setLight(double light) { this.light = light; }

    // ── Visitor ───────────────────────────────────────────────

    /**
     * Accepts a visitor. As a leaf node, Room simply calls
     * {@code visitor.visit(this)} — there are no children to traverse first.
     *
     * @param visitor the visitor to accept
     */
    @Override
    public void accept(LocationVisitor visitor) {
        visitor.visit(this);
    }
}
