package mil.army.usace.hec.units.viz;

import java.util.List;

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
 * what went wrong. That is what this fills in: the net factor as one readable
 * number, the chain of hops behind it, and every test case that touched it with
 * the value this conversion actually produces for each.
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

    static String of(Conversion conversion, EdgeStatus status, List<TestCase> forward,
                     List<TestCase> reverse) {
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
        out.append("<div class=\"fx-head\"><i>").append(Html.escape(from))
           .append("</i><span class=\"arrow\">→</span><i>").append(Html.escape(to))
           .append("</i>").append(statusChip(status)).append("</div>");

        // Derived conversions only expose postfix, so the affine probe has to run
        // against a postfix evaluator rather than the infix one.
        AffineForm form = FormulaRenderer.affineOf(x -> PostfixEvaluator.evaluate(postfix, x));

        out.append("<table class=\"fx-body\"><tr><td class=\"lhs\"><i>")
           .append(Html.escape(to)).append("</i></td><td class=\"eq\">=</td><td class=\"rhs\">");
        if (form == null) {
            out.append("<span class=\"warn\">not a simple scale + offset</span>");
        } else if (form.m() == 0.0) {
            out.append("<span class=\"warn\">ignores its input - always ")
               .append(FormulaRenderer.formatNumber(form.b())).append("</span>");
        } else {
            out.append("<i>").append(Html.escape(from)).append("</i>");
            if (form.m() != 1.0) {
                out.append(" × ").append(FormulaRenderer.formatNumber(form.m()));
            }
            if (form.b() != 0.0) {
                out.append(form.b() > 0 ? " + " : " − ")
                   .append(FormulaRenderer.formatNumber(Math.abs(form.b())));
            }
        }
        out.append("</td></tr></table>");

        // The hop chain is the most useful field for debugging: when a conversion
        // is wrong it is usually one hop in the middle, and this points at it.
        String chain = conversion.getConversionChain();
        if (chain != null && !chain.isBlank()) {
            int hops = hops(conversion);
            out.append("<div class=\"fx-where\"><span class=\"kw\">via</span>")
               .append(Html.escape(chain))
               .append("<span class=\"hopcount\">").append(hops)
               .append(hops == 1 ? " hop" : " hops").append("</span></div>");
        }

        out.append("<div class=\"fx-raw\">").append(Html.escape(postfix)).append("</div>");
        out.append(tests(postfix, from, to, forward, reverse));
        return out.append("</div>").toString();
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
     * Every test case that touches this pair, with what the conversion actually
     * produces for each.
     *
     * Rows written the other way round still exercise this conversion, because
     * the test converts and then converts back. For those the check runs in
     * reverse: feed in the row's expected value and the answer should be the
     * row's input.
     */
    private static String tests(String postfix, String from, String to,
                                List<TestCase> forward, List<TestCase> reverse) {
        int count = forward.size() + reverse.size();
        if (count == 0) {
            return "<div class=\"fx-tests\"><div class=\"lbl\">no test covers this pair</div></div>";
        }

        var out = new StringBuilder("<div class=\"fx-tests\"><div class=\"lbl\">")
            .append(count).append(count == 1 ? " test case" : " test cases")
            .append("</div><table>");

        for (TestCase test : forward) {
            row(out, postfix, from, to, test.input(), test.expected(), test.delta(), false);
        }
        for (TestCase test : reverse) {
            // The row reads to -> from, so this conversion is the return leg.
            row(out, postfix, from, to, test.expected(), test.input(), test.delta(), true);
        }
        return out.append("</table></div>").toString();
    }

    private static void row(StringBuilder out, String postfix, String from, String to,
                            double input, double want, double delta, boolean inverse) {
        double got;
        try {
            got = PostfixEvaluator.evaluate(postfix, input);
        } catch (RuntimeException e) {
            out.append("<tr class=\"bad\"><td colspan=\"4\">could not evaluate</td></tr>");
            return;
        }

        double off = Math.abs(got - want);
        boolean ok = Double.isFinite(got) && off <= delta;

        out.append("<tr class=\"").append(ok ? "ok" : "bad").append("\">")
           .append("<td class=\"in\">").append(FormulaRenderer.formatNumber(input))
           .append(" <span class=\"u\">").append(Html.escape(from)).append("</span></td>")
           .append("<td class=\"got\">").append(FormulaRenderer.formatNumber(got))
           .append(" <span class=\"u\">").append(Html.escape(to)).append("</span></td>")
           .append("<td class=\"exp\">want ").append(FormulaRenderer.formatNumber(want))
           .append("</td><td class=\"v\">");

        if (ok) {
            out.append("✓");
        } else {
            // The size of the miss is the diagnosis: a rounding-level gap means a
            // tolerance that is too tight, a large one means a wrong constant.
            out.append("off by ").append(FormulaRenderer.formatNumber(off));
        }
        out.append("</td></tr>");

        if (inverse) {
            out.append("<tr class=\"note\"><td colspan=\"4\">↑ return leg of the ")
               .append(Html.escape(to)).append(" → ").append(Html.escape(from))
               .append(" test</td></tr>");
        }
    }
}
