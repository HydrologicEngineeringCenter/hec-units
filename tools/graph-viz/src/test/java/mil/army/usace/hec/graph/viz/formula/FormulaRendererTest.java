package mil.army.usace.hec.graph.viz.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FormulaRendererTest {

    @ParameterizedTest(name = "{2}")
    @CsvSource(delimiter = '|', value = {
        "function: i * m_per_ft^3    | i * m_per_ft^3       | a function body passes through unchanged",
        "linear: unit_per_kilo 0     | i * unit_per_kilo    | a linear scale becomes a multiplication",
        "linear: 1 273.15            | i + 273.15           | a scale of one drops but the offset stays",
        "linear: milli_per_unit 0.0  | i * milli_per_unit   | a zero offset drops however it is spelled",
        "linear: 1 0                 | i                    | an identity conversion reduces to the variable"
    })
    void renders_a_method_as_an_expression(String method, String expected, String rule) {
        assertEquals(expected, FormulaRenderer.symbolic(method));
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(delimiter = '|', value = {
        "5280.0                 | 5280                        | a whole number keeps no decimal point",
        "1.47197952E11          | 147197952000                | a large whole number is not made exponential",
        "1.7999999999999972     | 1.8                         | floating point noise is trimmed",
        "273.15                 | 273.15                      | a genuine decimal survives",
        "0.028316846592         | 0.028316846592              | every significant digit of a factor is kept",
        "2.390057361376673E-5   | 2.3900573614 × 10⁻⁵         | only an extreme value goes exponential"
    })
    void formats_a_number_for_reading(double value, String expected, String rule) {
        assertEquals(expected, FormulaRenderer.formatNumber(value));
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', value = {
        "(i^2)/1000            | the exponent applies to the input, not a constant",
        "1 / i                 | dividing by the input yields Infinity rather than throwing",
        "i * still_a_name      | an unresolved constant cannot be evaluated at all",
        "i * (i - 1) * (i - 2) | a cubic that is collinear at 0, 1 and 2"
    })
    void rejects_a_formula_that_is_not_affine(String expression, String why) {
        assertNull(FormulaRenderer.affineOf(expression));
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
}
