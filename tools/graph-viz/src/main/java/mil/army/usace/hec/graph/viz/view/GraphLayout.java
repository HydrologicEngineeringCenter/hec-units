package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.Node;
import mil.army.usace.hec.graph.viz.model.Pair;

// Where the units sit
public final class GraphLayout {

    public record Placed(Map<String, double[]> positions, Map<String, double[]> normalized,
                         double width, double height, boolean tree) {
    }

    private GraphLayout() {
    }

    public static Placed of(List<Node> nodes, List<Edge> edges) {
        Map<String, double[]> tree = treeLayout(nodes, edges);
        var positions = new LinkedHashMap<String, double[]>();
        var normalized = new LinkedHashMap<String, double[]>();
        double width;
        double height;

        if (tree != null) {
            long cols = tree.values().stream().mapToLong(p -> Math.round(p[0] * 1e4)).distinct().count();
            long rows = tree.values().stream().mapToLong(p -> Math.round(p[1] * 1e4)).distinct().count();
            width = Math.max(340, 126 * Math.max(cols, 1));
            height = Math.max(160, 106 * Math.max(rows, 1));
            double padX = 52;
            double padY = 34;
            for (Node node : nodes) {
                double[] p = tree.get(node.id());
                positions.put(node.id(), new double[]{padX + p[0] * (width - 2 * padX),
                                                      padY + p[1] * (height - 2 * padY)});
                normalized.put(node.id(), p);
            }
        } else {
            double radius = 34 + 26 * nodes.size();
            double pad = 90;
            width = height = 2 * (radius + pad);
            int i = 0;
            for (Node node : nodes) {
                double angle = 2 * Math.PI * i++ / nodes.size() - Math.PI / 2;
                positions.put(node.id(), new double[]{width / 2 + radius * Math.cos(angle),
                                                      height / 2 + radius * Math.sin(angle)});
            }
        }
        return new Placed(positions, normalized, width, height, tree != null);
    }

    private static Map<String, double[]> treeLayout(List<Node> nodes, List<Edge> edges) {
        var adjacency = new TreeMap<String, Set<String>>();
        nodes.forEach(node -> adjacency.put(node.id(), new HashSet<>()));
        var pairs = new HashSet<Pair>();
        for (Edge edge : edges) {
            adjacency.get(edge.from()).add(edge.to());
            adjacency.get(edge.to()).add(edge.from());
            pairs.add(Pair.unordered(edge.from(), edge.to()));
        }
        if (pairs.size() != nodes.size() - 1) {
            return null;
        }

        String root = nodes.stream().map(Node::id)
            .min(Comparator.comparingInt((String n) -> eccentricity(n, adjacency))
                           .thenComparingInt(n -> -adjacency.get(n).size())
                           .thenComparing(n -> n))
            .orElseThrow();

        var depth = new HashMap<String, Integer>();
        var children = new HashMap<String, List<String>>();
        var seen = new HashSet<String>();
        var queue = new ArrayDeque<String>();
        depth.put(root, 0);
        seen.add(root);
        queue.add(root);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String next : new TreeSet<>(adjacency.get(current))) {
                if (seen.add(next)) {
                    depth.put(next, depth.get(current) + 1);
                    children.computeIfAbsent(current, key -> new ArrayList<>()).add(next);
                    queue.add(next);
                }
            }
        }

        var xs = new HashMap<String, Double>();
        placeSubtree(root, children, xs, new int[]{0});

        double span = xs.values().stream().mapToDouble(x -> x).max().orElse(0);
        double deepest = Math.max(depth.values().stream().mapToInt(d -> d).max().orElse(1), 1);
        var out = new LinkedHashMap<String, double[]>();
        for (Node node : nodes) {
            out.put(node.id(), new double[]{
                span == 0 ? 0.5 : xs.get(node.id()) / span,
                depth.get(node.id()) / deepest});
        }
        return out;
    }

    private static void placeSubtree(String node, Map<String, List<String>> children,
                                     Map<String, Double> xs, int[] slot) {
        List<String> kids = children.getOrDefault(node, List.of());
        if (kids.isEmpty()) {
            xs.put(node, (double) slot[0]++);
            return;
        }
        for (String kid : kids) {
            placeSubtree(kid, children, xs, slot);
        }
        xs.put(node, (xs.get(kids.get(0)) + xs.get(kids.get(kids.size() - 1))) / 2);
    }

    private static int eccentricity(String start, Map<String, Set<String>> adjacency) {
        var seen = new HashSet<String>(List.of(start));
        var frontier = List.of(start);
        int distance = 0;
        while (!frontier.isEmpty()) {
            var next = new ArrayList<String>();
            for (String node : frontier) {
                for (String neighbor : adjacency.get(node)) {
                    if (seen.add(neighbor)) {
                        next.add(neighbor);
                    }
                }
            }
            if (!next.isEmpty()) {
                distance++;
            }
            frontier = next;
        }
        return distance;
    }
}
