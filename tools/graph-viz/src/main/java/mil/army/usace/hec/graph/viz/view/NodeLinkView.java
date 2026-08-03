package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;
import mil.army.usace.hec.graph.viz.model.Pair;

/**
 * Renders a graph as one node-link drawing per group
 */
public final class NodeLinkView {

    private static final String CARD = """
        <div class="card seedcard {{shape}}" style="--i:{{index}}">
          <header><h2>{{group}}</h2><span class="badge {{shape}}">{{badge}}</span></header>
          <p class="meta">{{meta}}</p>
          {{svg}}
        </div>
        """;

    private NodeLinkView() {
    }

    public static String render(Graph graph) {
        var groups = new TreeMap<String, List<Node>>();
        for (Node node : graph.nodes()) {
            groups.computeIfAbsent(node.group(), key -> new ArrayList<>()).add(node);
        }
        var edgesByGroup = new HashMap<String, List<Edge>>();
        var nodeGroup = new HashMap<String, String>();
        graph.nodes().forEach(node -> nodeGroup.put(node.id(), node.group()));
        for (Edge edge : graph.edges()) {
            edgesByGroup.computeIfAbsent(nodeGroup.get(edge.from()), key -> new ArrayList<>())
                        .add(edge);
        }

        // Alphabetical, matching the coverage tab - the two grids should read
        // as the same page in two projections.
        var cards = new StringBuilder("<div class=\"grid\">\n");
        int index = 0;
        for (String group : groups.keySet()) {
            List<Edge> edges = edgesByGroup.getOrDefault(group, List.of());
            if (edges.isEmpty()) {
                continue;
            }
            cards.append(card(group, groups.get(group), edges, index++));
        }
        return cards.append("</div>\n").toString();
    }

    private static String card(String group, List<Node> nodes, List<Edge> edges, int index) {
        nodes.sort(Comparator.comparing(Node::id));

        int distinct = distinctPairs(edges);
        int parallel = edges.size() - distinct;
        int cycles = distinct - nodes.size() + 1;

        String shape;
        String badge;
        if (cycles > 0) {
            shape = "cyclic";
            badge = cycles + (cycles > 1 ? " cycles" : " cycle");
        } else if (parallel > 0) {
            shape = "dup";
            badge = parallel + " duplicate edge" + (parallel > 1 ? "s" : "");
        } else {
            shape = "tree";
            badge = "tree";
        }

        return Html.fill(CARD)
            .put("group", group)
            .put("index", index)
            .put("shape", shape)
            .put("badge", badge)
            .put("meta", nodes.size() + " units")
            .raw("svg", svg(nodes, edges))
            .render();
    }

    private static int distinctPairs(List<Edge> edges) {
        var pairs = new HashSet<Pair>();
        for (Edge edge : edges) {
            pairs.add(pairKey(edge));
        }
        return pairs.size();
    }

    /* ------------------------------------------------------------- drawing */

    private static String svg(List<Node> nodes, List<Edge> edges) {
        Map<String, double[]> tree = treeLayout(nodes, edges);
        double width;
        double height;
        Map<String, double[]> pos = new LinkedHashMap<>();

        if (tree != null) {
            long cols = tree.values().stream().mapToLong(p -> Math.round(p[0] * 1e4)).distinct().count();
            long rows = tree.values().stream().mapToLong(p -> Math.round(p[1] * 1e4)).distinct().count();
            width = Math.max(340, 126 * Math.max(cols, 1));
            height = Math.max(160, 106 * Math.max(rows, 1));
            double padX = 52;
            double padY = 34;
            for (Node node : nodes) {
                double[] p = tree.get(node.id());
                pos.put(node.id(), new double[]{padX + p[0] * (width - 2 * padX),
                                                padY + p[1] * (height - 2 * padY)});
            }
        } else {
            double radius = 34 + 26 * nodes.size();
            double pad = 90;
            width = height = 2 * (radius + pad);
            int i = 0;
            for (Node node : nodes) {
                double angle = 2 * Math.PI * i++ / nodes.size() - Math.PI / 2;
                pos.put(node.id(), new double[]{width / 2 + radius * Math.cos(angle),
                                                height / 2 + radius * Math.sin(angle)});
            }
        }

        var out = new StringBuilder();
        out.append("<svg class=\"nl\" viewBox=\"0 0 ").append(fmt(width)).append(' ')
           .append(fmt(height)).append("\" data-tree=\"").append(tree != null ? 1 : 0)
           .append("\"><g class=\"nl-root\">");

        Map<Edge, Double> bows = bows(edges);
        for (Edge edge : edges) {
            appendEdge(out, edge, pos, bows.get(edge), tree);
        }
        for (Node node : nodes) {
            appendNode(out, node, pos.get(node.id()), tree);
        }
        return out.append("</g></svg>").toString();
    }

