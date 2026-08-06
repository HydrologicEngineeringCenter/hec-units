package mil.army.usace.hec.graph.viz.model;


/**
 * A directed connection between two nodes.
 */
public record Edge(String from, String to, EdgeStatus status, String label, String detail) {

    public Edge(String from, String to, EdgeStatus status) {
        this(from, to, status, null, null);
    }
}
