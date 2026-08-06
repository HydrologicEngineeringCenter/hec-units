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
            <script>
              // Settle the theme before the first paint, or the page flashes
              // the wrong one. A saved choice outranks the system setting.
              (function () {
                var saved = null;
                try { saved = localStorage.getItem('viz-theme'); } catch (e) { }
                var dark = window.matchMedia
                        && matchMedia('(prefers-color-scheme:dark)').matches;
                document.documentElement.dataset.theme = saved || (dark ? 'dark' : 'light');
              })();
            </script>
            <style>
        {{css}}
            </style>
          </head>
          <body>
            <div class="pagehead">
              <h1>{{title2}}</h1>
              <div class="headbtns">
                {{summaryButton}}
                {{keysButton}}
                {{themeButton}}
              </div>
            </div>
            {{tabbar}}
            <div class="tabpane active" id="tab-coverage">
              {{legend}}
              {{coverbar}}
              {{body}}
            </div>
            {{seedpane}}
            {{findpane}}
            {{convertpane}}
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

    private static final String KEYS_BUTTON = """
        <button id="keysopen" type="button" title="Keyboard shortcuts and tips"
                aria-label="Keyboard shortcuts">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="2.2" y="6" width="19.6" height="12" rx="2.4"/>
            <path d="M6 9.6h.01M9.4 9.6h.01M12.8 9.6h.01M16.2 9.6h.01M18 12.8h.01
                     M6 12.8h.01M8.6 15.6h6.8"/>
          </svg>
          <span>Keys</span>
        </button>
        """;

    // Both icons ship; CSS shows whichever one names the theme you would get.
    private static final String THEME_BUTTON = """
        <button id="themetoggle" type="button" title="Switch between light and dark">
          <svg class="i-sun" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="4.1"/>
            <path d="M12 2.2v2.5M12 19.3v2.5M4.5 4.5l1.8 1.8M17.7 17.7l1.8 1.8
                     M2.2 12h2.5M19.3 12h2.5M4.5 19.5l1.8-1.8M17.7 6.3l1.8-1.8"/>
          </svg>
          <svg class="i-moon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M20.3 14.6A8.4 8.4 0 0 1 9.4 3.7 8.6 8.6 0 1 0 20.3 14.6Z"/>
          </svg>
          <span class="tlabel"></span>
        </button>
        """;

    private static final String OVERLAY = """
        <div id="overlay" role="dialog" aria-modal="true" aria-labelledby="otitle"
             aria-hidden="true">
          <div id="obar">
            <h3 id="otitle"></h3>
            <span id="oaxis">row → column</span>
            <span id="otally" class="tally"></span>
            <label class="find" id="ofind">
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/>
              <path d="M16.5 16.5 21 21"/></svg>
              <input type="search" placeholder="highlight a unit">
              <button type="button" class="clearfind" aria-label="clear">&times;</button>
            </label>
            <button id="ocycle" type="button" aria-pressed="true"
                    title="Cycle through the routes while two units are picked">
              Route cycling</button>
            <button id="oreset" type="button">Reset view</button>
            <button id="oclose" type="button">Close</button>
          </div>
          {{legend}}
          {{seedkey}}
          <div id="ostagewrap">
            <div id="ostage"></div>
            <div id="osplit" role="separator" aria-orientation="vertical" tabindex="0"
                 aria-label="Resize the detail panel"
                 title="Drag to resize, double-click to reset"></div>
            <aside id="opanel">
              <button type="button" id="opanelfold" title="Hide the panel"
                      aria-label="Hide the panel" aria-expanded="true">&rsaquo;</button>
              <div id="odetail" aria-live="polite"></div>
            </aside>
          </div>
        </div>
        """;

    private static final String SUMMARY = """
        <div id="summary" role="dialog" aria-modal="true" aria-label="Test suite summary"
             aria-hidden="true">
          <div id="sbar"><h3>Test suite summary</h3><button id="sclose" type="button">Close</button></div>
          <div id="sbody">{{content}}</div>
        </div>
        """;

    private static final String KEY = """
        <button type="button" class="key" data-status="{{cls}}"
                title="Search for these">
          <span class="k-top"><i class="sw {{cls}}"></i>{{label}}</span>{{counts}}
        </button>
        """;

    private PageRenderer() {
    }

    public static String render(String title, Stats stats, String body, String seedBody,
                                String data) throws IOException {
        boolean tabbed = !seedBody.isEmpty();
        return Html.fill(PAGE)
            .put("title", title)
            .put("title2", title)
            .raw("css", resource("/viz.css"))
            .raw("js", pageScript())
            .raw("cyto", tabbed ? resource("/cytoscape.min.js") : "")
            .raw("keysButton", tabbed ? KEYS_BUTTON : "")
            .raw("themeButton", THEME_BUTTON)
            .raw("summaryButton", stats == null ? ""
                 : "<button id=\"sumopen\" type=\"button\">Summary</button>")
            .raw("tabbar", tabbed ? TABBAR : "")
            .raw("legend", legend("legend", stats))
            .raw("body", body)
            .raw("coverbar", tabbed ? SearchView.bar("cover",
                     "Filter dimensions - try Volume, " + Labels.plain("ft3") + ", or Length", List.of(
                     new SearchView.Group("coverage", List.of(
                     new SearchView.Option("failed", "has failures"),
                     new SearchView.Option("untested", "incomplete"),
                     new SearchView.Option("complete", "fully covered"))))) : "")
            .raw("seedpane", tabbed
                 ? Html.fill(SEEDPANE).raw("legend", seedLegend())
                       .raw("bar", SearchView.bar("seed",
                            "Filter dimensions - try Volume, " + Labels.plain("ft3") + ", or Length", List.of(
                            new SearchView.Group("graph shape", List.of(
                            new SearchView.Option("tree", "trees"),
                            new SearchView.Option("cyclic", "has cycles"),
                            new SearchView.Option("dup", "duplicate edges"))))))
                       .raw("body", seedBody).render()
                 : "")
            .raw("findpane", tabbed ? SearchView.tab() : "")
            .raw("convertpane", tabbed ? CONVERTPANE : "")
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
          <button class="tab" data-pane="tab-seed" type="button">Conversion Graphs</button>
          <button class="tab" data-pane="tab-find" type="button">Search</button>
          <button class="tab" data-pane="tab-convert" type="button">Converter</button>
          <span class="tabink"></span>
        </div>
        """;

    private static final String CONVERTPANE = """
        <div class="tabpane" id="tab-convert">
          <div class="cv">
            <div class="cv-in">
              <label class="ed-lbl" for="cvvalue">value</label>
              <input id="cvvalue" type="text" inputmode="decimal" value="1"
                     autocomplete="off" aria-label="Value to convert">
              <label class="ed-lbl" for="cvunit">unit</label>
              <div class="cv-combo">
                <input id="cvunit" type="text" placeholder="ft" autocomplete="off"
                       role="combobox" aria-expanded="false" aria-controls="cvlist"
                       aria-label="Unit to convert from">
                <button id="cvpick" type="button" tabindex="-1"
                        aria-label="Show every unit">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M6 9.5 12 15.5 18 9.5"/>
                  </svg>
                </button>
                <div id="cvlist" role="listbox" hidden></div>
              </div>
            </div>
            <div class="cv-body">
              <div id="cvout" class="cv-out" aria-live="polite"></div>
              <aside id="cvwork" class="cv-side"></aside>
            </div>
          </div>
        </div>
        """;

    private static final String SEEDPANE = """
        <div class="tabpane" id="tab-seed">
          {{legend}}
          {{bar}}
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
              <button type="button" data-system="SI"><i class="sw t-si"></i>SI</button>
              <button type="button" data-system="English"><i class="sw t-english"></i>English</button>
              <button type="button" data-system="NULL"><i class="sw t-null"></i>system-agnostic</button>
              <button type="button" data-kind="linear"><i class="ln"></i><code>linear:</code> scale + offset</button>
              <button type="button" data-kind="function"><i class="ln dash"></i><code>function:</code> arbitrary expression</button>
              <span class="hint">a bowed pair is two different conversions for the same two units</span>
            </div>
            """;
    }

    // The same key on the dark backdrop, shown only while a graph is enlarged
    private static String seedKey() {
        return """
            <div class="legend olegend skey">
              <button type="button" data-system="SI"><i class="sw t-si"></i>SI</button>
              <button type="button" data-system="English"><i class="sw t-english"></i>English</button>
              <button type="button" data-system="NULL"><i class="sw t-null"></i>system-agnostic</button>
              <button type="button" data-kind="linear"><i class="ln"></i>linear</button>
              <button type="button" data-kind="function"><i class="ln dash"></i>function</button>
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
        "/viz/matrix.js",         // builds the coverage matrices and their detail panels
        "/viz/overlay.js",        // the enlarge overlay: open, close, detail panel
        "/viz/panel.js",          // the detail panel: dock, resize, collapse
        "/viz/tables.js",         // click-to-sort table headers
        "/viz/graph-style.js",    // the cytoscape stylesheet for the graph
        "/viz/graph.js",          // conversion-graph explorer (cytoscape + physics)
        "/viz/chrome.js",         // tab bar and summary modal
        "/viz/matrix-cells.js",   // hover and pin for enlarged matrix cells
        "/viz/search.js",         // search tab, grid filters, in-matrix highlight
        "/viz/converter.js",      // the converter tab and its worked derivations
        "/viz/extras.js");        // suggestions, precision budget, links, keyboard

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
