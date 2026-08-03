package mil.army.usace.hec.graph.viz.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class HtmlTest {

    @Test
    void escapes_markup_characters() {
        assertEquals("&lt;b&gt;&amp;&quot;", Html.escape("<b>&\""));
    }

    /** & must be escaped first, or the entities introduced get escaped again. */
    @Test
    void does_not_double_escape() {
        assertEquals("&amp;lt;", Html.escape("&lt;"));
    }

    @Test
    void fills_placeholders() {
        assertEquals("<p>hi</p>",
            Html.fill("<p>{{msg}}</p>").put("msg", "hi").render());
    }

    @Test
    void escapes_filled_values() {
        assertTrue(Html.fill("<p>{{msg}}</p>").put("msg", "<script>").render()
                       .contains("&lt;script&gt;"));
    }

    @Test
    void leaves_raw_values_alone() {
        assertEquals("<p><b>hi</b></p>",
            Html.fill("<p>{{msg}}</p>").raw("msg", "<b>hi</b>").render());
    }

    /** Nested markup lines up under the hole it fills, so templates compose
        into one correctly indented document. */
    @Test
    void nested_markup_adopts_the_indentation_of_its_hole() {
        String inner = """
            <li>one</li>
            <li>two</li>
            """;
        assertEquals("""
            <ul>
              <li>one</li>
              <li>two</li>
            </ul>
            """,
            Html.fill("""
                <ul>
                  {{items}}
                </ul>
                """).raw("items", inner).render());
    }

    @Test
    void a_hole_mid_line_is_left_alone() {
        assertEquals("  <p>a<b>b</b></p>",
            Html.fill("  <p>a{{x}}</p>").raw("x", "<b>b</b>").render());
    }

    /** A typo in a placeholder name should fail loudly, not render broken markup. */
    @Test
    void rejects_an_unfilled_placeholder() {
        var template = Html.fill("<p>{{msg}}</p>");

        var thrown = assertThrows(IllegalStateException.class, template::render);

        assertTrue(thrown.getMessage().contains("msg"));
    }

    @Test
    void rejects_a_value_with_no_placeholder() {
        var template = Html.fill("<p>{{msg}}</p>").put("msg", "hi").put("typo", "x");

        var thrown = assertThrows(IllegalStateException.class, template::render);

        assertTrue(thrown.getMessage().contains("typo"));
    }

    @Test
    void builds_a_tag_with_attributes() {
        assertEquals("<td class=\"passed\" title=\"a &gt; b\">1</td>",
            Html.tag("td").attr("class", "passed").attr("title", "a > b").text(1).toString());
    }

    /** Null attributes vanish, which is what keeps optional data out of the markup. */
    @Test
    void omits_null_attributes() {
        assertEquals("<td class=\"x\"></td>",
            Html.tag("td").attr("class", "x").attr("data-detail", null).toString());
    }

    @Test
    void renders_each_item() {
        assertEquals("<li>a</li><li>b</li>",
            Html.each(List.of("a", "b"), item -> "<li>" + item + "</li>"));
    }
}
