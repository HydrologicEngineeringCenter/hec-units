package mil.army.usace.hec.units.viz;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mil.army.usace.hec.graph.viz.formula.AffineForm;
import mil.army.usace.hec.graph.viz.formula.FormulaRenderer;
import mil.army.usace.hec.graph.viz.formula.Substitution;
import mil.army.usace.hec.graph.viz.view.Html;

/**
 * The panel for one direct conversion: the formula as authored
 */
final class SeedFormula {

    private static final Pattern TOKEN = Pattern.compile(
        "(?<num>\\d+\\.?\\d*(?:[eE][-+]?\\d+)?|\\.\\d+)"
      + "|(?<name>[A-Za-z_][A-Za-z0-9_/-]*)"
      + "|(?<op>[-+*/^()])"
      + "|(?<ws>\\s+)");

    // Flat for the same reason as ConversionDetail's: this ends up escaped
    // inside a data-detail attribute, not in the page's own structure.
    private static final String PANEL = """
        <div class="fx">
        <div class="fx-head">{{from}}<span class="arrow">→</span>{{to}}
        <span class="chip kind">{{kind}}</span></div>
        {{names}}
        <div class="fx-eq">{{lhs}}<span class="eq">=</span>{{symbolic}}</div>
        {{resolved}}
        {{where}}
        {{note}}
        <div class="fx-raw">{{raw}}</div>
        </div>
        """;

    private SeedFormula() {
    }

    static String render(String method, String from, String to, Map<String, String> constants,
                         Map<String, String> names) {
        String kind = method.contains(":") ? method.substring(0, method.indexOf(':')).trim()
                                           : "function";
        String symbolic = FormulaRenderer.symbolic(method);
        Substitution substitution = FormulaRenderer.substitute(symbolic, constants);
        AffineForm form = FormulaRenderer.affineOf(substitution.expression());

        String fromName = names.get(from);
        String toName = names.get(to);

        return Html.fill(PANEL)
            .raw("from", UnitFormat.unit(from))
            .raw("to", UnitFormat.unit(to))
            .put("kind", kind)
            .raw("names", fromName == null || toName == null ? ""
                 : Html.tag("div").attr("class", "fx-names")
                       .text(fromName + " to " + toName).toString())
            .raw("lhs", UnitFormat.unit(to))
            .raw("symbolic", pretty(symbolic, from))
            .raw("resolved", resolved(form, from))
            .raw("where", where(substitution, constants))
            .raw("note", form != null ? ""
                 : "<div class=\"fx-note\">not a simple scale + offset</div>")
            .put("raw", method)
            .render();
    }

    // "= ft3 × 0.028316846592" - the number a person can actually check
    private static String resolved(AffineForm form, String from) {
        if (form == null || (form.m() == 1.0 && form.b() == 0.0)) {
            return "";
        }
        var out = new StringBuilder("<div class=\"fx-eq cont\"><span class=\"eq\">=</span>")
            .append(UnitFormat.unit(from));
        if (form.m() != 1.0) {
            out.append("<span class=\"op\">×</span>").append(FormulaRenderer.formatNumber(form.m()));
        }
        if (form.b() != 0.0) {
            out.append("<span class=\"op\">").append(form.b() > 0 ? "+" : "−").append("</span>")
               .append(FormulaRenderer.formatNumber(Math.abs(form.b())));
        }
        return out.append("</div>").toString();
    }

    // "where m_per_ft = 0.3048" - the audit trail behind the number above
    private static String where(Substitution substitution, Map<String, String> constants) {
        if (substitution.used().isEmpty()) {
            return "";
        }
        var items = new StringBuilder();
        for (String name : substitution.used()) {
            if (items.length() > 0) {
                items.append("<span class=\"sep\">,&nbsp;</span>");
            }
            String value = constants.get(name);
            AffineForm parsed = FormulaRenderer.affineOf(value);
            items.append("<i>").append(Html.escape(name)).append("</i> = ")
                 .append(parsed != null && parsed.m() == 0.0
                         ? FormulaRenderer.formatNumber(parsed.b())
                         : Html.escape(value));
        }
        return "<div class=\"fx-where\"><span class=\"kw\">where</span>" + items + "</div>";
    }

    static String pretty(String expr, String variable) {
        var out = new StringBuilder();
        boolean pendingPower = false;

        Matcher matcher = TOKEN.matcher(expr);
        while (matcher.find()) {
            String token = matcher.group();
            if (matcher.group("ws") != null) {
                continue;
            }
            if (pendingPower) {
                pendingPower = false;
                if (matcher.group("num") != null) {
                    out.append("<sup>").append(Html.escape(token)).append("</sup>");
                    continue;
                }
                out.append('^'); 
            }
            if (matcher.group("op") != null) {
                switch (token) {
                    case "^" -> pendingPower = true;
                    case "*" -> out.append("<span class=\"op\">×</span>");
                    case "/" -> out.append("<span class=\"op\">∕</span>");
                    case "+" -> out.append("<span class=\"op\">+</span>");
                    case "-" -> out.append("<span class=\"op\">−</span>");
                    default -> out.append(Html.escape(token));
                }
            } else if (matcher.group("num") != null) {
                out.append(Html.escape(token));
            } else if (token.equals("i")) {
                out.append(UnitFormat.unit(variable));
            } else {
                out.append("<i>").append(Html.escape(token)).append("</i>");
            }
        }
        return out.toString();
    }
}
