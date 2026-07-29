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

    public static String render(String title, String body) throws IOException {
        return "<!doctype html>\n<html lang=\"en\">\n<head>\n"
            + "<meta charset=\"utf-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
            + "<title>" + Html.escape(title) + "</title>\n"
            + "<style>\n" + loadCss() + "\n</style>\n"
            + "</head>\n<body>\n"
            + "<h1>" + Html.escape(title) + "</h1>\n"
            + "<p class=\"lede\">Every conversion the algorithm can produce, one matrix per "
            + "dimension. Rows convert to columns. Click any card to enlarge it; hover a "
            + "cell for the pair and its status.</p>\n"
            + legend()
            + body
            + overlay()
            + "<script>\n" + loadJs() + "\n</script>\n"
            + "</body>\n</html>\n";
    }

    private static String legend() {
        return "<div class=\"legend\">"
            + "<span><i class=\"sw passed\"></i>passed</span>"
            + "<span><i class=\"sw failed\"></i>failed</span>"
            + "<span><i class=\"sw untested\"></i>reachable, not tested</span>"
            + "<span><i class=\"sw missing\"></i>no conversion</span>"
            + "</div>\n";
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
            + "<span id=\"otally\" class=\"tally\"></span>"
            + "<button id=\"oclose\" type=\"button\">Close</button>"
            + "</div>\n"
            + "<div id=\"ostagewrap\">\n"
            + "<div id=\"ostage\"></div>\n"
            + "<aside id=\"opanel\"><div id=\"odetail\"></div></aside>\n"
            + "</div>\n"
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
