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

    private MatrixView() {
    }

    public static String render(Graph graph) {
        var byGroup = new TreeMap<String, List<Node>>();
        for (Node node : graph.nodes()) {
            byGroup.computeIfAbsent(node.group(), key -> new ArrayList<>()).add(node);
        }

        var out = new StringBuilder("<div class=\"grid\">\n");
        for (var entry : byGroup.entrySet()) {
            List<Node> members = entry.getValue();
            if (members.size() < 2) {
                continue;   // skip lone units
            }
            members.sort(Comparator.comparing(Node::id));
            renderGroup(out, entry.getKey(), members, graph);
        }
        return out.append("</div>\n").toString();
    }

    private static void renderGroup(StringBuilder out, String group, List<Node> members, Graph graph) {
        out.append("<div class=\"card\">\n<header><h2>")
           .append(Html.escape(group))
           .append("</h2>")
           .append(tally(members, graph))
           .append("</header>\n<div class=\"scroll\">\n<table class=\"matrix\">\n");

        out.append("<thead><tr><th></th>");
        for (Node to : members) {
            out.append("<th>").append(Html.escape(to.id())).append("</th>");
        }
        out.append("</tr></thead>\n<tbody>\n");

        for (Node from : members) {
            out.append("<tr><th>").append(Html.escape(from.id())).append("</th>");
            for (Node to : members) {
                appendCell(out, graph, from, to);
            }
            out.append("</tr>\n");
        }

        out.append("</tbody>\n</table>\n</div>\n</div>\n");
    }

    private static void appendCell(StringBuilder out, Graph graph, Node from, Node to) {
        if (from.id().equals(to.id())) {
            out.append("<td class=\"self\"></td>");
            return;
        }
        Edge edge = graph.edge(from.id(), to.id());
        String state = stateOf(edge);

        out.append("<td class=\"").append(state).append("\" title=\"")
           .append(Html.escape(from.id() + " \u2192 " + to.id() + ": " + state))
           .append("\"");

        // The edge's own description travels with the cell, so the enlarged view
        // needs no second copy of the graph data to look anything up in.
        if (edge != null && edge.detail() != null) {
            out.append(" data-detail=\"").append(Html.escape(edge.detail())).append("\"");
        }
        out.append("></td>");
    }

    private static String stateOf(Edge edge) {
        if (edge == null) {
            return "missing";           // no conversion exists between this pair
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
        return "present";               // a seed edge, which carries no status at all
    }

    // Per group counts, shown beside the heading so you can rank dimensions by coverage.
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
            out.append("<span class=\"badge ").append(state).append("\">")
               .append(n).append("</span>"));
        return out.append("</span>").toString();
    }
}