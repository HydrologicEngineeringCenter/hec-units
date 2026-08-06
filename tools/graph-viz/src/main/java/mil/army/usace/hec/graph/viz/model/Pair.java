package mil.army.usace.hec.graph.viz.model;

/**
Pair is mainly used as a way to find whether there is an edge from X to Y in constant time. This is used in maps as a key
 */
public record Pair(String from, String to) {

    public static Pair unordered(String a, String b) {
        return a.compareTo(b) <= 0 ? new Pair(a, b) : new Pair(b, a);
    }
}
