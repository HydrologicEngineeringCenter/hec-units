package mil.army.usace.hec.graph.viz.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExpressionEvaluatorTest {

    @Test
    void evaluates_a_plain_number() {
        assertEquals(5.0, ExpressionEvaluator.evaluate("5", 0.0), 1e-9);
    }

    @Test
    void substitutes_the_variable() {
        assertEquals(7.0, ExpressionEvaluator.evaluate("i", 7.0), 1e-9);
    }

    @Test
    void respects_operator_precedence() {
        assertEquals(14.0, ExpressionEvaluator.evaluate("2 + 3 * 4", 0.0), 1e-9);
    }

    @Test
    void parentheses_override_precedence() {
        assertEquals(20.0, ExpressionEvaluator.evaluate("(2 + 3) * 4", 0.0), 1e-9);
    }

    @Test
    void power_is_right_associative() {
        assertEquals(512.0, ExpressionEvaluator.evaluate("2 ^ 3 ^ 2", 0.0), 1e-9);
    }

    @Test
    void handles_unary_minus() {
        assertEquals(-4.0, ExpressionEvaluator.evaluate("-2 ^ 2", 0.0), 1e-9);
    }

    @Test
    void handles_a_linear_conversion_formula() {
        assertEquals(3.048, ExpressionEvaluator.evaluate("i * (3.048)", 1.0), 1e-9);
    }

    @Test
    void handles_a_temperature_style_formula() {
        assertEquals(0.0, ExpressionEvaluator.evaluate("(i - 32) * 5 / 9", 32.0), 1e-9);
    }
}