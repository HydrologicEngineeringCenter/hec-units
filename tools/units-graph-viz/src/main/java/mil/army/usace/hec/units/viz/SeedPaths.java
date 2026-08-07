package mil.army.usace.hec.units.viz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import cwms.units.Loader;
import mil.army.usace.hec.graph.viz.formula.AffineForm;
import mil.army.usace.hec.graph.viz.formula.FormulaRenderer;
import mil.army.usace.hec.graph.viz.formula.Substitution;
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

    /**
     * Each hand-written conversion as it was actually authored, with the
     * constants it names resolved.
     *
     * The converter shows its working, and "x 0.3048" is only half an answer -
     * the other half is that 0.3048 is m_per_ft, which is where you would go to
     * check it. Nested by from and to so no separator has to be invented for
     * unit names that already contain slashes and spaces.
     */
    static String formulas() throws java.io.IOException {
        RawSeedData raw = RawSeedData.load();
        Map<String, String> constants = raw.constants();
        var byFrom = new java.util.LinkedHashMap<String, List<String>>();

        for (RawSeedData.Row row : raw.rows()) {
            String symbolic = FormulaRenderer.symbolic(row.method());
            Substitution substitution = FormulaRenderer.substitute(symbolic, constants);

            var used = new ArrayList<String>();
            for (String name : substitution.used()) {
                used.add(quote(name) + ":" + quote(constants.getOrDefault(name, "")));
            }
            byFrom.computeIfAbsent(row.from(), key -> new ArrayList<>())
                  .add(quote(row.to()) + ":{\"r\":" + quote(row.method())
                       + ",\"w\":{" + String.join(",", used) + "}}");
        }

        var entries = new ArrayList<String>();
        byFrom.forEach((from, tos) ->
            entries.add(quote(from) + ":{" + String.join(",", tos) + "}"));
        return "var FORMULA={" + String.join(",", entries) + "};\n";
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
