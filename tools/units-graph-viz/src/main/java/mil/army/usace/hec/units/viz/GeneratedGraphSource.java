package mil.army.usace.hec.units.viz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import cwms.units.ConversionGraph;
import cwms.units.Loader;
import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;
import mil.army.usace.hec.graph.viz.model.Pair;
import net.hobbyscience.database.Conversion;

/**
 * Builds the post-algorithm graph from three sources:
 *
 *   ConversionGraph - every pair, its formula, its hop chain
 *   the test report - which pairs were tested, and the outcome
 *   the test CSV    - the inputs and expected values behind those outcomes
 */
public final class GeneratedGraphSource {

    private GeneratedGraphSource() {
    }

    // True when a report exists to read coverage from
    public static boolean hasReport(Path report) {
        return report != null && Files.isReadable(report);
    }

    public static Graph load(Loader loader, Path report, Path testCsv)
            throws IOException, XMLStreamException {
        var nodes = new ArrayList<Node>();
        var known = new HashSet<String>();
        var names = new HashMap<String, String>();
        loader.getUnits().forEach((abbreviation, unit) -> {
            nodes.add(new Node(abbreviation, unit.getName(), unit.getAbstractParameter()));
            known.add(abbreviation);
            names.put(abbreviation, unit.getName());
        });

        var conversions = conversionsByPair(loader);
        var tests = TestCaseReader.read(testCsv);

        List<Edge> edges = hasReport(report)
            ? withCoverage(report, conversions, tests, names, known)
            : withoutCoverage(conversions, tests, names, known);

        return new Graph(nodes, edges);
    }

    // Edges from the report, each carrying its status and its description
    private static List<Edge> withCoverage(Path report, Map<Pair, Conversion> conversions,
                                           Map<Pair, List<TestCase>> tests,
                                           Map<String, String> names, HashSet<String> known)
            throws IOException, XMLStreamException {
        var edges = new ArrayList<Edge>();
        var stale = new ArrayList<String>();

        for (Edge edge : TestReportReader.read(report)) {
            if (known.contains(edge.from()) && known.contains(edge.to())) {
                edges.add(build(edge.from(), edge.to(), edge.status(), conversions, tests, names));
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
        return edges;
    }

    /**
     * Edges straight from the algorithm, with every pair marked untested.
     */
    private static List<Edge> withoutCoverage(Map<Pair, Conversion> conversions,
                                              Map<Pair, List<TestCase>> tests,
                                              Map<String, String> names,
                                              HashSet<String> known) {
        var edges = new ArrayList<Edge>();
        for (Conversion conversion : conversions.values()) {
            String from = conversion.getFrom().getAbbreviation();
            String to = conversion.getTo().getAbbreviation();
            if (known.contains(from) && known.contains(to)) {
                edges.add(build(from, to, EdgeStatus.UNTESTED, conversions, tests, names));
            }
        }
        return edges;
    }

    private static Edge build(String from, String to, EdgeStatus status,
                              Map<Pair, Conversion> conversions,
                              Map<Pair, List<TestCase>> tests,
                              Map<String, String> names) {
        Conversion conversion = conversions.get(new Pair(from, to));
        if (conversion == null) {
            return new Edge(from, to, status);
        }
        List<TestCase> direct = tests.getOrDefault(TestCaseReader.key(from, to), List.of());
        List<TestCase> roundTrip = tests.getOrDefault(TestCaseReader.key(to, from), List.of());

        // Reproducing a round-trip test needs the opposite conversion too: the
        // suite runs that one first and feeds its result back through this one.
        String inversePostfix = postfixOf(conversions.get(new Pair(to, from)));

        return new Edge(from, to, status,
                        Integer.toString(ConversionDetail.hops(conversion)),
                        ConversionDetail.of(conversion, inversePostfix, status, names,
                                            direct, roundTrip));
    }

    private static String postfixOf(Conversion conversion) {
        if (conversion == null) {
            return null;
        }
        try {
            return conversion.getMethod().getPostfix();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Runs the real conversion algorithm and indexes every pair it produces.
     */
    private static Map<Pair, Conversion> conversionsByPair(Loader loader) {
        var generated = new ConversionGraph(loader.getConversions()).generateConversions();
        var conversions = new HashMap<Pair, Conversion>();

        for (Conversion conversion : generated) {
            record(conversions, conversion);
        }
        for (Conversion conversion : generated) {
            try {
                record(conversions, conversion.getInverse());
            } catch (Exception e) {
                // Not every method can be inverted; the forward direction still shows.
            }
        }
        return conversions;
    }

    private static void record(Map<Pair, Conversion> conversions, Conversion conversion) {
        conversions.putIfAbsent(new Pair(conversion.getFrom().getAbbreviation(),
                                        conversion.getTo().getAbbreviation()),
                                conversion);
    }

}
