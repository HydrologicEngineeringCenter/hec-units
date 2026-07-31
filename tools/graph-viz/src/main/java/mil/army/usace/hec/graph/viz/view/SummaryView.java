package mil.army.usace.hec.graph.viz.view;

import java.util.List;
import java.util.Map;

/**
 * A whole-project read-out: how much of the graph is covered, where the gaps
 * are, and what needs attention.
 *
 * The matrices answer "what is the state of this one conversion". This answers
 * "what is the state of the project", which is the question you actually have
 * when you open the page cold.
 */
public final class SummaryView {

    /** Chart geometry: r chosen so the circumference is exactly 100 units. */
    private static final double RADIUS = 15.9154943;

    private SummaryView() {
    }

    public static String render(Stats stats, String routeLengthTitle) {
        var out = new StringBuilder("<div class=\"sum\">");
        headline(out, stats);
        breakdown(out, stats);
        dimensions(out, stats);
        routeLengths(out, stats, routeLengthTitle);
        attention(out, stats);
        return out.append("</div>").toString();
    }

    /** The three numbers worth reading first. */
    private static void headline(StringBuilder out, Stats stats) {
        out.append("<div class=\"sum-top\">");
        out.append(donut(stats));
        out.append("<div class=\"sum-figures\">");
        figure(out, "coverage", Stats.percent(stats.coverage()),
               stats.tested() + " of " + stats.reachable() + " reachable conversions tested");
        figure(out, "pass rate", Stats.percent(stats.passRate()),
               stats.passed() + " of " + stats.tested() + " tested conversions pass");
        figure(out, "conversions", Integer.toString(stats.reachable()),
               "reachable across " + stats.groups().size() + " dimensions, "
               + stats.nodeCount() + " units");
        out.append("</div></div>");
    }

    private static void figure(StringBuilder out, String label, String value, String note) {
        out.append("<div class=\"fig\"><div class=\"fig-label\">").append(Html.escape(label))
           .append("</div><div class=\"fig-value\">").append(Html.escape(value))
           .append("</div><div class=\"fig-note\">").append(Html.escape(note))
           .append("</div></div>");
    }

    /**
     * A donut drawn as SVG arcs.
     *
     * With the radius set so the circumference is 100, each slice's
     * stroke-dasharray is simply "percentage, remainder" - no trigonometry and
     * no charting library. The -25 offset rotates the start to twelve o'clock.
     */
    private static String donut(Stats stats) {
        record Slice(String cls, int count) { }
        var slices = List.of(new Slice("passed", stats.passed()),
                             new Slice("failed", stats.failed()),
                             new Slice("untested", stats.untested()),
                             new Slice("missing", stats.missing()));

        var out = new StringBuilder("<svg class=\"donut\" viewBox=\"0 0 42 42\" role=\"img\">");
        out.append("<circle class=\"donut-hole\" cx=\"21\" cy=\"21\" r=\"").append(RADIUS)
           .append("\" fill=\"none\" stroke-width=\"5\"></circle>");

        double offset = 25;
        for (Slice slice : slices) {
            double share = stats.percentOfPairs(slice.count());
            if (share <= 0) {
                continue;
            }
            out.append("<circle class=\"seg ").append(slice.cls())
               .append("\" cx=\"21\" cy=\"21\" r=\"").append(RADIUS)
               .append("\" fill=\"none\" stroke-width=\"5\" stroke-dasharray=\"")
               .append(round(share)).append(' ').append(round(100 - share))
               .append("\" stroke-dashoffset=\"").append(round(offset)).append("\"></circle>");
            offset -= share;
        }

        out.append("<text class=\"donut-mid\" x=\"21\" y=\"20.2\">")
           .append(Math.round(stats.coverage())).append("%</text>");
        out.append("<text class=\"donut-sub\" x=\"21\" y=\"24.6\">covered</text>");
        return out.append("</svg>").toString();
    }

    /** Every category with a raw count, a share, and a proportional bar. */
    private static void breakdown(StringBuilder out, Stats stats) {
        out.append("<h4>Every conversion slot</h4>");
        out.append("<div class=\"sum-note\">Counting both directions of every pair within a "
                 + "dimension, excluding a unit with itself.</div>");
        out.append("<table class=\"sum-table\">");
        bar(out, "passed", "passed", stats.passed(), stats);
        bar(out, "failed", "failed", stats.failed(), stats);
        bar(out, "untested", "reachable, not tested", stats.untested(), stats);
        bar(out, "missing", "no conversion exists", stats.missing(), stats);
        out.append("<tr class=\"total\"><td></td><td>total</td><td class=\"n\">")
           .append(stats.pairs()).append("</td><td class=\"p\">100.00%</td><td></td></tr>");
        out.append("</table>");
    }

