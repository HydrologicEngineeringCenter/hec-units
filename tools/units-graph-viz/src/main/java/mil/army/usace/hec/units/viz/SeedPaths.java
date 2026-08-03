package mil.army.usace.hec.units.viz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cwms.units.Loader;
import mil.army.usace.hec.graph.viz.formula.AffineForm;
import mil.army.usace.hec.graph.viz.formula.FormulaRenderer;
import net.hobbyscience.SimplePostfixCalculator;
import net.hobbyscience.database.Conversion;

/**
 * The hand-written conversions, as data the page walks at runtime.
 */
final class SeedPaths {

    static String script(Loader loader) {
        var rows = new ArrayList<String>();
        for (Conversion conversion : loader.getConversions()) {
            final String postfix;
            try {
                postfix = conversion.getMethod().getPostfix();
            } catch (Exception e) {
                continue;
            }
            AffineForm form = FormulaRenderer.affineOf(x -> SimplePostfixCalculator.calculate(postfix, x));
            if (form == null || form.m() == 0.0) {
                continue;   // cannot be composed into a route factor
            }
            rows.add("[" + quote(conversion.getFrom().getAbbreviation())
                   + "," + quote(conversion.getTo().getAbbreviation())
                   + "," + number(form.m()) + "," + number(form.b()) + "]");
        }
        rows.sort(Comparator.naturalOrder());   // stable output between runs
        return "var SEED=[" + String.join(",", rows) + "];\n";
    }

    private static String number(double value) {
        // Full precision on purpose: these get multiplied together along a route,
        // so rounding here would compound into a visibly wrong factor.
        return Double.toString(value);
    }

    private static String quote(String text) {
        var out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '<') {
                out.append("\\u003c");      // cannot end the enclosing script tag
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }

    // Convenience for callers that only have the list
    static List<String> unitsWithSeedEdges(Loader loader) {
        var units = new ArrayList<String>();
        for (Conversion conversion : loader.getConversions()) {
            units.add(conversion.getFrom().getAbbreviation());
            units.add(conversion.getTo().getAbbreviation());
        }
        return units;
    }
}
