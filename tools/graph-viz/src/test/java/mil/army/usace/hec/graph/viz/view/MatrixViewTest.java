package mil.army.usace.hec.graph.viz.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

class MatrixViewTest {

    private static Graph sample() {
        return new Graph(
            List.of(new Node("ft", "Feet", "Length"),
                    new Node("m", "Metres", "Length"),
                    new Node("in", "Inches", "Length")),
            List.of(new Edge("ft", "m", EdgeStatus.PASSED),
                    new Edge("m", "ft", EdgeStatus.FAILED),
                    new Edge("ft", "in", EdgeStatus.UNTESTED)));
    }

    @Test
    void renders_a_table_for_the_group() {
        var html = MatrixView.render(sample());

        assertTrue(html.contains("<h2>Length"));
        assertTrue(html.contains("<table class=\"matrix\">"));
    }

    @Test
    void labels_each_cell_with_its_pair_and_status() {
        var html = MatrixView.render(sample());

        assertTrue(html.contains("title=\"ft \u2192 m: passed\""));
        assertTrue(html.contains("title=\"m \u2192 ft: failed\""));
        assertTrue(html.contains("title=\"ft \u2192 in: untested\""));
    }

    @Test
    void marks_an_absent_pair_as_missing_not_untested() {
        var html = MatrixView.render(sample());

        assertTrue(html.contains("title=\"in \u2192 ft: missing\""));
        assertFalse(html.contains("title=\"in \u2192 ft: untested\""));
    }

    @Test
    void skips_a_group_with_only_one_member() {
        var lonely = new Graph(List.of(new Node("x", "X", "Solo")), List.of());

        assertFalse(MatrixView.render(lonely).contains("Solo"));
    }
}