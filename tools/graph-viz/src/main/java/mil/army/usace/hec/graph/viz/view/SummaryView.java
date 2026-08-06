package mil.army.usace.hec.graph.viz.view;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class SummaryView {

    private static final double SWEEP = 0.7;

    // Radius chosen so the circumference is exactly 100, making arcs percentages
    private static final double RADIUS = 15.9154943;

    private static final String LAYOUT = """
        <div class="sum">
          <div class="sum-top">
            <button type="button" class="donutbtn" aria-expanded="false"
                    title="Click for a closer look">{{donut}}</button>
            <div class="sum-figures">{{figures}}</div>
            <div class="donutkey">{{donutkey}}</div>
          </div>
          <h4>Every conversion slot</h4>
          <div class="sum-note">Both directions of every pair within a dimension,
            excluding a unit with itself.</div>
          <table class="sum-table">
            {{breakdown}}
          </table>
          <h4>Coverage by dimension</h4>
          <div class="sum-note">Alphabetical, like the matrices. The bar is the share of
            reachable conversions that a test exercises. Click a heading to re-sort.</div>
          <div class="foldwrap" data-visible="{{visible}}">
            <table class="sum-table dims sortable">
              <thead><tr><th>dimension</th><th>units</th><th>reachable</th><th>passed</th>
              <th>failed</th><th>untested</th><th>coverage</th><th></th></tr></thead>
              <tbody>
                {{dimensions}}
              </tbody>
            </table>
          </div>
          {{dimtoggle}}
          {{routes}}
          <div id="sumextra"></div>
          <h4>Worth a look</h4>
          <div class="sum-cards">
            {{cards}}
          </div>
        </div>
        """;

    private static final String FIGURE = """
        <div class="fig">
          <div class="fig-label">{{label}}</div>
          <div class="fig-value">{{value}}</div>
          <div class="fig-note">{{note}}</div>
        </div>
        """;

    private static final String BAR_ROW = """
        <tr data-state="{{state}}"><td><i class="sw {{cls}}"></i></td><td>{{label}}</td>
        <td class="n">{{count}}</td><td class="p">{{share}}</td>
        <td class="barcell"><span class="bar {{cls2}}" style="width:{{width}}%"></span></td></tr>
        """;

    private static final String DIM_ROW = """
        <tr><td class="name">{{name}}</td><td class="n">{{units}}</td>
        <td class="n">{{reachable}}</td><td class="n {{oktone}}">{{passed}}</td>
        <td class="n {{failtone}}">{{failed}}</td><td class="n">{{untested}}</td>
        <td class="p">{{coverage}}</td>
        <td class="barcell"><span class="bar {{tone}}" style="width:{{width}}%"></span>
        <span class="target" title="80% target"></span></td></tr>
        """;

    private static final String ROUTES = """
        <h4>{{title}}</h4>
        <div class="sum-note">Every hop multiplies in another constant, so long chains
          are where rounding error accumulates.</div>
        <table class="sum-table sortable">
          <thead><tr><th>route</th><th>conversions</th><th>share</th><th></th></tr></thead>
          <tbody>
            {{rows}}
          </tbody>
        </table>
        """;

    private static final String ROUTE_ROW = """
        <tr><td class="hopn">{{hops}}</td><td class="n">{{count}}</td>
        <td class="p">{{share}}</td>
        <td class="barcell"><span class="bar hops" style="width:{{width}}%"></span></td></tr>
        """;

    private static final String CARD = """
        <div class="sum-card {{tone}}">
          <div class="c-value">{{value}}</div>
          <div class="c-label">{{label}}</div>
          <div class="c-note">{{note}}</div>
        </div>
        """;

    private static final String ARC = """
        <circle class="seg {{cls}}" cx="21" cy="21" r="{{r}}" fill="none" stroke-width="5"
          style="--dash:{{share}} {{rest}};animation-delay:{{delay}}s;animation-duration:{{dur}}s"
          stroke-dasharray="{{share}} {{rest}}" stroke-dashoffset="{{offset}}"></circle>
        """;

    private SummaryView() {
    }

    public static String render(Stats stats, String routeTitle) {
        return Html.fill(LAYOUT)
            .raw("donut", donut(stats))
            .raw("donutkey", donutKey(stats))
            .raw("figures", figures(stats))
            .raw("breakdown", breakdown(stats))
            .put("visible", VISIBLE_DIMENSIONS)
            .raw("dimensions", dimensionRows(stats))
            .raw("dimtoggle", foldToggle(stats.groups().size()))
            .raw("routes", routes(stats, routeTitle))
            .raw("cards", cards(stats))
            .render();
    }

    /** How many rows of a long table are worth showing before folding. */
    private static final int VISIBLE_DIMENSIONS = 8;

    private static String coverageTone(Stats.Group group) {
        if (group.failed() > 0) {
            return "failed";
        }
        if (group.reachable() == 0) {
            return "untested";
        }
        if (group.coverage() >= 80) {
            return "passed";
        }
        return group.coverage() >= 40 ? "warn" : "low";
    }

    private static String dimensionRows(Stats stats) {
        var rows = new StringBuilder();
        for (Stats.Group group : stats.groups()) {
            rows.append(dimensionRow(group));
        }
        return rows.toString();
    }

    private static String foldToggle(int total) {
        if (total <= VISIBLE_DIMENSIONS) {
            return "";
        }
        return Html.fill("""
            <button type="button" class="foldbtn" aria-expanded="false"
                    data-more="Show all {{total}} dimensions" data-less="Show fewer"
                    >Show all {{total2}} dimensions</button>
            """).put("total", total).put("total2", total).render();
    }

    private static String figures(Stats stats) {
        return figure("coverage", Stats.percent(stats.coverage()),
                      stats.tested() + " of " + stats.reachable() + " reachable conversions tested")
             + figure("pass rate", Stats.percent(stats.passRate()),
                      stats.passed() + " of " + stats.tested() + " tested conversions pass")
             + figure("conversions", Integer.toString(stats.reachable()),
                      "reachable across " + stats.groups().size() + " dimensions, "
                      + stats.nodeCount() + " units");
    }

    private static String figure(String label, String value, String note) {
        return Html.fill(FIGURE).put("label", label).put("value", value).put("note", note).render();
    }

    private static String donut(Stats stats) {
        record Slice(String cls, int count) { }
        var slices = List.of(new Slice("passed", stats.passed()),
                             new Slice("failed", stats.failed()),
                             new Slice("untested", stats.untested()),
                             new Slice("missing", stats.missing()));

        var arcs = new StringBuilder();
        double offset = 25;                     // rotates the start to twelve o'clock
        double traveled = 0;                   // how far round the pen already is


        for (Slice slice : slices) {
            double share = stats.percentOfPairs(slice.count());
            if (share <= 0) {
                continue;
            }
            arcs.append(Html.fill(ARC)
                .put("cls", slice.cls())
                .put("r", RADIUS)
                .put("share", round(share))
                .put("rest", round(100 - share))
                .put("offset", round(offset))
                .put("delay", seconds(SWEEP * traveled / 100))
                .put("dur", seconds(SWEEP * share / 100))
                .render());
            offset -= share;
            traveled += share;
        }

        return Html.fill("""
            <svg class="donut" viewBox="0 0 42 42" role="img">
              <circle class="donut-hole" cx="21" cy="21" r="{{r}}" fill="none" stroke-width="5"></circle>
              {{arcs}}
              <text class="donut-mid" x="21" y="20.2">{{pct}}%</text>
              <text class="donut-sub" x="21" y="24.6">covered</text>
            </svg>
            """)
            .put("r", RADIUS)
            .raw("arcs", arcs.toString())
            .put("pct", Math.round(stats.coverage()))
            .render();
    }

    private static final String KEY_ROW = """
        <div class="dk-row">
          <span class="dk-label">{{label}}</span>
          <span class="dk-bar"><span class="bar {{cls}}" style="width:{{width}}%"></span></span>
          <span class="dk-count">{{count}}</span>
        </div>
        """;

    private static final String KEY_BLOCK = """
        <div class="dk-block">
          <div class="dk-title">{{title}}</div>
          <div class="dk-note">{{note}}</div>
          {{rows}}
        </div>
        """;

    private static String donutKey(Stats stats) {
        var gaps = stats.groups().stream()
            .filter(group -> group.untested() > 0)
            .sorted(Comparator.comparingInt(Stats.Group::untested).reversed())
            .limit(6)
            .toList();

        var broken = stats.groups().stream()
            .filter(group -> group.failed() > 0)
            .sorted(Comparator.comparingInt(Stats.Group::failed).reversed())
            .limit(6)
            .toList();

        return block("biggest gaps",
                     gaps.isEmpty() ? "Every reachable conversion is tested."
                                    : "Dimensions with the most untested conversions.",
                     gaps, Stats.Group::untested, "untested")
             + block("where the failures are",
                     broken.isEmpty() ? "Nothing is failing."
                                      : "Dimensions with a conversion that does not agree.",
                     broken, Stats.Group::failed, "failed");
    }

    private static String block(String title, String note, List<Stats.Group> groups,
                                java.util.function.ToIntFunction<Stats.Group> count, String cls) {
        int most = groups.stream().mapToInt(count).max().orElse(1);
        var rows = new StringBuilder();
        for (Stats.Group group : groups) {
            int value = count.applyAsInt(group);
            rows.append(Html.fill(KEY_ROW)
                .put("label", group.name())
                .put("cls", cls)
                .put("count", value)
                .put("width", round(most == 0 ? 0 : value * 100.0 / most))
                .render());
        }
        return Html.fill(KEY_BLOCK)
            .put("title", title)
            .put("note", note)
            .raw("rows", rows.toString())
            .render();
    }

    private static String breakdown(Stats stats) {
        return barRow("passed", "passed", stats.passed(), stats)
             + barRow("failed", "failed", stats.failed(), stats)
             + barRow("untested", "reachable, not tested", stats.untested(), stats)
             + barRow("missing", "no conversion exists", stats.missing(), stats)
             + Html.fill("""
                 <tr class="total"><td></td><td>total</td><td class="n">{{count}}</td>
                 <td class="p">100.00%</td><td></td></tr>
                 """).put("count", stats.pairs()).render();
    }

    private static String barRow(String cls, String label, int count, Stats stats) {
        double share = stats.percentOfPairs(count);
        return Html.fill(BAR_ROW)
            .put("cls", cls).put("cls2", cls)
            // "missing" means no conversion exists, so there is nothing to list
            .put("state", cls.equals("missing") ? "" : cls)
            .put("label", label)
            .put("count", count)
            .put("share", Stats.percent(share))
            .put("width", round(share))
            .render();
    }

    private static String dimensionRow(Stats.Group group) {
        return Html.fill(DIM_ROW)
            .put("name", group.name())
            .put("units", group.units())
            .put("reachable", group.reachable())
            .put("passed", group.passed())
            .put("oktone", group.passed() > 0 ? "ok" : "")
            .put("failed", group.failed())
            .put("failtone", group.failed() > 0 ? "bad" : "")
            .put("untested", group.untested())
            .put("coverage", Stats.percent(group.coverage()))
            .put("width", round(group.coverage()))
            .put("tone", coverageTone(group))
            .render();
    }

    private static String routes(Stats stats, String title) {
        Map<Integer, Integer> lengths = stats.routeLengths();
        if (lengths.isEmpty()) {
            return "";
        }
        int most = lengths.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int total = lengths.values().stream().mapToInt(Integer::intValue).sum();

        var rows = new StringBuilder();
        lengths.forEach((hops, count) -> rows.append(Html.fill(ROUTE_ROW)
            .put("hops", hops + (hops == 1 ? " hop" : " hops"))
            .put("count", count)
            .put("share", Stats.percent(total == 0 ? 0 : count * 100.0 / total))
            .put("width", round(most == 0 ? 0 : count * 100.0 / most))
            .render()));

        return Html.fill(ROUTES).put("title", title).raw("rows", rows.toString()).render();
    }

    private static String cards(Stats stats) {
        var noTests = stats.groups().stream()
            .filter(group -> group.tested() == 0).map(Stats.Group::name).toList();

        return card(stats.failed() > 0 ? "bad" : "ok", stats.failed(), "failing conversions",
                    stats.failed() == 0 ? "Nothing is failing." : list(stats.failures(), 8))
             + card(noTests.isEmpty() ? "ok" : "warn", noTests.size(),
                    "dimensions with no tests at all",
                    noTests.isEmpty() ? "Every dimension has at least one test." : list(noTests, 10))
             + card(stats.isolated().isEmpty() ? "ok" : "warn", stats.isolated().size(),
                    "units with no conversions",
                    stats.isolated().isEmpty() ? "Every unit connects to something."
                                               : list(stats.isolated(), 12))
             + card("ok", stats.singletonGroups(), "dimensions with a single unit",
                    "No matrix is drawn for these - there is nothing to convert between.");
    }

    private static String card(String tone, int value, String label, String note) {
        return Html.fill(CARD)
            .put("tone", tone).put("value", value).put("label", label).put("note", note).render();
    }

    private static String list(List<String> items, int limit) {
        if (items.size() <= limit) {
            return String.join(", ", items);
        }
        return String.join(", ", items.subList(0, limit)) + ", and " + (items.size() - limit) + " more";
    }

    private static String seconds(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static String round(double value) {
        return String.format("%.3f", value);
    }
}
