package mil.army.usace.hec.graph.viz.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wraps view fragments in a complete, self-contained HTML document.
 */
public final class PageRenderer {

    private PageRenderer() {
    }

    /**
     * @param stats counts shown in the key and the summary; may be null
     * @param data  a JavaScript snippet emitted before the page script, used to
     *              hand the page any dataset it has to work with at runtime
     */
    public static String render(String title, Stats stats, String body, String data)
            throws IOException {
        return "<!doctype html>\n<html lang=\"en\">\n<head>\n"
            + "<meta charset=\"utf-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
            + "<title>" + Html.escape(title) + "</title>\n"
            + "<style>\n" + loadCss() + "\n</style>\n"
            + "</head>\n<body>\n"
            + header(title, stats)
            + legend("legend", stats)
            + body
            + overlay()
            + summary(stats)
            + (data.isEmpty() ? "" : "<script>\n" + data + "</script>\n")
            + "<script>\n" + loadJs() + "\n</script>\n"
            + "</body>\n</html>\n";
    }

    private static String header(String title, Stats stats) {
        var out = new StringBuilder("<div class=\"pagehead\"><div>");
        out.append("<h1>").append(Html.escape(title)).append("</h1>\n");
        out.append("<p class=\"lede\">Every conversion the algorithm can produce, one matrix per "
                 + "dimension. <b>Each row converts into the columns.</b> Click a card to enlarge "
                 + "it, then click any cell for its equation and test results.</p>");
        out.append("</div>");
        if (stats != null) {
            // Put the whole-project read-out one click from the top of the page:
            // "how are we doing overall" is the first question, and the matrices
            // answer it only after a lot of squinting.
            out.append("<button id=\"sumopen\" type=\"button\">Summary</button>");
        }
        return out.append("</div>\n").toString();
    }

    /**
     * The colour key, with each category's raw count and share underneath.
     *
     * Rendered twice - once on the page and once inside the enlarged view -
     * because the enlarged view covers the page completely, and a key you have
     * to close the thing you are reading to consult is no key at all.
     */
    private static String legend(String className, Stats stats) {
        var out = new StringBuilder("<div class=\"" + className + "\">");
        key(out, stats, "passed", "passed", stats == null ? -1 : stats.passed());
        key(out, stats, "failed", "failed", stats == null ? -1 : stats.failed());
        key(out, stats, "untested", "reachable, not tested", stats == null ? -1 : stats.untested());
        key(out, stats, "missing", "no conversion", stats == null ? -1 : stats.missing());
        out.append("<span class=\"hint\">numbers in cells are hops in the chosen route</span>");
        return out.append("</div>\n").toString();
    }

    private static void key(StringBuilder out, Stats stats, String cls, String label, int count) {
        out.append("<span class=\"key\"><span class=\"k-top\"><i class=\"sw ").append(cls)
           .append("\"></i>").append(Html.escape(label)).append("</span>");
        if (count >= 0) {
            out.append("<span class=\"k-num\">").append(count)
               .append("<em>").append(Stats.percent(stats.percentOfPairs(count)))
               .append("</em></span>");
        }
        out.append("</span>");
    }

    /**
     * The empty shell the enlarged view is populated into.
     *
     * It ships as part of every page rather than being created in script, so the
     * layout and styling live in the same two files as everything else and the
     * JavaScript only has to move content into it.
     */
    private static String overlay() {
        return "<div id=\"overlay\">\n"
            + "<div id=\"obar\">"
            + "<h3 id=\"otitle\"></h3>"
            + "<span id=\"oaxis\">row → column</span>"
            + "<span id=\"otally\" class=\"tally\"></span>"
            + "<button id=\"oclose\" type=\"button\">Close</button>"
            + "</div>\n"
            + legend("legend olegend", null)
            + "<div id=\"ostagewrap\">\n"
            + "<div id=\"ostage\"></div>\n"
            + "<aside id=\"opanel\"><div id=\"odetail\"></div></aside>\n"
            + "</div>\n"
            + "</div>\n";
    }

    /** Fully rendered up front - it is static, so there is nothing to build lazily. */
    private static String summary(Stats stats) {
        if (stats == null) {
            return "";
        }
        return "<div id=\"summary\">\n"
            + "<div id=\"sbar\"><h3>Test suite summary</h3>"
            + "<button id=\"sclose\" type=\"button\">Close</button></div>\n"
            + "<div id=\"sbody\">" + SummaryView.render(stats, "Route length") + "</div>\n"
            + "</div>\n";
    }

    private static String loadCss() throws IOException {
        return loadResource("/viz.css");
    }

    private static String loadJs() throws IOException {
        return loadResource("/viz.js");
    }

    /**
     * Reads a stylesheet or script out of the module's resources.
     *
     * These live as real .css and .js files so they can be edited as such rather
     * than as escaped Java strings, but they get inlined into the document rather
     * than linked. That keeps the output a single self-contained file, which
     * cannot arrive with a broken asset path however it is opened or copied.
     *
     * The leading slash matters: without it the path would be resolved relative
     * to this class's package instead of the classpath root, and come back null.
     */
    private static String loadResource(String path) throws IOException {
        try (InputStream in = PageRenderer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is missing from the graph-viz resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
