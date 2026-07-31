package mil.army.usace.hec.graph.viz.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Wraps view fragments in a complete, self-contained HTML document. */
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
        <p class="lede">Every conversion the algorithm can produce, one matrix per dimension.
          <b>Each row converts into the columns.</b> Click a card to enlarge it, then click any
          cell for its equation and test results.</p>
        </div>
        {{summaryButton}}
        </div>
        {{legend}}
        {{body}}
        {{overlay}}
        {{summary}}
        {{data}}
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
        <button id="oclose" type="button">Close</button>
        </div>
        {{legend}}
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

    /**
     * @param stats counts for the key and the summary; null omits both
     * @param data  a script emitted before the page script, for any dataset the
     *              page has to work with at runtime
     */
    public static String render(String title, Stats stats, String body, String data)
            throws IOException {
        return Html.fill(PAGE)
            .put("title", title)
            .put("title2", title)
            .raw("css", resource("/viz.css"))
            .raw("js", resource("/viz.js"))
            .raw("summaryButton", stats == null ? ""
                 : "<button id=\"sumopen\" type=\"button\">Summary</button>")
            .raw("legend", legend("legend", stats))
            .raw("body", body)
            .raw("overlay", Html.fill(OVERLAY).raw("legend", legend("legend olegend", null)).render())
            .raw("summary", summary(stats))
            .raw("data", data.isEmpty() ? "" : "<script>\n" + data + "</script>")
            .render();
    }

    /**
     * The colour key, with each category's count and share.
     *
     * Rendered twice - the enlarged view covers the page, and a key you must
     * close the thing you are reading to consult is no key at all.
     */
    private static String legend(String className, Stats stats) {
        return Html.tag("div").attr("class", className).html(
                key(stats, "passed", "passed")
              + key(stats, "failed", "failed")
              + key(stats, "untested", "reachable, not tested")
              + key(stats, "missing", "no conversion")
              + "<span class=\"hint\">numbers in cells are hops in the chosen route</span>")
            .toString();
    }

    private static String key(Stats stats, String cls, String label) {
        return Html.fill(KEY)
            .put("cls", cls)
            .put("label", label)
            .raw("counts", stats == null ? "" : counts(stats, cls))
            .render();
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

    /**
     * CSS and JS live as real files but are inlined, so the output is one
     * self-contained page that cannot arrive with a broken asset path.
     *
     * The leading slash matters: without it the path resolves relative to this
     * class's package and comes back null.
     */
    private static String resource(String path) throws IOException {
        try (InputStream in = PageRenderer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is missing from the graph-viz resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