    private static void bar(StringBuilder out, String cls, String label, int count, Stats stats) {
        double share = stats.percentOfPairs(count);
        out.append("<tr><td><i class=\"sw ").append(cls).append("\"></i></td><td>")
           .append(Html.escape(label)).append("</td><td class=\"n\">").append(count)
           .append("</td><td class=\"p\">").append(Stats.percent(share))
           .append("</td><td class=\"barcell\"><span class=\"bar ").append(cls)
           .append("\" style=\"width:").append(round(share)).append("%\"></span></td></tr>");
    }

    /** Worst-covered dimension first, since that is the actionable order. */
    private static void dimensions(StringBuilder out, Stats stats) {
        out.append("<h4>Coverage by dimension</h4>");
        out.append("<div class=\"sum-note\">Least covered first. The bar is the share of "
                 + "reachable conversions that a test exercises.</div>");
        out.append("<table class=\"sum-table dims\"><thead><tr><th>dimension</th><th>units</th>"
                 + "<th>reachable</th><th>passed</th><th>failed</th><th>untested</th>"
                 + "<th>coverage</th><th></th></tr></thead><tbody>");

        for (Stats.Group group : stats.groupsByCoverage()) {
            out.append("<tr><td class=\"name\">").append(Html.escape(group.name()))
               .append("</td><td class=\"n\">").append(group.units())
               .append("</td><td class=\"n\">").append(group.reachable())
               .append("</td><td class=\"n ok\">").append(group.passed())
               .append("</td><td class=\"n ").append(group.failed() > 0 ? "bad" : "")
               .append("\">").append(group.failed())
               .append("</td><td class=\"n\">").append(group.untested())
               .append("</td><td class=\"p\">").append(Stats.percent(group.coverage()))
               .append("</td><td class=\"barcell\"><span class=\"bar passed\" style=\"width:")
               .append(round(group.coverage())).append("%\"></span></td></tr>");
        }
        out.append("</tbody></table>");
    }

    /**
     * How many hops each conversion takes.
     *
     * Worth surfacing because every extra hop multiplies another constant in,
     * so a long chain is where rounding error accumulates and where a single
     * bad constant does the most damage.
     */
    private static void routeLengths(StringBuilder out, Stats stats, String title) {
        Map<Integer, Integer> lengths = stats.routeLengths();
        if (lengths.isEmpty()) {
            return;
        }
        int most = lengths.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int total = lengths.values().stream().mapToInt(Integer::intValue).sum();

        out.append("<h4>").append(Html.escape(title)).append("</h4>");
        out.append("<div class=\"sum-note\">Every hop multiplies in another constant, so the "
                 + "long chains are where rounding error accumulates.</div>");
        out.append("<table class=\"sum-table\">");
        lengths.forEach((hops, count) ->
            out.append("<tr><td class=\"hopn\">").append(hops)
               .append(hops == 1 ? " hop" : " hops").append("</td><td class=\"n\">").append(count)
               .append("</td><td class=\"p\">")
               .append(Stats.percent(total == 0 ? 0 : count * 100.0 / total))
               .append("</td><td class=\"barcell\"><span class=\"bar hops\" style=\"width:")
               .append(round(most == 0 ? 0 : count * 100.0 / most)).append("%\"></span></td></tr>"));
        out.append("</table>");
    }

    /** Things a person should probably go and look at. */
    private static void attention(StringBuilder out, Stats stats) {
        out.append("<h4>Worth a look</h4><div class=\"sum-cards\">");

        card(out, stats.failed() > 0 ? "bad" : "ok", Integer.toString(stats.failed()),
             "failing conversions",
             stats.failed() == 0 ? "Nothing is failing."
                                 : String.join(", ", first(stats.failures(), 8)));

        var zero = stats.groups().stream().filter(g -> g.tested() == 0).map(Stats.Group::name).toList();
        card(out, zero.isEmpty() ? "ok" : "warn", Integer.toString(zero.size()),
             "dimensions with no tests at all",
             zero.isEmpty() ? "Every dimension has at least one test."
                            : String.join(", ", first(zero, 10)));

        card(out, stats.isolated().isEmpty() ? "ok" : "warn",
             Integer.toString(stats.isolated().size()), "units with no conversions",
             stats.isolated().isEmpty() ? "Every unit connects to something."
                                        : String.join(", ", first(stats.isolated(), 12)));

        card(out, "ok", Integer.toString(stats.singletonGroups()),
             "dimensions with a single unit",
             "No matrix is drawn for these - there is nothing to convert between.");

        out.append("</div>");
    }

    private static void card(StringBuilder out, String tone, String value, String label, String note) {
        out.append("<div class=\"sum-card ").append(tone).append("\"><div class=\"c-value\">")
           .append(Html.escape(value)).append("</div><div class=\"c-label\">")
           .append(Html.escape(label)).append("</div><div class=\"c-note\">")
           .append(Html.escape(note)).append("</div></div>");
    }

    private static List<String> first(List<String> items, int limit) {
        if (items.size() <= limit) {
            return items;
        }
        var shown = new java.util.ArrayList<>(items.subList(0, limit));
        shown.add("and " + (items.size() - limit) + " more");
        return shown;
    }

    private static String round(double value) {
        return String.format("%.3f", value);
    }
}
