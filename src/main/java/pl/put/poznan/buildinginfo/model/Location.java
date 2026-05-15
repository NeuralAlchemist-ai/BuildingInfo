package pl.put.poznan.buildinginfo.model;

/**
 * Represents any location in the building hierarchy:
 * a {@link Building}, a {@link Level}, or a {@link Room}.
 *
 * <p>Every location has a unique identifier and an optional human-readable name.</p>
 */
public interface Location {

    /**
     * Returns the unique identifier of this location.
     *
     * @return location id
     */
    int getId();

    /**
     * Returns the optional name of this location, or {@code null} if not set.
     *
     * @return location name, may be {@code null}
     */
    String getName();

    /**
     * Returns the total floor area of this location in m².
     *
     * @return area in m²
     */
    double getArea();

    /**
     * Returns the total volume (cubature) of this location in m³.
     *
     * @return volume in m³
     */
    double getCube();

    /**
     * Returns the total lighting power of this location in watts.
     *
     * @return lighting power in W
     */
    double getLight();

    /**
     * Returns the total heating energy consumption of this location.
     *
     * @return heating energy consumption
     */
    double getHeating();

    /**
     * Returns the average lighting power per m² of floor area.
     *
     * @return light per m²
     */
    double getLightPerArea();

    /**
     * Returns the average heating energy consumption per m³ of volume.
     *
     * @return heating per m³
     */
    double getHeatingPerCube();

    /**
     * Accepts a {@link LocationVisitor}, dispatching to the correct
     * {@code visit} overload for this node type (double dispatch).
     *
     * <p>Composite nodes ({@link Building}, {@link Level}) must first
     * propagate the call to all their children before calling
     * {@code visitor.visit(this)}, ensuring post-order traversal.</p>
     *
     * @param visitor the visitor to accept
     */
    void accept(LocationVisitor visitor);
}
