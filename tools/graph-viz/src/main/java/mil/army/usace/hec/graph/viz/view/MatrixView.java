package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

/** Renders a graph as one coverage matrix per node group. */
public final class MatrixView {

    private static final String GRID = """
        <div class="grid">
          {{cards}}
        </div>
        """;

    private static final String CARD = """
        <div class="card" style="--i:{{index}}" data-name="{{group}}" data-find="{{find}}"
         data-failed="{{failed}}" data-untested="{{untested}}">
          <header><h2>{{group}}</h2><span class="meta">{{units}} units</span>{{tally}}</header>
          <div class="thumb scroll"><div class="mx" data-group="{{group}}"></div></div>
        </div>
        """;

    private MatrixView() {
    }

    public static String render(Graph graph) {
        var groups = groupNodes(graph);
        var cards = new StringBuilder();
        int[] index = {0};                      // drives the staggered entrance
        groups.forEach((group, members) -> {
            if (members.size() >= 2) {          // a lone unit has nothing to convert to
                cards.append(card(group, members, graph, index[0]++));
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

    private static String card(String group, List<Node> members, Graph graph, int index) {
        var counts = counts(members, graph);
        return Html.fill(CARD)
            .put("group", group)
            .put("index", index)
            .put("units", members.size())
            .put("find", searchText(group, members))
            .put("failed", counts.getOrDefault("failed", 0))
            .put("untested", counts.getOrDefault("untested", 0))
            .raw("tally", tally(members, graph))
            .render();
    }

    /** Package-visible so Stats classifies cells exactly as the matrix draws them. */
    static String stateOf(Optional<Edge> edge) {
        return edge.map(MatrixView::state)
            .orElse("missing");             // no conversion exists between this pair
    }

    private static String state(Edge edge) {
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

    private static TreeMap<String, Integer> counts(List<Node> members, Graph graph) {
        var counts = new TreeMap<String, Integer>();
        for (Node from : members) {
            for (Node to : members) {
                if (!from.id().equals(to.id())) {
                    counts.merge(stateOf(graph.edge(from.id(), to.id())), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    /** Everything a card should match on: its group and every unit inside it. */
    private static String searchText(String group, List<Node> members) {
        var text = new StringBuilder(group);
        for (Node node : members) {
            text.append(' ').append(node.id()).append(' ').append(node.label());
        }
        return text.toString().toLowerCase(java.util.Locale.ROOT);
    }

    /** Counts beside the heading, so dimensions can be triaged without opening them. */
    private static String tally(List<Node> members, Graph graph) {
        var counts = counts(members, graph);
        var badges = new StringBuilder();
        counts.forEach((state, count) -> badges.append(
            Html.tag("span").attr("class", "badge " + state).text(count)));
        return Html.tag("span").attr("class", "tally").html(badges.toString()).toString();
    }
}
