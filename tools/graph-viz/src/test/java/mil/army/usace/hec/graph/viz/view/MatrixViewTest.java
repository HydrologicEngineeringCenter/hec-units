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
                    new Node("m", "Meters", "Length"),
                    new Node("in", "Inches", "Length")),
            List.of(new Edge("ft", "m", EdgeStatus.PASSED),
                    new Edge("m", "ft", EdgeStatus.FAILED),
                    new Edge("ft", "in", EdgeStatus.UNTESTED)));
    }

    @Test
    void renders_a_card_for_the_group() {
        var html = MatrixView.render(sample());

        assertTrue(html.contains("<h2>Length"));
        assertTrue(html.contains("data-name=\"Length\""));
        assertTrue(html.contains("<div class=\"mx\" data-group=\"Length\">"));
    }

    @Test
    void carries_no_table_markup_of_its_own() {
        var html = MatrixView.render(sample());

        assertFalse(html.contains("<table"), "the table is assembled in the browser");
        assertFalse(html.contains("data-detail"), "detail travels on the INDEX row");
    }

    @Test
    void counts_every_state_for_the_heading() {
        var html = MatrixView.render(sample());

        // 3 units, 6 ordered pairs: ft->m passed, m->ft failed, ft->in untested,
        // and the remaining three have no conversion at all.
        assertTrue(html.contains("badge passed\">1"));
        assertTrue(html.contains("badge failed\">1"));
        assertTrue(html.contains("badge untested\">1"));
        assertTrue(html.contains("badge missing\">3"));
    }

    @Test
    void flags_the_card_for_the_grid_filters() {
        var html = MatrixView.render(sample());

        assertTrue(html.contains("data-failed=\"1\""));
        assertTrue(html.contains("data-untested=\"1\""));
        // members are sorted by id, so ft, in, m - not declaration order
        assertTrue(html.contains("data-find=\"length ft feet in inches m meters\""));
    }

    @Test
    void skips_a_group_with_only_one_member() {
        var lonely = new Graph(List.of(new Node("x", "X", "Solo")), List.of());

        assertFalse(MatrixView.render(lonely).contains("Solo"));
    }
}