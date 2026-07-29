package mil.army.usace.hec.graph.viz.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FormulaRendererTest {

    @Test
    void substitutes_a_single_constant() {
        var result = FormulaRenderer.substitute("i * m_per_ft", Map.of("m_per_ft", "0.3048"));

        assertEquals("i * (0.3048)", result.expression());
        assertEquals(List.of("m_per_ft"), result.used());
    }

    @Test
    void leaves_expressions_with_no_constants_alone() {
        var result = FormulaRenderer.substitute("(i * 9/5) + 32", Map.of("m_per_ft", "0.3048"));

        assertEquals("(i * 9/5) + 32", result.expression());
        assertTrue(result.used().isEmpty());
    }

    /** The real collision that the production Loader only survives by JSON ordering. */
    @Test
    void does_not_replace_a_short_name_inside_a_longer_one() {
        var constants = Map.of("kg_per_lbm", "4.5359237E-1", "g_per_lbm", "453.59237");

        var result = FormulaRenderer.substitute("i * kg_per_lbm", constants);

        assertEquals("i * (4.5359237E-1)", result.expression());
        assertEquals(List.of("kg_per_lbm"), result.used());
    }

    /** ft3 -> m3: looks nonlinear, but the exponent is on the constant, not on i. */
    @Test
    void treats_a_constant_raised_to_a_power_as_a_plain_scale() {
        var form = FormulaRenderer.affineOf("i * (3.048E-1)^3");

        assertEquals(0.028316846592, form.m(), 1e-15);
        assertEquals(0.0, form.b(), 1e-15);
    }

    @Test
    void finds_both_the_scale_and_the_offset() {
        var form = FormulaRenderer.affineOf("(i * 9/5) + 32");

        assertEquals(1.8, form.m(), 1e-9);
        assertEquals(32.0, form.b(), 1e-9);
    }

    /** Hz -> B: the exponent is on i itself, so no single a and b can describe it. */
    @Test
    void rejects_a_formula_whose_exponent_applies_to_the_input() {
        assertNull(FormulaRenderer.affineOf("(i^2)/1000"));
    }

    /** Java yields Infinity here instead of throwing, so this needs its own guard. */
    @Test
    void rejects_a_formula_that_divides_by_the_input() {
        assertNull(FormulaRenderer.affineOf("1 / i"));
    }

    @Test
    void rejects_an_expression_it_cannot_evaluate() {
        assertNull(FormulaRenderer.affineOf("i * still_a_name"));
    }
}