    /** Edges sharing an unordered pair get spread bow offsets: -1, 1; -2, 0, 2... */
    private static Map<Edge, Double> bows(List<Edge> edges) {
        var count = new HashMap<Pair, Integer>();
        var seen = new HashMap<Pair, Integer>();
        for (Edge edge : edges) {
            count.merge(pairKey(edge), 1, Integer::sum);
        }
        var bows = new HashMap<Edge, Double>();
        for (Edge edge : edges) {
            Pair key = pairKey(edge);
            int n = count.get(key);
            int i = seen.merge(key, 1, Integer::sum) - 1;
            bows.put(edge, n == 1 ? 0.0 : (i - (n - 1) / 2.0) * 2.0);
        }
        return bows;
    }

    private static Pair pairKey(Edge edge) {
        return Pair.unordered(edge.from(), edge.to());
    }

    private static void appendEdge(StringBuilder out, Edge edge, Map<String, double[]> pos,
                                   double bow, Map<String, double[]> tree) {
        double[] a = pos.get(edge.from());
        double[] b = pos.get(edge.to());

        Pair ends = pairKey(edge);
        double[] lo = pos.get(ends.from());
        double[] hi = pos.get(ends.to());

        String[] parts = (edge.label() == null ? "" : edge.label()).split("\\|", -1);
        String tag = parts.length > 0 ? parts[0] : "";

        var path = Html.tag("path")
            .attr("class", ("nl-edge " + tag).trim())
            .attr("d", arc(a[0], a[1], b[0], b[1], bow * (tree != null ? 16 : 20),
                           hi[0] - lo[0], hi[1] - lo[1]))
            .attr("data-a", edge.from())
            .attr("data-b", edge.to())
            .attr("data-bow", fmt(bow))
            .attr("data-m", parts.length > 1 ? parts[1] : null)
            .attr("data-k", parts.length > 2 ? parts[2] : null)
            .attr("data-detail", edge.detail());
        out.append(path);
    }

    /** Quadratic path bowed sideways; straight when the bow is zero. */
    private static String arc(double x1, double y1, double x2, double y2,
                              double bow, double refX, double refY) {
        if (bow == 0) {
            return "M " + fmt(x1) + ' ' + fmt(y1) + " L " + fmt(x2) + ' ' + fmt(y2);
        }
        double d = Math.hypot(refX, refY);
        if (d == 0) {
            d = 1;
        }
        double mx = (x1 + x2) / 2 + (-refY / d) * bow * 2;
        double my = (y1 + y2) / 2 + (refX / d) * bow * 2;
        return "M " + fmt(x1) + ' ' + fmt(y1) + " Q " + fmt(mx) + ' ' + fmt(my)
             + ' ' + fmt(x2) + ' ' + fmt(y2);
    }

    private static void appendNode(StringBuilder out, Node node, double[] p,
                                   Map<String, double[]> tree) {
        double boxWidth = 26 + 13.0 * node.id().length();
        String tone = node.tone() == null ? "" : node.tone().toLowerCase(Locale.ROOT)
                                                     .replaceAll("[^a-z0-9]", "");
        var group = Html.tag("g")
            .attr("class", ("nl-node t-" + tone).trim())
            .attr("transform", "translate(" + fmt(p[0]) + "," + fmt(p[1]) + ")")
            .attr("data-id", node.id())
            .attr("data-name", node.label())
            .attr("data-nx", tree != null ? fmt5(tree.get(node.id())[0]) : null)
            .attr("data-ny", tree != null ? fmt5(tree.get(node.id())[1]) : null)
            .html("<rect x=\"" + fmt(-boxWidth / 2) + "\" y=\"-23\" width=\"" + fmt(boxWidth)
                + "\" height=\"46\" rx=\"23\"></rect>"
                + Html.tag("text").attr("y", "6").text(node.id()));
        out.append(group);
    }

    private static Map<String, double[]> treeLayout(List<Node> nodes, List<Edge> edges) {
        var adjacency = new TreeMap<String, Set<String>>();
        nodes.forEach(node -> adjacency.put(node.id(), new HashSet<>()));
        for (Edge edge : edges) {
            adjacency.get(edge.from()).add(edge.to());
            adjacency.get(edge.to()).add(edge.from());
        }
        if (distinctPairs(edges) != nodes.size() - 1) {
            return null;
        }

        // The tree centre gives the shallowest drawing; ties broken by degree
        // then name so the layout is deterministic.
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
            for (String next : new java.util.TreeSet<>(adjacency.get(current))) {
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

    /** Leaves take slots left to right; a parent centres over its children. */
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
                for (String neighbour : adjacency.get(node)) {
                    if (seen.add(neighbour)) {
                        next.add(neighbour);
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

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String fmt5(double value) {
        return String.format(Locale.ROOT, "%.5f", value);
    }
}
