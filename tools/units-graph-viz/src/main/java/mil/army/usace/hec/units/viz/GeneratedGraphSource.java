package mil.army.usace.hec.units.viz;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.stream.XMLStreamException;

import cwms.units.Loader;
import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

/**
 * Builds the post-algorithm graph.
 * Units come from Loader (which knows each unit's dimension); edges and their
 * statuses come from the test report.
 */
public final class GeneratedGraphSource {

    private GeneratedGraphSource() {
    }

    public static Graph load(Path report) throws IOException, XMLStreamException {
        var loader = new Loader();

        var nodes = new ArrayList<Node>();
        var known = new HashSet<String>();
        loader.getUnits().forEach((abbreviation, unit) -> {
            nodes.add(new Node(abbreviation, unit.getName(), unit.getAbstractParameter()));
            known.add(abbreviation);
        });

        var edges = new ArrayList<Edge>();
        var stale = new ArrayList<String>();
        for (Edge edge : TestReportReader.read(report)) {
            if (known.contains(edge.from()) && known.contains(edge.to())) {
                edges.add(edge);
            } else {
                stale.add(edge.from() + " -> " + edge.to());
            }
        }

        if (!stale.isEmpty()) {
            System.err.println("warning: skipped " + stale.size()
                + " report entries naming units that no longer exist."
                + " The report is probably stale - rerun './gradlew :units:test'."
                + " First few: " + stale.subList(0, Math.min(5, stale.size())));
        }

        return new Graph(nodes, edges);
    }
}