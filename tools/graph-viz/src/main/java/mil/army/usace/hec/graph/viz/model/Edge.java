package mil.army.usace.hec.graph.viz.model;


/**
 * A directed connection between two nodes.
 *
 * `label` is short text drawn inside the cell; `detail` is pre-rendered markup
 * shown when the edge is selected. Both are opaque here - the adapter builds
 * them, this module only carries them.
 */
public record Edge(String from, String to, EdgeStatus status, String label, String detail) {

    /** An edge with no label or detail attached. */
    public Edge(String from, String to, EdgeStatus status) {
        this(from, to, status, null, null);
    }
}
