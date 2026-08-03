package mil.army.usace.hec.graph.viz.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;



class GraphTest {

    @Test
    void edge_lookup_test() {
        var a = new Node("a", "Apple", "fruits");
        var b = new Node("b", "Banana", "fruits");
        var edge = new Edge("a", "b", EdgeStatus.PASSED);

        var graph = new Graph(List.of(a, b), List.of(edge));

        assertEquals(Optional.of(edge), graph.edge("a", "b"));
    }

    @Test
    void an_unknown_pair_has_no_edge() {
        var graph = new Graph(List.of(), List.of());

        assertTrue(graph.edge("x", "y").isEmpty());
    }

    @Test
    void direction_matters() {
        var a = new Node("a", "A", "group1");
        var b = new Node("b", "B", "group1");
        var edge = new Edge("a", "b", EdgeStatus.PASSED);
        var graph = new Graph(List.of(a, b), List.of(edge));
        assertTrue(graph.edge("b", "a").isEmpty());
    }
}