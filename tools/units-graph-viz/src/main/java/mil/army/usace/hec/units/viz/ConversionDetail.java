package mil.army.usace.hec.units.viz;

import java.util.List;
import java.util.Map;

import mil.army.usace.hec.graph.viz.formula.AffineForm;
import mil.army.usace.hec.graph.viz.formula.FormulaRenderer;
import mil.army.usace.hec.graph.viz.formula.PostfixEvaluator;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;
import mil.army.usace.hec.graph.viz.view.Html;
import net.hobbyscience.database.Conversion;

/**
 * Describes what one conversion does, as the panel shown when a cell is picked:
 * the pair in plain English, the net factor, the hops behind it, and the tests
 * that touched it.
 */
final class ConversionDetail {

    private static final String PANEL = """
        <div class="fx">
        <div class="fx-head">{{from}}<span class="arrow">→</span>{{to}}{{chip}}</div>
        {{names}}
        <div class="fx-eq">{{lhs}}<span class="eq">=</span>{{rhs}}</div>
        {{chain}}
        {{tests}}
        </div>
        """;

    private static final String CHAIN = """
        <div class="fx-where"><span class="kw">via</span>{{steps}}
        <span class="hopcount">{{hops}}</span></div>
        """;

    private static final String TESTS = """
        <div class="fx-tests">
        <div class="lbl">{{count}}</div>
        <table>{{rows}}</table>
        </div>
        """;

    private static final String TEST_ROW = """
        <tr class="{{tone}}">
        <td class="k">{{kind}}</td>
        <td class="in">{{input}} {{fromUnit}}</td>
        <td class="got">{{got}} {{toUnit}}</td>
        <td class="v">{{verdict}}</td>
        </tr>
        <tr class="want {{tone2}}"><td></td>
        <td colspan="3">expected {{want}} ± {{delta}}</td></tr>
        """;

    private ConversionDetail() {
    }

    /** Conversions chained together, counted from the hop chain. */
    static int hops(Conversion conversion) {
        String chain = conversion.getConversionChain();
        if (chain == null || chain.isBlank()) {
            return 1;
        }
        return Math.max(1, chain.split("->").length - 1);
    }

    static String of(Conversion conversion, String inversePostfix, EdgeStatus status,
                     Map<String, String> names, List<TestCase> direct, List<TestCase> roundTrip) {
        final String postfix;
        try {
            postfix = conversion.getMethod().getPostfix();
        } catch (Exception e) {
            return null;                    // unreadable formula, not worth failing the page
        }

        String from = conversion.getFrom().getAbbreviation();
        String to = conversion.getTo().getAbbreviation();

        return Html.fill(PANEL)
            .raw("from", UnitFormat.unit(from))
            .raw("to", UnitFormat.unit(to))
            .raw("chip", chip(status))
            .raw("names", names(names.get(from), names.get(to)))
            .raw("lhs", UnitFormat.unit(to))
            .raw("rhs", equation(postfix, from))
            .raw("chain", chain(conversion))
            .raw("tests", tests(postfix, inversePostfix, from, to, direct, roundTrip))
            .render();
    }

    /** Abbreviations are terse; the full names remove any doubt about which unit. */
    private static String names(String fromName, String toName) {
        if (fromName == null || toName == null) {
            return "";
        }
        return Html.tag("div").attr("class", "fx-names").text(fromName + " to " + toName).toString();
    }

    private static String chip(EdgeStatus status) {
        String label = status == EdgeStatus.PASSED ? "passed"
                     : status == EdgeStatus.FAILED ? "failed" : "not tested";
        String cls = status == EdgeStatus.PASSED ? "passed"
                   : status == EdgeStatus.FAILED ? "failed" : "untested";
        return Html.tag("span").attr("class", "chip " + cls).text(label).toString();
    }

