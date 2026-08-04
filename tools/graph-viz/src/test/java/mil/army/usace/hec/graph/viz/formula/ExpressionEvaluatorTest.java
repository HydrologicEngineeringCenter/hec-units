package mil.army.usace.hec.graph.viz.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionEvaluatorTest {

    @ParameterizedTest(name = "{3}")
    @CsvSource(delimiter = '|', value = {
        "5                | 0  | 5     | a plain number",
        "i                | 7  | 7     | the variable stands in for the input",
        "2 + 3 * 4        | 0  | 14    | multiplication binds tighter than addition",
        "(2 + 3) * 4      | 0  | 20    | parentheses override precedence",
        "2 ^ 3 ^ 2        | 0  | 512   | power is right associative",
        "-2 ^ 2           | 0  | -4    | unary minus applies after the power",
        "i * (3.048)      | 1  | 3.048 | a linear conversion",
        "(i - 32) * 5 / 9 | 32 | 0     | a temperature style formula"
    })
    void evaluates(String expression, double input, double expected, String rule) {
        assertEquals(expected, ExpressionEvaluator.evaluate(expression, input), 1e-9);
    }
}
