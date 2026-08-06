package mil.army.usace.hec.units.viz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import cwms.units.Unit;
import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.view.Json;

/**
 * The two datasets the search tab runs on: every conversion, and every unit.
 *
 * Field names are short because this ships inside the page. The rendered detail
 * of a conversion is not included, since the page already carries it on the
 * matrix cells and graph edges and can read it back from there.
 */
final class SearchIndex {

    private SearchIndex() {
    }

    static String script(Graph generated, Graph direct, Map<String, Unit> units) {
        var kinds = new HashMap<String, String>();
        var neighbors = new TreeMap<String, TreeSet<String>>();
        for (Edge edge : direct.edges()) {
            String kind = edge.label() == null ? "" : edge.label().split("\\|")[0];
            kinds.put(pair(edge.from(), edge.to()), kind);
            kinds.putIfAbsent(pair(edge.to(), edge.from()), kind);
            neighbors.computeIfAbsent(edge.from(), key -> new TreeSet<>()).add(edge.to());
            neighbors.computeIfAbsent(edge.to(), key -> new TreeSet<>()).add(edge.from());
        }

        // Passed, failed and untested per unit, counting every conversion the
        // unit takes part in - which is what "is this unit covered" means.
        var tallies = new HashMap<String, int[]>();
        var rows = new ArrayList<String>();
        for (Edge edge : generated.edges()) {
            Unit from = units.get(edge.from());
            Unit to = units.get(edge.to());
            if (from == null || to == null) {
                continue;
            }
            rows.add(row(edge, from, to, kinds.get(pair(edge.from(), edge.to()))));
            int slot = slot(edge.status());
            tallies.computeIfAbsent(edge.from(), key -> new int[3])[slot]++;
            tallies.computeIfAbsent(edge.to(), key -> new int[3])[slot]++;
        }
        rows.sort(Comparator.naturalOrder());

        return "var INDEX=[" + String.join(",", rows) + "];\n"
             + "var UNITS=" + unitsJson(units, neighbors, tallies) + ";\n";
    }

    private static String row(Edge edge, Unit from, Unit to, String kind) {
        return "{\"f\":" + Json.str(from.getAbbreviation())
             + ",\"t\":" + Json.str(to.getAbbreviation())
             + ",\"fn\":" + Json.str(from.getName())
             + ",\"tn\":" + Json.str(to.getName())
             + ",\"d\":" + Json.str(from.getAbstractParameter())
             + ",\"s\":" + Json.str(status(edge.status()))
             + ",\"h\":" + (edge.label() == null ? "null" : edge.label())
             + ",\"k\":" + Json.str(kind)
             // The pair's detail, which used to ride on every matrix cell as
             // escaped markup. The page builds the panel around it on demand.
             + (edge.detail() == null ? "" : ",\"x\":" + edge.detail())
             + "}";
    }

    private static String unitsJson(Map<String, Unit> units,
                                    Map<String, TreeSet<String>> neighbors,
                                    Map<String, int[]> tallies) {
        var entries = new ArrayList<String>();
        new TreeMap<>(units).forEach((id, unit) -> entries.add(Json.str(id) + ":"
            + unitJson(unit, neighbors.getOrDefault(id, new TreeSet<>()),
                       tallies.getOrDefault(id, new int[3]))));
        return "{" + String.join(",", entries) + "}";
    }

    private static String unitJson(Unit unit, Collection<String> neighbors, int[] tally) {
        return "{\"n\":" + Json.str(unit.getName())
             + ",\"d\":" + Json.str(unit.getAbstractParameter())
             + ",\"y\":" + Json.str(unit.getSystem())
             + ",\"x\":" + Json.str(unit.getDescription())
             + ",\"a\":" + array(unit.getAliases())
             + ",\"nb\":" + array(neighbors)
             + ",\"c\":[" + tally[0] + "," + tally[1] + "," + tally[2] + "]}";
    }

    private static String array(Collection<String> values) {
        var items = new ArrayList<String>();
        values.forEach(value -> items.add(Json.str(value)));
        return "[" + String.join(",", items) + "]";
    }

    /** Null separator, because several abbreviations contain spaces. */
    private static String pair(String from, String to) {
        return from + (char) 0 + to;
    }

    private static int slot(EdgeStatus status) {
        if (status == EdgeStatus.PASSED) {
            return 0;
        }
        return status == EdgeStatus.FAILED ? 1 : 2;
    }

    private static String status(EdgeStatus status) {
        if (status == EdgeStatus.PASSED) {
            return "passed";
        }
        if (status == EdgeStatus.FAILED) {
            return "failed";
        }
        return "untested";
    }
}
