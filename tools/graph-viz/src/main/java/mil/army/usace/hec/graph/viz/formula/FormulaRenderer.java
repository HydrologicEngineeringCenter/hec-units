package mil.army.usace.hec.graph.viz.formula;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class FormulaRenderer {

    /** Inputs used to check that the extracted (a, b) really describe the whole formula. */
    private static final double[] VERIFY_AT = {2.0, -3.0, 0.5, 137.0, 1.0e6};

    /**
     * Resolves constant names to their values: "i * m_per_ft" with
     * {"m_per_ft": "0.3048"} becomes "i * (0.3048)".
     */
    public static Substitution substitute(String expr, Map<String, String> constants) {
        var used = new ArrayList<String>();

        var names = new ArrayList<>(constants.keySet());
        names.sort(Comparator.comparingInt(String::length).reversed());

        for (String name : names) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
            Matcher matcher = pattern.matcher(expr);
            if (matcher.find()) {
                used.add(name); // loops through constants to try to find one in String expr
                String replacement = "(" + constants.get(name) + ")";
                expr = matcher.replaceAll(Matcher.quoteReplacement(replacement));
            }
        }
        // returns a substituted expression and the symbol it replaced (in used)
        return new Substitution(expr, List.copyOf(used));
    }

    // Basically derive the slope and y-intercept from the expressian
    public static AffineForm affineOf(String expr) {
        double f0;
        double f1;
        try {
            f0 = ExpressionEvaluator.evaluate(expr, 0.0);
            f1 = ExpressionEvaluator.evaluate(expr, 1.0);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!Double.isFinite(f0) || !Double.isFinite(f1)) {
            return null;
        }

        double b = f0;
        double m = f1 - f0;

        // We are basically making sure the given formula is linear before putting it in affine form
        for (double x : VERIFY_AT) {
            double actual;
            try {
                actual = ExpressionEvaluator.evaluate(expr, x);
            } catch (IllegalArgumentException e) {
                return null;
            }
            if (!Double.isFinite(actual)) {
                return null;
            }
            if (Math.abs((m * x + b) - actual) > 1e-9 * Math.max(1.0, Math.abs(actual))) {
                return null;
            }
        }
        return new AffineForm(m, b);
    }

}