package mil.army.usace.hec.graph.viz.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FormulaRendererTest {

    @Test
    void passes_a_function_body_through_unchanged() {
        assertEquals("i * m_per_ft^3", FormulaRenderer.symbolic("function: i * m_per_ft^3"));
    }

    @Test
    void turns_a_linear_scale_into_an_expression() {
        assertEquals("i * unit_per_kilo", FormulaRenderer.symbolic("linear: unit_per_kilo 0"));
    }

    @Test
    void drops_a_scale_of_one_but_keeps_the_offset() {
        assertEquals("i + 273.15", FormulaRenderer.symbolic("linear: 1 273.15"));
    }

    /** Some entries write the offset as "0.0" rather than "0". */
    @Test
    void treats_a_zero_offset_the_same_however_it_is_spelled() {
        assertEquals("i * milli_per_unit", FormulaRenderer.symbolic("linear: milli_per_unit 0.0"));
    }

    @Test
    void reduces_an_identity_conversion_to_the_variable() {
        assertEquals("i", FormulaRenderer.symbolic("linear: 1 0"));
    }

    /** The whole chain, end to end, on a linear conversion. */
    @Test
    void a_linear_conversion_survives_the_full_pipeline() {
        var expr = FormulaRenderer.symbolic("linear: 1 273.15");
        var sub = FormulaRenderer.substitute(expr, Map.of());
        var form = FormulaRenderer.affineOf(sub.expression());

        assertEquals(1.0, form.m(), 1e-9);
        assertEquals(273.15, form.b(), 1e-9);
    }

    @Test
    void drops_the_decimal_point_on_whole_numbers() {
        assertEquals("5280", FormulaRenderer.formatNumber(5280.0));
        assertEquals("147197952000", FormulaRenderer.formatNumber(1.47197952E11));
    }

    @Test
    void trims_floating_point_noise() {
        assertEquals("1.8", FormulaRenderer.formatNumber(1.7999999999999972));
        assertEquals("273.15", FormulaRenderer.formatNumber(273.15));
    }

    @Test
    void keeps_the_significant_digits_of_a_conversion_factor() {
        assertEquals("0.028316846592", FormulaRenderer.formatNumber(0.028316846592));
    }

    @Test
    void uses_scientific_notation_only_for_extreme_values() {
        assertEquals("2.3900573614 \u00d7 10\u207b\u2075",
                    FormulaRenderer.formatNumber(2.390057361376673E-5));
    }

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

    /** Collinear at 0, 1 and 2 - only the extra verification points reject it. */
    @Test
    void rejects_a_cubic_that_fools_a_three_point_probe() {
        assertNull(FormulaRenderer.affineOf("i * (i - 1) * (i - 2)"));
    }
}