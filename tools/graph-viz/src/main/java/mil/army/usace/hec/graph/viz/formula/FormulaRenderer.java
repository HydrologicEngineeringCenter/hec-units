package mil.army.usace.hec.graph.viz.formula;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class FormulaRenderer {

    /** Inputs used to check that the extracted (a, b) really describe the whole formula. */
    private static final double[] VERIFY_AT = {2.0, -3.0, 0.5, 137.0, 1.0e6};
    private static final String SUPERSCRIPT_DIGITS =
    "\u2070\u00b9\u00b2\u00b3\u2074\u2075\u2076\u2077\u2078\u2079";

    /**
     * Conversions a conversions.json row into a symbolic equation
     */
    public static String symbolic(String method) {
        int colon = method.indexOf(':');
        if (colon < 0) {
            return method.trim();
        }
        String kind = method.substring(0, colon).trim();
        String body = method.substring(colon + 1).trim();

        if (!kind.equalsIgnoreCase("linear")) {
            return body;
        }

        String[] parts = body.split("\\s+");
        if (parts.length != 2) {
            return body;
        }
        String scale = parts[0];
        String offset = parts[1];

        var sb = new StringBuilder();
        if (scale.equals("1") || scale.equals("1.0")) {
            sb.append("i");
        } else {
            sb.append("i * ").append(scale);
        }
        if (!offset.equals("0") && !offset.equals("0.0")) {
            sb.append(" + ").append(offset);
        }
        return sb.toString();
    }

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
        return affineOf(x -> ExpressionEvaluator.evaluate(expr, x));
    }

    public static AffineForm affineOf(DoubleUnaryOperator formula) {
        double f0;
        double f1;
        try {
            f0 = formula.applyAsDouble(0.0);
            f1 = formula.applyAsDouble(1.0);
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
                actual = formula.applyAsDouble(x);
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

    public static String formatNumber(double v) {
        if (!Double.isFinite(v)) {
            return Double.toString(v);
        }
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return Long.toString((long) v);
        }
        if (v != 0 && (Math.abs(v) < 1e-4 || Math.abs(v) >= 1e9)) {
            String[] parts = String.format("%.10e", v).split("e");
            return stripTrailingZeros(parts[0]) + " \u00d7 10" + superscript(Integer.parseInt(parts[1]));
        }

        return stripTrailingZeros(String.format("%.12g", v));
    }

    private static String stripTrailingZeros(String s) {
        if (s.indexOf('.') < 0) {
            return s;          // no decimal point - "5280" must not become "528"
        }
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }

    private static String superscript(int value) {
        var sb = new StringBuilder();
        if (value < 0) {
            sb.append('\u207b');
        }
        for (char c : Integer.toString(Math.abs(value)).toCharArray()) {
            sb.append(SUPERSCRIPT_DIGITS.charAt(c - '0'));
        }
        return sb.toString();
    }

}