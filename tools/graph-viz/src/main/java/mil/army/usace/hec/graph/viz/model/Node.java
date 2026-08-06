package mil.army.usace.hec.graph.viz.model;


/**
 * A point in a graph.
 */
public record Node(String id, String label, String group, String tone) {

    /** A node with no coloring category. */
    public Node(String id, String label, String group) {
        this(id, label, group, null);
    }
}
