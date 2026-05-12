package pl.put.poznan.buildinginfo.logic.visitor;

import pl.put.poznan.buildinginfo.model.Building;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Location;
import pl.put.poznan.buildinginfo.model.LocationVisitor;
import pl.put.poznan.buildinginfo.model.Room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link LocationVisitor} that collects every {@link Room} whose
 * heating energy consumption per m³ exceeds a configurable threshold.
 *
 * <p>Usage:</p>
 * <pre>
 *     HeatingThresholdVisitor visitor = new HeatingThresholdVisitor(200.0);
 *     building.accept(visitor);
 *     List&lt;Room&gt; hotRooms = visitor.getResult();
 * </pre>
 *
 * <p>The visitor only acts on {@link Room} nodes; {@link Level} and
 * {@link Building} visits are intentional no-ops because all the
 * relevant data lives at the room level.</p>
 *
 * <p>Instances are <em>not</em> reusable: create a fresh visitor for
 * each traversal to avoid accumulating results across calls.</p>
 */
public class HeatingThresholdVisitor implements LocationVisitor {

    /** Heating-per-m³ value above which a room is considered to exceed the threshold. */
    private final double threshold;

    /** Rooms collected during traversal whose heating per m³ exceeds {@link #threshold}. */
    private final List<Room> result = new ArrayList<>();

    /**
     * Creates a new visitor with the given heating threshold.
     *
     * @param threshold maximum acceptable heating per m³;
     *                  rooms strictly above this value are collected
     */
    public HeatingThresholdVisitor(double threshold) {
        this.threshold = threshold;
    }

    // ── LocationVisitor ───────────────────────────────────────

    /**
     * Checks whether this room's heating per m³ exceeds the threshold
     * and, if so, adds it to the result list.
     *
     * @param room the room being visited
     */
    @Override
    public void visit(Room room) {
        if (room.getHeatingPerCube() > threshold) {
            result.add(room);
        }
    }

    /**
     * No-op: level-level data is derived from rooms, which are handled
     * individually in {@link #visit(Room)}.
     *
     * @param level the level being visited
     */
    @Override
    public void visit(Level level) {
        // nothing to do at level granularity
    }

    /**
     * No-op: building-level data is derived from rooms, which are handled
     * individually in {@link #visit(Room)}.
     *
     * @param building the building being visited
     */
    @Override
    public void visit(Building building) {
        // nothing to do at building granularity
    }

    // ── Result ────────────────────────────────────────────────

    /**
     * Returns an unmodifiable view of the rooms collected during traversal.
     *
     * @return rooms whose heating per m³ exceeded the threshold; never {@code null}
     */
    public List<Room> getResult() {
        return Collections.unmodifiableList(result);
    }
}
