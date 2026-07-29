package mil.army.usace.hec.graph.viz.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;



class GraphTest {

    @Test
    void edge_lookup_test() {
        var a = new Node("a", "Apple", "fruits");
        var b = new Node("b", "Banana", "fruits");
        var edge = new Edge("a", "b", EdgeStatus.PASSED);

        var graph = new Graph(List.of(a, b), List.of(edge));

        assertEquals(edge, graph.edge("a", "b"));
    }

    @Test
    void an_unknown_pair_has_no_edge() {
        var graph = new Graph(List.of(), List.of());

        assertNull(graph.edge("x", "y"));
    }

    @Test
    void direction_matters() {
        var a = new Node("a", "A", "group1");
        var b = new Node("b", "B", "group1");
        var edge = new Edge("a", "b", EdgeStatus.PASSED);
        var graph = new Graph(List.of(a, b), List.of(edge));
        assertNull(graph.edge("b", "a"));
    }
}