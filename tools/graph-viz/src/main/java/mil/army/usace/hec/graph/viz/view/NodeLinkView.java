package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

// One card per group, each holding a container that cytoscape draws into.
 
public final class NodeLinkView {

    private static final String CARD = """
        <div class="card seedcard {{shape}}" style="--i:{{index}}">
          <header><h2>{{group}}</h2><span class="badge {{shape}}">{{badge}}</span></header>
          <p class="meta">{{meta}}</p>
          <div class="cy" data-group="{{group2}}" style="aspect-ratio:{{ratio}}"></div>
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

        // Alphabetical, matching the coverage tab
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
        Map<String, Integer> shape = CytoscapeData.shape(nodes, edges);
        int cycles = shape.get("cycles");
        int parallel = shape.get("parallel");

        String kind;
        String badge;
        if (cycles > 0) {
            kind = "cyclic";
            badge = cycles + (cycles > 1 ? " cycles" : " cycle");
        } else if (parallel > 0) {
            kind = "dup";
            badge = parallel + " duplicate edge" + (parallel > 1 ? "s" : "");
        } else {
            kind = "tree";
            badge = "tree";
        }

        return Html.fill(CARD)
            .put("group", group)
            .put("group2", group)
            .put("index", index)
            .put("shape", kind)
            .put("badge", badge)
            .put("meta", nodes.size() + " units")
            .put("ratio", ratio(nodes, edges))
            .render();
    }
    private static String ratio(List<Node> nodes, List<Edge> edges) {
        GraphLayout.Placed placed = GraphLayout.of(nodes, edges);
        return String.format(java.util.Locale.ROOT, "%.0f/%.0f",
                             placed.width(), placed.height());
    }
}
