package mil.army.usace.hec.units.viz;

import mil.army.usace.hec.graph.viz.formula.AffineForm;
import mil.army.usace.hec.graph.viz.formula.FormulaRenderer;
import mil.army.usace.hec.graph.viz.formula.PostfixEvaluator;
import mil.army.usace.hec.graph.viz.view.Html;
import net.hobbyscience.database.Conversion;

/**
 * Describes what a single conversion actually does, as an HTML fragment.
 *
 * A coloured square tells you a conversion passed or was never tested. It does
 * not tell you whether the number it produces is right - and that is the whole
 * point of the tool. This is what turns a cell into something checkable: the
 * net factor as a single readable number, and the chain of hops used to get it.
 */
final class ConversionDetail {

    private ConversionDetail() {
    }

    static String of(Conversion conversion) {
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
           .append("</i></div>");

        // Derived conversions only expose postfix, so the affine probe has to run
        // against a postfix evaluator rather than the infix one.
        AffineForm form = FormulaRenderer.affineOf(x -> PostfixEvaluator.evaluate(postfix, x));

        out.append("<table class=\"fx-body\"><tr><td class=\"lhs\"><i>")
           .append(Html.escape(to)).append("</i></td><td class=\"eq\">=</td><td class=\"rhs\">");
        if (form == null) {
            out.append("<span class=\"warn\">not a simple scale + offset</span>");
        } else if (form.m() == 0.0) {
            // A zero scale means the formula never uses its input, so every value
            // converts to the same number. Always a bug, and worth saying so
            // rather than printing "x 0 + 1" as though it were a real equation.
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

        // The hop chain is the single most useful field for debugging: when a
        // conversion is wrong, it is usually one hop in the middle that is wrong,
        // and this is what points at it.
        String chain = conversion.getConversionChain();
        if (chain != null && !chain.isBlank()) {
            out.append("<div class=\"fx-where\"><span class=\"kw\">via</span>")
               .append(Html.escape(chain)).append("</div>");
        }

        out.append("<div class=\"fx-raw\">").append(Html.escape(postfix)).append("</div>");
        return out.append("</div>").toString();
    }
}
