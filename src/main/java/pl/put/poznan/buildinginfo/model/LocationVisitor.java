package pl.put.poznan.buildinginfo.model;

/**
 * Visitor interface for the {@link Location} composite hierarchy.
 *
 * <p>Implement this interface to add new reporting or analysis operations
 * over the building structure without modifying the model classes themselves.</p>
 *
 * <p>Traversal is driven by each node's {@link Location#accept(LocationVisitor)}
 * method. Composite nodes ({@link Building}, {@link Level}) walk their children
 * first, then call {@code visit(this)} on the visitor, giving post-order
 * (bottom-up) traversal by default.</p>
 *
 * <p>Example — collecting all rooms above a heating threshold:</p>
 * <pre>
 *     HeatingThresholdVisitor v = new HeatingThresholdVisitor(200.0);
 *     building.accept(v);
 *     List&lt;Room&gt; hotRooms = v.getResult();
 * </pre>
 */
public interface LocationVisitor {

    /**
     * Called when visiting a {@link Building} node (after all its levels
     * and their rooms have already been visited).
     *
     * @param building the building being visited
     */
    void visit(Building building);

    /**
     * Called when visiting a {@link Level} node (after all its rooms
     * have already been visited).
     *
     * @param level the level being visited
     */
    void visit(Level level);

    /**
     * Called when visiting a {@link Room} leaf node.
     *
     * @param room the room being visited
     */
    void visit(Room room);
}