    /** Derived conversions expose only postfix, so the affine probe runs against that. */
    private static String equation(String postfix, String from) {
        AffineForm form = FormulaRenderer.affineOf(x -> PostfixEvaluator.evaluate(postfix, x));

        if (form == null) {
            return warn("not a simple scale + offset");
        }
        if (form.m() == 0.0) {
            // A zero scale means the input is ignored, which is always a bug.
            return warn("ignores its input - always " + FormulaRenderer.formatNumber(form.b()));
        }

        var out = new StringBuilder(UnitFormat.unit(from));
        if (form.m() != 1.0) {
            out.append(op("×")).append(FormulaRenderer.formatNumber(form.m()));
        }
        if (form.b() != 0.0) {
            out.append(op(form.b() > 0 ? "+" : "−"))
               .append(FormulaRenderer.formatNumber(Math.abs(form.b())));
        }
        return out.toString();
    }

    private static String warn(String text) {
        return Html.tag("span").attr("class", "warn").text(text).toString();
    }

    private static String op(String symbol) {
        return Html.tag("span").attr("class", "op").text(symbol).toString();
    }

    /** When a derived conversion is wrong it is usually one hop in the middle. */
    private static String chain(Conversion conversion) {
        String chain = conversion.getConversionChain();
        if (chain == null || chain.isBlank()) {
            return "";
        }
        int hops = hops(conversion);
        var steps = new StringBuilder();
        String[] parts = chain.split("->");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                steps.append("<span class=\"arrow\">→</span>");
            }
            steps.append(UnitFormat.unit(parts[i].trim()));
        }
        return Html.fill(CHAIN)
            .raw("steps", steps.toString())
            .put("hops", hops + (hops == 1 ? " hop" : " hops"))
            .render();
    }

    /**
     * Rows written the other way round still exercise this conversion: the suite
     * converts and converts back. Reproducing that means running the opposite
     * conversion first and feeding its result in, exactly as the suite does.
     */
    private static String tests(String postfix, String inversePostfix, String from, String to,
                                List<TestCase> direct, List<TestCase> roundTrip) {
        boolean canRoundTrip = inversePostfix != null;
        int count = direct.size() + (canRoundTrip ? roundTrip.size() : 0);
        if (count == 0) {
            return "<div class=\"fx-tests\"><div class=\"lbl\">no test covers this pair</div></div>";
        }

        var rows = new StringBuilder();
        for (TestCase test : direct) {
            rows.append(testRow("direct", from, to, test.input(),
                                evaluate(postfix, test.input()), test.expected(), test.delta()));
        }
        if (canRoundTrip) {
            for (TestCase test : roundTrip) {
                Double there = evaluate(inversePostfix, test.input());
                Double back = there == null ? null : evaluate(postfix, there);
                rows.append(testRow("round trip", from, to, there, back,
                                    test.input(), test.inverseDelta()));
            }
        }

        return Html.fill(TESTS)
            .put("count", count + (count == 1 ? " test case" : " test cases"))
            .raw("rows", rows.toString())
            .render();
    }

    private static Double evaluate(String postfix, double input) {
        try {
            double value = PostfixEvaluator.evaluate(postfix, input);
            return Double.isFinite(value) ? value : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String testRow(String kind, String from, String to,
                                  Double input, Double got, double want, double delta) {
        if (input == null || got == null) {
            return Html.fill("""
                <tr class="bad"><td class="k">{{kind}}</td>
                <td colspan="3">could not evaluate</td></tr>
                """).put("kind", kind).render();
        }

        double off = Math.abs(got - want);
        boolean ok = off <= delta;

        return Html.fill(TEST_ROW)
            .put("tone", ok ? "ok" : "bad")
            .put("tone2", ok ? "ok" : "bad")
            .put("kind", kind)
            .put("input", FormulaRenderer.formatNumber(input))
            .raw("fromUnit", UnitFormat.unit(from))
            .put("got", FormulaRenderer.formatNumber(got))
            .raw("toUnit", UnitFormat.unit(to))
            // The size of the miss is the diagnosis: rounding-level means the
            // tolerance is tight, large means a wrong constant.
            .put("verdict", ok ? "✓" : "off by " + FormulaRenderer.formatNumber(off))
            .put("want", FormulaRenderer.formatNumber(want))
            .put("delta", FormulaRenderer.formatNumber(delta))
            .render();
    }
}
