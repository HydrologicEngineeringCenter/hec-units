package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;
import mil.army.usace.hec.graph.viz.model.Pair;


// The graph as cytoscape elements, one entry per group
// Basically translates the java graphs to cytoscape jsons
public final class CytoscapeData {

    private CytoscapeData() {
    }

    public static String script(Graph graph) {
        var nodesByGroup = new TreeMap<String, List<Node>>();
        for (Node node : graph.nodes()) {
            nodesByGroup.computeIfAbsent(node.group(), key -> new ArrayList<>()).add(node);
        }

        var groupOf = new HashMap<String, String>();
        graph.nodes().forEach(node -> groupOf.put(node.id(), node.group()));

        var edgesByGroup = new HashMap<String, List<Edge>>();
        for (Edge edge : graph.edges()) {
            edgesByGroup.computeIfAbsent(groupOf.get(edge.from()), key -> new ArrayList<>())
                        .add(edge);
        }

        var entries = new ArrayList<String>();
        nodesByGroup.forEach((group, nodes) -> {
            List<Edge> edges = edgesByGroup.getOrDefault(group, List.of());
            if (!edges.isEmpty()) {
                entries.add(Json.str(group) + ":" + group(nodes, edges));
            }
        });
        return "var GRAPHS={" + String.join(",", entries) + "};\n";
    }

    private static String group(List<Node> nodes, List<Edge> edges) {
        nodes.sort(java.util.Comparator.comparing(Node::id));

        var here = new java.util.HashSet<String>();
        nodes.forEach(node -> here.add(node.id()));

        var kept = new ArrayList<Edge>();
        for (Edge edge : edges) {
            if (here.contains(edge.from()) && here.contains(edge.to())) {
                kept.add(edge);
            } else {
                System.err.println("warning: skipping " + edge.from() + " -> " + edge.to()
                    + ", which leaves its dimension and cannot be drawn with it.");
            }
        }

        GraphLayout.Placed placed = GraphLayout.of(nodes, kept);
        var elements = new ArrayList<String>();
        for (Node node : nodes) {
            elements.add(node(node, placed));
        }

        var count = new HashMap<Pair, Integer>();
        var seen = new HashMap<Pair, Integer>();
        for (Edge edge : kept) {
            count.merge(Pair.unordered(edge.from(), edge.to()), 1, Integer::sum);
        }
        int index = 0;
        for (Edge edge : kept) {
            Pair key = Pair.unordered(edge.from(), edge.to());
            int n = count.get(key);
            int i = seen.merge(key, 1, Integer::sum) - 1;
            double bow = n == 1 ? 0.0 : (i - (n - 1) / 2.0) * 2.0;
            if (!edge.from().equals(key.from())) {
                bow = -bow;
            }
            elements.add(edge(edge, "e" + index++, bow));
        }
        return "{\"tree\":" + placed.tree()
             + ",\"width\":" + round(placed.width())
             + ",\"height\":" + round(placed.height())
             + ",\"elements\":[" + String.join(",", elements) + "]}";
    }

    private static String node(Node node, GraphLayout.Placed placed) {
        double[] at = placed.positions().get(node.id());
        double[] fraction = placed.normalised().get(node.id());
        return "{\"data\":{\"id\":" + Json.str(node.id())
             + ",\"label\":" + Json.str(node.id())
             + ",\"name\":" + Json.str(node.label())
             + (fraction == null ? ""
                : ",\"nx\":" + round(fraction[0]) + ",\"ny\":" + round(fraction[1]))
             + "}"
             + ",\"position\":{\"x\":" + round(at[0]) + ",\"y\":" + round(at[1]) + "}"
             + ",\"classes\":" + Json.str("t-" + tone(node)) + "}";
    }

    private static String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String edge(Edge edge, String id, double bow) {
        String[] parts = (edge.label() == null ? "" : edge.label()).split("\\|", -1);
        String kind = parts.length > 0 ? parts[0] : "";
        return "{\"data\":{\"id\":" + Json.str(id)
             + ",\"source\":" + Json.str(edge.from())
             + ",\"target\":" + Json.str(edge.to())
             + ",\"m\":" + affine(parts, 1)
             + ",\"b\":" + affine(parts, 2)
             + ",\"bow\":" + Json.num(bow)
             + ",\"detail\":" + Json.str(edge.detail()) + "}"
             + ",\"classes\":" + Json.str(kind) + "}";
    }

    private static String affine(String[] parts, int at) {
        if (parts.length <= at || parts[at].isEmpty()) {
            return "null";
        }
        try {
            return Json.num(Double.parseDouble(parts[at]));
        } catch (NumberFormatException e) {
            return "null";
        }
    }

    private static String tone(Node node) {
        if (node.tone() == null) {
            return "";
        }
        return node.tone().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static Map<String, Integer> shape(List<Node> nodes, List<Edge> edges) {
        var pairs = new java.util.HashSet<Pair>();
        for (Edge edge : edges) {
            pairs.add(Pair.unordered(edge.from(), edge.to()));
        }
        return Map.of("distinct", pairs.size(),
                      "parallel", edges.size() - pairs.size(),
                      "cycles", pairs.size() - nodes.size() + 1);
    }
}
