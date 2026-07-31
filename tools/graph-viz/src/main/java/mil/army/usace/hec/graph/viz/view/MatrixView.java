package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

/** Renders a graph as one coverage matrix per node group. */
public final class MatrixView {

    private static final String GRID = """
        <div class="grid">{{cards}}</div>
        """;

    private static final String CARD = """
        <div class="card">
        <header><h2>{{group}}</h2>{{tally}}</header>
        <div class="scroll">
        <table class="matrix">
        <thead><tr><th class="corner"></th>{{columns}}</tr></thead>
        <tbody>{{rows}}</tbody>
        </table>
        </div>
        </div>
        """;

    private static final String ROW = """
        <tr><th>{{unit}}</th>{{cells}}</tr>
        """;

    private MatrixView() {
    }

    public static String render(Graph graph) {
        var groups = groupNodes(graph);
        var cards = new StringBuilder();
        groups.forEach((group, members) -> {
            if (members.size() >= 2) {          // a lone unit has nothing to convert to
                cards.append(card(group, members, graph));
            }
        });
        return Html.fill(GRID).raw("cards", cards.toString()).render();
    }

    /** Nodes by group, each group sorted, so the page is byte-identical per run. */
    private static TreeMap<String, List<Node>> groupNodes(Graph graph) {
        var byGroup = new TreeMap<String, List<Node>>();
        for (Node node : graph.nodes()) {
            byGroup.computeIfAbsent(node.group(), key -> new ArrayList<>()).add(node);
        }
        byGroup.values().forEach(members -> members.sort(Comparator.comparing(Node::id)));
        return byGroup;
    }

    private static String card(String group, List<Node> members, Graph graph) {
        return Html.fill(CARD)
            .put("group", group)
            .raw("tally", tally(members, graph))
            .raw("columns", Html.each(members, to -> Html.tag("th").text(to.id()).toString()))
            .raw("rows", Html.each(members, from -> row(from, members, graph)))
            .render();
    }

    private static String row(Node from, List<Node> members, Graph graph) {
        return Html.fill(ROW)
            .put("unit", from.id())
            .raw("cells", Html.each(members, to -> cell(from, to, graph)))
            .render();
    }

    private static String cell(Node from, Node to, Graph graph) {
        if (from.id().equals(to.id())) {
            return "<td class=\"self\"></td>";
        }
        Edge edge = graph.edge(from.id(), to.id());
        String state = stateOf(edge);

        return Html.tag("td")
            .attr("class", state)
            .attr("title", from.id() + " → " + to.id() + ": " + state)
            .attr("data-from", from.id())
            .attr("data-to", to.id())
            // The edge carries its own description, so the enlarged view needs no
            // second copy of the graph data.
            .attr("data-detail", edge == null ? null : edge.detail())
            .html(label(edge))
            .toString();
    }

    /** Hidden by CSS at thumbnail size, where a 22px square cannot hold a digit. */
    private static String label(Edge edge) {
        if (edge == null || edge.label() == null) {
            return "";
        }
        return Html.tag("span").attr("class", "lab").text(edge.label()).toString();
    }

    /** Package-visible so Stats classifies cells exactly as the matrix draws them. */
    static String stateOf(Edge edge) {
        if (edge == null) {
            return "missing";               // no conversion exists between this pair
        }
        if (edge.status() == EdgeStatus.PASSED) {
            return "passed";
        }
        if (edge.status() == EdgeStatus.FAILED) {
            return "failed";
        }
        if (edge.status() == EdgeStatus.UNTESTED) {
            return "untested";
        }
        return "present";                   // a seed edge, carrying no status
    }

    /** Counts beside the heading, so dimensions can be triaged without opening them. */
    private static String tally(List<Node> members, Graph graph) {
        var counts = new TreeMap<String, Integer>();
        for (Node from : members) {
            for (Node to : members) {
                if (!from.id().equals(to.id())) {
                    counts.merge(stateOf(graph.edge(from.id(), to.id())), 1, Integer::sum);
                }
            }
        }
        var badges = new StringBuilder();
        counts.forEach((state, count) -> badges.append(
            Html.tag("span").attr("class", "badge " + state).text(count)));
        return Html.tag("span").attr("class", "tally").html(badges.toString()).toString();
    }
}
