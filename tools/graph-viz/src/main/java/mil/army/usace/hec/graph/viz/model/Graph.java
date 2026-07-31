package mil.army.usace.hec.graph.viz.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public final class Graph {

    // Pairs and byPair is mainly used as a way to find whether there is an edge from X to Y in constant time
    private record Pair(String from, String to) {
    }

    private final List<Node> nodes;
    private final List<Edge> edges;
    private final Map<Pair, Edge> byPair;

    public Graph(List<Node> nodes, List<Edge> edges) {
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);

        var nodeIds = new HashSet<String>();
        for (Node n : this.nodes) {
            nodeIds.add(n.id());
        }

        this.byPair = new HashMap<>();
        for (Edge e : this.edges) {
            if (!nodeIds.contains(e.from()) || !nodeIds.contains(e.to())) {
                throw new IllegalArgumentException(
                    "Edge " + e.from() + " -> " + e.to() + " references a node that isn't in this graph");
            }
            byPair.put(new Pair(e.from(), e.to()), e);
        }
    }

    public List<Node> nodes() {
        return nodes;
    }

    public List<Edge> edges() {
        return edges;
    }

    // Caller should null check
    public Edge edge(String from, String to) {
        return byPair.get(new Pair(from, to));
    }
}