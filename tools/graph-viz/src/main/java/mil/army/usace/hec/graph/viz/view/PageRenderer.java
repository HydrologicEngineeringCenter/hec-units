package mil.army.usace.hec.graph.viz.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Wraps view fragments in a complete, self-contained HTML document
public final class PageRenderer {

    private static final String PAGE = """
        <!doctype html>
        <html lang="en">
          <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>{{title}}</title>
            <style>
        {{css}}
            </style>
          </head>
          <body>
            <div class="pagehead">
              <div>
                <h1>{{title2}}</h1>
                <p class="lede">{{lede}}</p>
              </div>
              {{summaryButton}}
            </div>
            {{tabbar}}
            <div class="tabpane active" id="tab-coverage">
              {{legend}}
              {{body}}
            </div>
            {{seedpane}}
            {{overlay}}
            {{summary}}
            {{data}}
            <script>
        {{cyto}}
            </script>
            <script>
        {{js}}
            </script>
          </body>
        </html>
        """;

    private static final String OVERLAY = """
        <div id="overlay">
          <div id="obar">
            <h3 id="otitle"></h3>
            <span id="oaxis">row → column</span>
            <span id="otally" class="tally"></span>
            <button id="oreset" type="button">Reset view</button>
            <button id="oclose" type="button">Close</button>
          </div>
          {{legend}}
          {{seedkey}}
          <div id="ostagewrap">
            <div id="ostage"></div>
            <aside id="opanel"><div id="odetail"></div></aside>
          </div>
        </div>
        """;

    private static final String SUMMARY = """
        <div id="summary">
          <div id="sbar"><h3>Test suite summary</h3><button id="sclose" type="button">Close</button></div>
          <div id="sbody">{{content}}</div>
        </div>
        """;

    private static final String KEY = """
        <span class="key">
          <span class="k-top"><i class="sw {{cls}}"></i>{{label}}</span>{{counts}}
        </span>
        """;

    private PageRenderer() {
    }

    public static String render(String title, Stats stats, String body, String seedBody,
                                String data) throws IOException {
        boolean tabbed = !seedBody.isEmpty();
        return Html.fill(PAGE)
            .put("title", title)
            .put("title2", title)
            .put("lede", tabbed
                 ? "Coverage shows every conversion the algorithm can produce, one matrix per "
                 + "dimension - each row converts into the columns. Conversion graphs show the "
                 + "direct conversions those are all derived from - the ones written by hand, "
                 + "one step each. Click any card to enlarge it."
                 : "Every conversion the algorithm can produce, one matrix per dimension. "
                 + "Each row converts into the columns. Click a card to enlarge it, then click "
                 + "any cell for its equation and test results.")
            .raw("css", resource("/viz.css"))
            .raw("js", pageScript())
            .raw("cyto", tabbed ? resource("/cytoscape.min.js") : "")
            .raw("summaryButton", stats == null ? ""
                 : "<button id=\"sumopen\" type=\"button\">Summary</button>")
            .raw("tabbar", tabbed ? TABBAR : "")
            .raw("legend", legend("legend", stats))
            .raw("body", body)
            .raw("seedpane", tabbed
                 ? Html.fill(SEEDPANE).raw("legend", seedLegend()).raw("body", seedBody).render()
                 : "")
            .raw("overlay", Html.fill(OVERLAY)
                 .raw("legend", legend("legend olegend mkey", null))
                 .raw("seedkey", tabbed ? seedKey() : "")
                 .render())
            .raw("summary", summary(stats))
            .raw("data", data.isEmpty() ? "" : "<script>\n" + data + "</script>")
            .render();
    }

    private static final String TABBAR = """
        <div class="tabs" role="tablist">
          <button class="tab active" data-pane="tab-coverage" type="button">Coverage</button>
          <button class="tab" data-pane="tab-seed" type="button">Conversion graphs</button>
        </div>
        """;

    private static final String SEEDPANE = """
        <div class="tabpane" id="tab-seed">
          {{legend}}
          {{body}}
        </div>
        """;

    private static final String LEGEND = """
        <div class="{{class}}">
          {{keys}}
          <span class="hint">numbers in cells are hops in the chosen route</span>
        </div>
        """;

    private static final String TOTAL = """
        <span class="key total">
          <span class="k-top">total</span>
          <span class="k-num">{{count}}</span>
        </span>
        """;

    // The graph tab's own key: node systems and the two stroke kinds
    private static String seedLegend() {
        return """
            <div class="legend seedlegend">
              <span><i class="sw t-si"></i>SI</span>
              <span><i class="sw t-english"></i>English</span>
              <span><i class="sw t-null"></i>system-agnostic</span>
              <span><i class="ln"></i><code>linear:</code> scale + offset</span>
              <span><i class="ln dash"></i><code>function:</code> arbitrary expression</span>
              <span class="hint">a bowed pair is two different conversions for the same two units</span>
            </div>
            """;
    }

    // The same key on the dark backdrop, shown only while a graph is enlarged
    private static String seedKey() {
        return """
            <div class="legend olegend skey">
              <span><i class="sw t-si"></i>SI</span>
              <span><i class="sw t-english"></i>English</span>
              <span><i class="sw t-null"></i>system-agnostic</span>
              <span><i class="ln"></i>linear</span>
              <span><i class="ln dash"></i>function</span>
              <span class="hint">click two units for routes · click an edge for its formula</span>
            </div>
            """;
    }

    private static String legend(String className, Stats stats) {
        return Html.fill(LEGEND)
            .put("class", className)
            .raw("keys", key(stats, "passed", "passed")
                       + key(stats, "failed", "failed")
                       + key(stats, "untested", "reachable, not tested")
                       + key(stats, "missing", "no conversion")
                       + total(stats))
            .render();
    }

    private static String key(Stats stats, String cls, String label) {
        return Html.fill(KEY)
            .put("cls", cls)
            .put("label", label)
            .raw("counts", stats == null ? "" : counts(stats, cls))
            .render();
    }

    // The denominator the percentages above are shares of
    private static String total(Stats stats) {
        if (stats == null) {
            return "";
        }
        return Html.fill(TOTAL).put("count", stats.pairs()).render();
    }

    private static String counts(Stats stats, String cls) {
        int count = switch (cls) {
            case "passed" -> stats.passed();
            case "failed" -> stats.failed();
            case "untested" -> stats.untested();
            default -> stats.missing();
        };
        return Html.fill("<span class=\"k-num\">{{count}}<em>{{share}}</em></span>")
            .put("count", count)
            .put("share", Stats.percent(stats.percentOfPairs(count)))
            .render();
    }

    private static String summary(Stats stats) {
        if (stats == null) {
            return "";
        }
        return Html.fill(SUMMARY).raw("content", SummaryView.render(stats, "Route length")).render();
    }

    private static final List<String> SCRIPTS = List.of(
        "/viz/overlay.js",        // the enlarge overlay: open, close, detail panel
        "/viz/tables.js",         // click-to-sort table headers
        "/viz/graph.js",          // conversion-graph explorer (cytoscape + physics)
        "/viz/chrome.js",         // tab bar and summary modal
        "/viz/matrix-cells.js");  // hover and pin for enlarged matrix cells

    private static String pageScript() throws IOException {
        var script = new StringBuilder("(function () {\n");
        for (String path : SCRIPTS) {
            script.append(resource(path));
        }
        return script.append("})();\n").toString();
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = PageRenderer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is missing from the graph-viz resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
