package mil.army.usace.hec.graph.viz.model;


/**
 * A directed connection between two nodes.
 *
 * `label` is optional short text drawn inside the cell itself - a couple of
 * characters at most. `detail` is an optional pre-rendered HTML fragment shown
 * when the edge is selected. Both are opaque here: this module builds neither,
 * it only carries whatever the adapter supplied. That mirrors the Python edge
 * dict, which had the same `html` field.
 */
public record Edge(String from, String to, EdgeStatus status, String label, String detail) {

    /** An edge with no label or detail attached. */
    public Edge(String from, String to, EdgeStatus status) {
        this(from, to, status, null, null);
    }
}
