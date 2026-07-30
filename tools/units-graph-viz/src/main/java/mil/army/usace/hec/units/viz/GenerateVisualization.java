package mil.army.usace.hec.units.viz;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import mil.army.usace.hec.graph.viz.view.MatrixView;
import mil.army.usace.hec.graph.viz.view.PageRenderer;

/**
 * Joins the hec-units data sources to the generic views and writes the page.
 * Branches between graph-viz and units-graph-viz
 */
public final class GenerateVisualization {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: GenerateVisualization <output-dir> <test-report.xml>");
            System.exit(2);
        }

        Path outputDir = Path.of(args[0]);
        Path report = Path.of(args[1]);

        // A missing report is not an error here. The page is still worth having -
        // it just cannot say anything about coverage, so it says so instead of
        // quietly showing every conversion as untested.
        boolean covered = GeneratedGraphSource.hasReport(report);

        var graph = GeneratedGraphSource.load(report);
        String html = PageRenderer.render("Unit conversion coverage",
                                          (covered ? "" : missingReportNotice()) + MatrixView.render(graph));

        Files.createDirectories(outputDir);
        Path index = outputDir.resolve("index.html");
        Files.writeString(index, html, StandardCharsets.UTF_8);

        if (!covered) {
            System.err.println("warning: no test report at " + report.toAbsolutePath()
                + " - showing the algorithm's conversions with no coverage information.");
        }
        System.out.println(graph.nodes().size() + " units, " + graph.edges().size() + " conversions"
            + (covered ? "" : " (no coverage data)"));
        System.out.println("Open: file://" + index.toAbsolutePath());
    }

    private static String missingReportNotice() {
        return "<div class=\"notice\"><b>No test report found.</b> Every conversion below is "
            + "shown as untested because there is no coverage data to read - not because it "
            + "went untested. Run <code>./gradlew :units:test</code>, then regenerate.</div>\n";
    }
}