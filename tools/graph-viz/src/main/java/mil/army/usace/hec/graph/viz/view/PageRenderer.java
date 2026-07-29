package mil.army.usace.hec.graph.viz.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wraps view fragments in a complete, self-contained HTML document.
 */
public final class PageRenderer {

    public static String render(String title, String body) throws IOException {
        return "<!doctype html>\n<html lang=\"en\">\n<head>\n"
             + "<meta charset=\"utf-8\">\n"
             + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
             + "<title>" + Html.escape(title) + "</title>\n"
             + "<style>\n" + loadCss() + "\n</style>\n"
             + "</head>\n<body>\n"
             + "<h1>" + Html.escape(title) + "</h1>\n"
             + legend()
             + body
             + "</body>\n</html>\n";
    }

    private static String legend() {
        return "<div class=\"legend\">"
             + "<span><i class=\"passed\"></i>passed</span>"
             + "<span><i class=\"failed\"></i>failed</span>"
             + "<span><i class=\"untested\"></i>not tested</span>"
             + "<span><i class=\"missing\"></i>no conversion</span>"
             + "</div>\n";
    }

    private static String loadCss() throws IOException {
        try (InputStream in = PageRenderer.class.getResourceAsStream("/viz.css")) {
            if (in == null) {
                throw new IOException("viz.css is missing from the graph-viz resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}