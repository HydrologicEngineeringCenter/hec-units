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

        var graph = GeneratedGraphSource.load(report);
        String html = PageRenderer.render("Unit conversion coverage", MatrixView.render(graph));

        Files.createDirectories(outputDir);
        Path index = outputDir.resolve("index.html");
        Files.writeString(index, html, StandardCharsets.UTF_8);

        System.out.println(graph.nodes().size() + " units, " + graph.edges().size() + " conversions");
        System.out.println("Open: file://" + index.toAbsolutePath());
    }
}