package mil.army.usace.hec.units.viz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Generates the whole page and checks it holds together.
 */
class PageGenerationTest {

    private static final Pattern SCRIPT = Pattern.compile("<script>(.*?)</script>", Pattern.DOTALL);
    private static final Pattern HOLE = Pattern.compile("\\{\\{[a-zA-Z]\\w*}}");

    @TempDir
    static Path output;

    private static String page;

    @BeforeAll
    static void generate() throws Exception {
        GenerateVisualization.main(new String[] {
            output.toString(),
            output.resolve("no-such-report.xml").toString(),
            output.resolve("no-such-tests.csv").toString()});
        page = Files.readString(output.resolve("index.html"));
    }

    /** Everything except the vendored library, which carries templates of its own. */
    private static List<String> ourScripts() {
        var blocks = new ArrayList<String>();
        Matcher matcher = SCRIPT.matcher(page);
        while (matcher.find()) {
            blocks.add(matcher.group(1));
        }
        String vendored = blocks.stream().max((a, b) -> a.length() - b.length()).orElse("");
        blocks.remove(vendored);
        return blocks;
    }

    private static String markup() {
        return page.substring(0, page.indexOf("<script>", page.indexOf("</style>")));
    }

    private static String styles() {
        return page.substring(page.indexOf("<style>") + 7, page.indexOf("</style>"));
    }

    @Test
    void the_page_is_written_and_substantial() throws IOException {
        assertTrue(Files.isRegularFile(output.resolve("index.html")));
        assertTrue(page.length() > 100_000,
                   "page is only " + page.length() + " bytes - generation probably failed");
    }

    @Test
    void every_template_hole_was_filled() {
        var unfilled = new ArrayList<String>();
        var sources = new ArrayList<>(ourScripts());
        sources.add(markup());
        sources.add(styles());

        for (String source : sources) {
            Matcher matcher = HOLE.matcher(source);
            while (matcher.find()) {
                unfilled.add(matcher.group());
            }
        }
        assertTrue(unfilled.isEmpty(), "unfilled template placeholders: " + unfilled);
    }

    @Test
    void the_script_and_style_blocks_are_balanced() {
        String script = ourScripts().get(ourScripts().size() - 1);
        assertBalanced("our script", script, '{', '}');
        assertBalanced("our script", script, '(', ')');
        assertBalanced("style", styles(), '{', '}');
    }

    private static void assertBalanced(String what, String text, char open, char close) {
        long opens = text.chars().filter(c -> c == open).count();
        long closes = text.chars().filter(c -> c == close).count();
        assertTrue(opens == closes,
                   what + " has " + opens + " '" + open + "' and " + closes + " '" + close + "'");
    }

    /** The browser builds the tables and panels out of these; none may go missing. */
    @Test
    void the_generated_data_tables_are_all_present() {
        for (String table : List.of("var INDEX=[", "var UNITS={", "var SEED=[", "var GRAPHS={")) {
            assertTrue(page.contains(table), "missing generated data: " + table);
        }
    }

    @Test
    void the_coverage_grid_has_a_mount_for_every_dimension() {
        int mounts = markup().split("class=\"mx\" data-group=", -1).length - 1;
        assertTrue(mounts >= 20, "only " + mounts + " matrix mounts - the grid looks wrong");
    }

    /**
     * A stray control byte turns the whole file binary: editors refuse to show
     * it, git stops diffing it, and grep goes quiet - all without breaking the
     * build, so nothing else would report it.
     */
    @Test
    void the_page_is_plain_text() {
        var found = new ArrayList<String>();
        for (int i = 0; i < page.length(); i++) {
            char c = page.charAt(i);
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                found.add(String.format("0x%02x at %d near %s", (int) c, i,
                          page.substring(Math.max(0, i - 30), Math.min(page.length(), i + 10))));
            }
        }
        assertTrue(found.isEmpty(), "control characters in the page: " + found);
    }

    /** The matrices are drawn in the browser now, so the markup must not carry them. */
    @Test
    void the_heavy_markup_stayed_out_of_the_page() {
        assertFalse(markup().contains("<table class=\"matrix\""),
                    "matrix tables should be built in the browser");
        assertFalse(markup().contains("data-detail="),
                    "detail should travel on the INDEX row, not on every cell");
    }
}
