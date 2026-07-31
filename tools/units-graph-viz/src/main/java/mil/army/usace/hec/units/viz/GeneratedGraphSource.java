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
import net.hobbyscience.database.Conversion;

/**
 * Builds the post-algorithm graph from three sources:
 *
 *   ConversionGraph - every pair, its formula, its hop chain
 *   the test report - which pairs were tested, and the outcome
 *   the test CSV    - the inputs and expected values behind those outcomes
 *
 * Only the first is required; without a report every edge is UNTESTED.
 */
public final class GeneratedGraphSource {

    private GeneratedGraphSource() {
    }

    /** True when a report exists to read coverage from. */
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

    /** Edges from the report, each carrying its status and its description. */
    private static List<Edge> withCoverage(Path report, Map<String, Conversion> conversions,
                                           Map<String, List<TestCase>> tests,
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
     *
     * UNTESTED rather than null is the honest answer: these conversions could
     * have been tested, and we simply do not know whether they were.
     */
    private static List<Edge> withoutCoverage(Map<String, Conversion> conversions,
                                              Map<String, List<TestCase>> tests,
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
                              Map<String, Conversion> conversions,
                              Map<String, List<TestCase>> tests,
                              Map<String, String> names) {
        Conversion conversion = conversions.get(pair(from, to));
        if (conversion == null) {
            return new Edge(from, to, status);
        }
        List<TestCase> direct = tests.getOrDefault(TestCaseReader.key(from, to), List.of());
        List<TestCase> roundTrip = tests.getOrDefault(TestCaseReader.key(to, from), List.of());

        // Reproducing a round-trip test needs the opposite conversion too: the
        // suite runs that one first and feeds its result back through this one.
        String inversePostfix = postfixOf(conversions.get(pair(to, from)));

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
     *
     * The report knows nothing about formulas, so this is the only way a cell can
     * show what it computes. generateConversions() returns each pair in one
     * direction only, so the inverse is derived too - a matrix has cells on both
     * sides of the diagonal.
     */
    private static Map<String, Conversion> conversionsByPair(Loader loader) {
        var generated = new ConversionGraph(loader.getConversions()).generateConversions();
        var conversions = new HashMap<String, Conversion>();

        // Two passes, and the order is the point. generateConversions() returns a
        // HashSet, so its iteration order is arbitrary; interleaving these would
        // let a derived inverse land on a pair the algorithm already produced
        // natively and win purely by luck. Every conversion the algorithm states
        // outright is recorded first, and inverses only fill what is left.
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

    private static void record(Map<String, Conversion> conversions, Conversion conversion) {
        conversions.putIfAbsent(pair(conversion.getFrom().getAbbreviation(),
                                     conversion.getTo().getAbbreviation()),
                                conversion);
    }

    /**
     * Joins two abbreviations into a lookup key.
     *
     * The separator is a null character rather than a space because several
     * abbreviations contain spaces of their own - "1000 acre", "1000 m3" - and a
     * space would let two different pairs produce the same key.
     */
    private static String pair(String from, String to) {
        return from + "\u0000" + to;
    }
}
