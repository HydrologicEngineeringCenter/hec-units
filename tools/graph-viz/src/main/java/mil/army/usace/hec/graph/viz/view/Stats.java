package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

/**
 * Every number the key and the summary need, counted once.
 *
 * Knows nothing of units - it counts nodes, edges and groups.
 */
public final class Stats {

    /** One group's tally. A "pair" is an ordered pair of distinct members. */
    public record Group(String name, int units, int passed, int failed, int untested, int missing) {

        public int pairs() {
            return passed + failed + untested + missing;
        }

        /** Pairs the algorithm can actually convert - everything but the gaps. */
        public int reachable() {
            return passed + failed + untested;
        }

        public int tested() {
            return passed + failed;
        }

        /** Share of reachable conversions that a test actually exercises. */
        public double coverage() {
            return reachable() == 0 ? 0 : tested() * 100.0 / reachable();
        }
    }

    private final List<Group> groups = new ArrayList<>();
    private final Map<Integer, Integer> routeLengths = new TreeMap<>();
    private final List<String> isolated = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    private int passed;
    private int failed;
    private int untested;
    private int missing;
    private int nodeCount;
    private int singletonGroups;

    public Stats(Graph graph) {
        var byGroup = new TreeMap<String, List<Node>>();
        for (Node node : graph.nodes()) {
            byGroup.computeIfAbsent(node.group(), key -> new ArrayList<>()).add(node);
        }
        nodeCount = graph.nodes().size();

        for (var entry : byGroup.entrySet()) {
            List<Node> members = entry.getValue();
            if (members.size() < 2) {
                singletonGroups++;
                continue;   // no pairs to count, and no matrix is drawn for it
            }
            members.sort(Comparator.comparing(Node::id));
            groups.add(tally(entry.getKey(), members, graph));
        }

        // Route length comes from the edge label, which the adapter fills in.
        // Only numeric labels are counted, so a graph that labels edges some
        // other way simply produces no distribution rather than nonsense.
        for (Edge edge : graph.edges()) {
            Integer length = asInteger(edge.label());
            if (length != null) {
                routeLengths.merge(length, 1, Integer::sum);
            }
        }

        var connected = new java.util.HashSet<String>();
        for (Edge edge : graph.edges()) {
            connected.add(edge.from());
            connected.add(edge.to());
        }
        for (Node node : graph.nodes()) {
            if (!connected.contains(node.id())) {
                isolated.add(node.id());
            }
        }
    }

    private Group tally(String name, List<Node> members, Graph graph) {
        int p = 0;
        int f = 0;
        int u = 0;
        int m = 0;
        for (Node from : members) {
            for (Node to : members) {
                if (from.id().equals(to.id())) {
                    continue;
                }
                String state = MatrixView.stateOf(graph.edge(from.id(), to.id()));
                switch (state) {
                    case "passed" -> p++;
                    case "failed" -> {
                        f++;
                        failures.add(from.id() + " → " + to.id());
                    }
                    case "missing" -> m++;
                    default -> u++;
                }
            }
        }
        passed += p;
        failed += f;
        untested += u;
        missing += m;
        return new Group(name, members.size(), p, f, u, m);
    }

    private static Integer asInteger(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Group> groups() {
        return groups;
    }

    /**
     * Groups worst-covered first, which is the order worth acting on.
     *
     * Groups with nothing reachable sink to the bottom rather than topping the
     * list at 0%. They are not badly covered - there is simply nothing there to
     * cover, and letting them crowd out the real gaps defeats the sort.
     */
    public List<Group> groupsByCoverage() {
        var sorted = new ArrayList<>(groups);
        sorted.sort(Comparator.comparingInt((Group g) -> g.reachable() == 0 ? 1 : 0)
                              .thenComparingDouble(Group::coverage)
                              .thenComparing(Comparator.comparingInt(Group::reachable).reversed()));
        return sorted;
    }

    public Map<Integer, Integer> routeLengths() {
        return routeLengths;
    }

    public List<String> isolated() {
        return isolated;
    }

    public List<String> failures() {
        return failures;
    }

    public int passed() {
        return passed;
    }

    public int failed() {
        return failed;
    }

    public int untested() {
        return untested;
    }

    public int missing() {
        return missing;
    }

    public int nodeCount() {
        return nodeCount;
    }

    public int singletonGroups() {
        return singletonGroups;
    }

    /** Every cell a matrix draws, excluding the diagonal. */
    public int pairs() {
        return passed + failed + untested + missing;
    }

    public int reachable() {
        return passed + failed + untested;
    }

    public int tested() {
        return passed + failed;
    }

    /** Share of reachable conversions a test exercises. */
    public double coverage() {
        return reachable() == 0 ? 0 : tested() * 100.0 / reachable();
    }

    /** Share of exercised conversions that pass. */
    public double passRate() {
        return tested() == 0 ? 0 : passed * 100.0 / tested();
    }

    public double percentOfPairs(int count) {
        return pairs() == 0 ? 0 : count * 100.0 / pairs();
    }

    public static String percent(double value) {
        return String.format("%.2f%%", value);
    }
}
