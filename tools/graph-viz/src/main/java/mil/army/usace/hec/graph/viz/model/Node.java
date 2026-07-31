package mil.army.usace.hec.graph.viz.model;


/**
 * A point in a graph. `group` is the caller's category for grouping into
 * cards; `tone` is an optional second category used only for colouring.
 * Neither is interpreted here.
 */
public record Node(String id, String label, String group, String tone) {

    /** A node with no colouring category. */
    public Node(String id, String label, String group) {
        this(id, label, group, null);
    }
}
