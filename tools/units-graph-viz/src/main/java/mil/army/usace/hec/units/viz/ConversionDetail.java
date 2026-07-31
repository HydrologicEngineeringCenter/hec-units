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
 * Describes what a single conversion does, as an HTML fragment.
 *
 * A coloured square tells you a conversion passed or was never tested. It does
 * not tell you whether the number it produces is right, nor - when it is red -
 * what went wrong. That is what this fills in: the pair in plain English, the
 * net factor as one readable number, the chain of hops behind it, and every test
 * case that touched it with the value this conversion actually produces.
 */
final class ConversionDetail {

    private ConversionDetail() {
    }

    /** Number of conversions chained together, from the hop chain string. */
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
            // A formula we cannot read is not worth failing the whole page over.
            return null;
        }

        String from = conversion.getFrom().getAbbreviation();
        String to = conversion.getTo().getAbbreviation();

        var out = new StringBuilder("<div class=\"fx\">");

        out.append("<div class=\"fx-head\">")
           .append(UnitFormat.unit(from))
           .append("<span class=\"arrow\">→</span>")
           .append(UnitFormat.unit(to))
           .append(statusChip(status))
           .append("</div>");

        // The abbreviations are terse by necessity; the full names remove any
        // doubt about which unit a two-letter symbol refers to.
        String fromName = names.get(from);
        String toName = names.get(to);
        if (fromName != null && toName != null) {
            out.append("<div class=\"fx-names\">").append(Html.escape(fromName))
               .append(" to ").append(Html.escape(toName)).append("</div>");
        }

        // Derived conversions only expose postfix, so the affine probe has to run
        // against a postfix evaluator rather than the infix one.
        AffineForm form = FormulaRenderer.affineOf(x -> PostfixEvaluator.evaluate(postfix, x));

        out.append("<div class=\"fx-eq\">").append(UnitFormat.unit(to)).append("<span class=\"eq\">=</span>");
        if (form == null) {
            out.append("<span class=\"warn\">not a simple scale + offset</span>");
        } else if (form.m() == 0.0) {
            out.append("<span class=\"warn\">ignores its input - always ")
               .append(FormulaRenderer.formatNumber(form.b())).append("</span>");
        } else {
            out.append(UnitFormat.unit(from));
            if (form.m() != 1.0) {
                out.append("<span class=\"op\">×</span>")
                   .append(FormulaRenderer.formatNumber(form.m()));
            }
            if (form.b() != 0.0) {
                out.append("<span class=\"op\">").append(form.b() > 0 ? "+" : "−").append("</span>")
                   .append(FormulaRenderer.formatNumber(Math.abs(form.b())));
            }
        }
        out.append("</div>");

        // The hop chain is the most useful field for debugging: when a conversion
        // is wrong it is usually one hop in the middle, and this points at it.
        String chain = conversion.getConversionChain();
        if (chain != null && !chain.isBlank()) {
            int hops = hops(conversion);
            out.append("<div class=\"fx-where\"><span class=\"kw\">via</span>")
               .append(chainSymbols(chain))
               .append("<span class=\"hopcount\">").append(hops)
               .append(hops == 1 ? " hop" : " hops").append("</span></div>");
        }

        out.append(tests(postfix, inversePostfix, from, to, direct, roundTrip));
        return out.append("</div>").toString();
    }

    /** "ac-ft -> ft3 -> m3" with each unit typeset and a real arrow. */
    private static String chainSymbols(String chain) {
        String[] steps = chain.split("->");
        var out = new StringBuilder();
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) {
                out.append("<span class=\"arrow\">→</span>");
            }
            out.append(UnitFormat.unit(steps[i].trim()));
        }
        return out.toString();
    }

    private static String statusChip(EdgeStatus status) {
        if (status == EdgeStatus.PASSED) {
            return "<span class=\"chip passed\">passed</span>";
        }
        if (status == EdgeStatus.FAILED) {
            return "<span class=\"chip failed\">failed</span>";
        }
        return "<span class=\"chip untested\">not tested</span>";
    }

    /**
     * Every test case that touches this pair, with what the conversion produces.
     *
     * Rows written the other way round still exercise this conversion, because
     * the suite converts and then converts back. Reproducing that faithfully
     * means running the opposite conversion first and feeding its result in -
     * exactly as the suite does. Substituting the row's ideal value instead
     * would break the round trip and report a failure the suite never sees.
     */
    private static String tests(String postfix, String inversePostfix, String from, String to,
                                List<TestCase> direct, List<TestCase> roundTrip) {
        boolean canRoundTrip = inversePostfix != null;
        int count = direct.size() + (canRoundTrip ? roundTrip.size() : 0);
        if (count == 0) {
            return "<div class=\"fx-tests\"><div class=\"lbl\">no test covers this pair</div></div>";
        }

        var out = new StringBuilder("<div class=\"fx-tests\"><div class=\"lbl\">")
            .append(count).append(count == 1 ? " test case" : " test cases")
            .append("</div><table>");

        for (TestCase test : direct) {
            row(out, "direct", from, to, test.input(),
                evaluate(postfix, test.input()), test.expected(), test.delta());
        }

        if (canRoundTrip) {
            for (TestCase test : roundTrip) {
                // The row reads to -> from. The suite converts that way first, then
                // back through this conversion, and checks it lands where it started.
                Double there = evaluate(inversePostfix, test.input());
                Double back = there == null ? null : evaluate(postfix, there);
                row(out, "round trip", from, to, there, back, test.input(), test.inverseDelta());
            }
        }

        return out.append("</table></div>").toString();
    }

    private static Double evaluate(String postfix, double input) {
        try {
            double value = PostfixEvaluator.evaluate(postfix, input);
            return Double.isFinite(value) ? value : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void row(StringBuilder out, String kind, String from, String to,
                            Double input, Double got, double want, double delta) {
        if (input == null || got == null) {
            out.append("<tr class=\"bad\"><td class=\"k\">").append(kind)
               .append("</td><td colspan=\"3\">could not evaluate</td></tr>");
            return;
        }

        double off = Math.abs(got - want);
        boolean ok = off <= delta;

        out.append("<tr class=\"").append(ok ? "ok" : "bad").append("\">")
           .append("<td class=\"k\">").append(kind).append("</td>")
           .append("<td class=\"in\">").append(FormulaRenderer.formatNumber(input))
           .append(" ").append(UnitFormat.unit(from)).append("</td>")
           .append("<td class=\"got\">").append(FormulaRenderer.formatNumber(got))
           .append(" ").append(UnitFormat.unit(to)).append("</td>")
           .append("<td class=\"v\">");

        if (ok) {
            out.append("✓");
        } else {
            // The size of the miss is the diagnosis: a rounding-level gap means a
            // tolerance that is too tight, a large one means a wrong constant.
            out.append("off by ").append(FormulaRenderer.formatNumber(off));
        }
        out.append("</td></tr>")
           .append("<tr class=\"want ").append(ok ? "ok" : "bad").append("\"><td></td>")
           .append("<td colspan=\"3\">expected ").append(FormulaRenderer.formatNumber(want))
           .append(" ± ").append(FormulaRenderer.formatNumber(delta)).append("</td></tr>");
    }
}
