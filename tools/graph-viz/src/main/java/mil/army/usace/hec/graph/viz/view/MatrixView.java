package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

/**
 * Renders a graph as one coverage matrix per node group.
 */
public final class MatrixView {

    public static String render(Graph graph) {
        // separates nodes by their respective groups
        var byGroup = new TreeMap<String, List<Node>>();
        for (Node node : graph.nodes()) {
            byGroup.computeIfAbsent(node.group(), key -> new ArrayList<>()).add(node);
        }

        var out = new StringBuilder();
        for (var entry : byGroup.entrySet()) {
            List<Node> members = entry.getValue();
            if (members.size() < 2) {
                continue;   // a lone unit has nothing to convert to, we can ignore
            }
            members.sort(Comparator.comparing(Node::id));
            renderGroup(out, entry.getKey(), members, graph);
        }
        return out.toString();
    }

    private static void renderGroup(StringBuilder out, String group, List<Node> members, Graph graph) {
        out.append("<section class=\"dimension\">\n<h2>")
           .append(Html.escape(group))
           .append(tally(members, graph))
           .append("</h2>\n<div class=\"scroll\">\n<table class=\"matrix\">\n");

        // Column headers: the "to" unit of every cell beneath them.
        out.append("<thead><tr><th class=\"corner\"></th>");
        for (Node to : members) {
            out.append("<th>").append(Html.escape(to.id())).append("</th>");
        }
        out.append("</tr></thead>\n<tbody>\n");

        // One row per "from" unit, so reading across a row shows everything that
        // unit can convert into.
        for (Node from : members) {
            out.append("<tr><th>").append(Html.escape(from.id())).append("</th>");
            for (Node to : members) {
                appendCell(out, graph, from, to);
            }
            out.append("</tr>\n");
        }

        out.append("</tbody>\n</table>\n</div>\n</section>\n");
    }

    private static void appendCell(StringBuilder out, Graph graph, Node from, Node to) {
        if (from.id().equals(to.id())) {
            out.append("<td class=\"self\"></td>");   // the diagonal: nothing to convert
            return;
        }
        String state = stateOf(graph.edge(from.id(), to.id()));
        out.append("<td class=\"").append(state).append("\" title=\"")
           .append(Html.escape(from.id() + " \u2192 " + to.id() + ": " + state))
           .append("\"></td>");
    }

    private static String stateOf(Edge edge) {
        if (edge == null) {
            return "missing";
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
        return "present";  
    }

    /** Counts beside the heading, so 28 matrices can be triaged by scanning headings. */
    private static String tally(List<Node> members, Graph graph) {
        var counts = new TreeMap<String, Integer>();
        for (Node from : members) {
            for (Node to : members) {
                if (!from.id().equals(to.id())) {
                    counts.merge(stateOf(graph.edge(from.id(), to.id())), 1, Integer::sum);
                }
            }
        }
        var out = new StringBuilder("<span class=\"tally\">");
        counts.forEach((state, n) ->
            out.append("<b class=\"").append(state).append("\">").append(n).append("</b>"));
        return out.append("</span>").toString();
    }
}