package mil.army.usace.hec.graph.viz.model;


/**
 * A directed connection between two nodes.
 *
 * `detail` is an optional pre-rendered HTML fragment describing what this edge
 * actually does - shown when the edge is selected. It is opaque here: this
 * module builds no such text itself, it only carries whatever the adapter
 * supplied. That mirrors the Python edge dict, which had the same `html` field.
 */
public record Edge(String from, String to, EdgeStatus status, String detail) {

    /** An edge with no detail text attached. */
    public Edge(String from, String to, EdgeStatus status) {
        this(from, to, status, null);
    }
}