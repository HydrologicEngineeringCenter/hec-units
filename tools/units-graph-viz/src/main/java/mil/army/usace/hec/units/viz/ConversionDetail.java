package mil.army.usace.hec.units.viz;

import java.util.ArrayList;
import java.util.List;

import mil.army.usace.hec.graph.viz.formula.AffineForm;
import mil.army.usace.hec.graph.viz.formula.FormulaRenderer;
import mil.army.usace.hec.graph.viz.view.Json;
import net.hobbyscience.SimplePostfixCalculator;
import net.hobbyscience.database.Conversion;


final class ConversionDetail {

    private ConversionDetail() {
    }

    // Conversions chained together, counted from the hop chain
    static int hops(Conversion conversion) {
        String chain = conversion.getConversionChain();
        if (chain == null || chain.isBlank()) {
            return 1;
        }
        return Math.max(1, chain.split("->").length - 1);
    }


    static String of(Conversion conversion, String inversePostfix,
                     List<TestCase> direct, List<TestCase> roundTrip) {
        final String postfix;
        try {
            postfix = conversion.getMethod().getPostfix();
        } catch (Exception e) {
            return null;                    // unreadable formula, not worth failing the page
        }

        var fields = new ArrayList<String>();
        fields.add("\"e\":" + equation(postfix));
        String chain = chain(conversion);
        if (chain != null) {
            fields.add("\"c\":" + chain);
        }
        String tests = tests(postfix, inversePostfix, direct, roundTrip);
        if (tests != null) {
            fields.add("\"q\":" + tests);
        }
        return "{" + String.join(",", fields) + "}";
    }

    // Derived conversions expose only postfix, so the affine probe runs against that
    private static String equation(String postfix) {
        AffineForm form = FormulaRenderer.affineOf(x -> SimplePostfixCalculator.calculate(postfix, x));

        if (form == null) {
            return "[\"!\"," + Json.str("not a simple scale + offset") + "]";
        }
        if (form.m() == 0.0) {
            // A zero scale means the input is ignored, which is always a bug.
            return "[\"!\"," + Json.str("ignores its input - always "
                                        + FormulaRenderer.formatNumber(form.b())) + "]";
        }

        String factor = form.m() == 1.0 ? "null"
                      : Json.str(FormulaRenderer.formatNumber(form.m()));
        String offset = form.b() == 0.0 ? "null"
                      : Json.str((form.b() > 0 ? "+" : "−")
                                 + FormulaRenderer.formatNumber(Math.abs(form.b())));
        return "[" + factor + "," + offset + "]";
    }

    // When a derived conversion is wrong it is usually one hop in the middle
    private static String chain(Conversion conversion) {
        String chain = conversion.getConversionChain();
        if (chain == null || chain.isBlank()) {
            return null;
        }
        var steps = new ArrayList<String>();
        for (String part : chain.split("->")) {
            steps.add(Json.str(part.trim()));
        }
        return "[" + String.join(",", steps) + "]";
    }

    /**
     * Every test that runs this conversion, in this direction.
     *
     * A CSV row written the other way still exercises this conversion: the suite
     * converts across and then back, and the trip home is this one. It is listed
     * as its own test, with the intermediate value the suite actually feeds in.
     */
    private static String tests(String postfix, String inversePostfix,
                                List<TestCase> direct, List<TestCase> roundTrip) {
        boolean canRoundTrip = inversePostfix != null;
        var rows = new ArrayList<String>();

        for (TestCase test : direct) {
            rows.add(testRow(test.input(), evaluate(postfix, test.input()),
                             test.expected(), test.delta()));
        }
        if (canRoundTrip) {
            for (TestCase test : roundTrip) {
                Double there = evaluate(inversePostfix, test.input());
                Double back = there == null ? null : evaluate(postfix, there);
                rows.add(testRow(there, back, test.input(), test.inverseDelta()));
            }
        }
        return rows.isEmpty() ? null : "[" + String.join(",", rows) + "]";
    }

    private static Double evaluate(String postfix, double input) {
        try {
            double value = SimplePostfixCalculator.calculate(postfix, input);
            return Double.isFinite(value) ? value : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String testRow(Double input, Double got, double want, double delta) {
        if (input == null || got == null) {
            return "[]";                    // the page shows "could not evaluate"
        }

        double off = Math.abs(got - want);
        boolean ok = off <= delta;

        return "[" + Json.str(FormulaRenderer.formatNumber(input))
             + "," + Json.str(FormulaRenderer.formatNumber(got))
             + "," + Json.str(FormulaRenderer.formatNumber(want))
             // The size of the miss is the diagnosis: rounding-level means the
             // tolerance is tight, large means a wrong constant.
             + "," + Json.str(FormulaRenderer.formatNumber(off))
             + "," + Json.str(relativeError(off, want))
             + "," + Json.str(FormulaRenderer.formatNumber(delta))
             + "," + (ok ? 1 : 0) + "]";
    }

    /**
     * The miss as a share of the expected value, which is what says whether it
     * matters: 1e-9 off a billion is rounding, 1e-9 off 1e-9 is a wrong formula.
     */
    private static String relativeError(double off, double want) {
        if (want == 0 || off == 0) {
            return "";
        }
        double percent = off / Math.abs(want) * 100;
        if (percent < 0.001) {
            return " (<0.001%)";
        }
        return " (" + FormulaRenderer.formatNumber(round(percent, 3)) + "%)";
    }

    private static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
