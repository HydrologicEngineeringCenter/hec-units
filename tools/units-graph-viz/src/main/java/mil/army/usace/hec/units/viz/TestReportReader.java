package mil.army.usace.hec.units.viz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.EdgeStatus;

/**
 * Reads the unit_conversion_report.xml written by :units:test and turns each
 * entry into an Edge carrying its pass/fail/untested status
 */
public final class TestReportReader {

    public static List<Edge> read(Path report) throws IOException, XMLStreamException {
        if (!Files.isReadable(report)) {
            throw new IOException("No test report at " + report.toAbsolutePath()
                + " - run './gradlew :units:test' first.");
        }

        var factory = XMLInputFactory.newInstance();

        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        var edges = new ArrayList<Edge>();
        try (InputStream in = Files.newInputStream(report)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT
                        && "conversion".equals(reader.getLocalName())) {
                    edges.add(new Edge(
                        reader.getAttributeValue(null, "from"),
                        reader.getAttributeValue(null, "to"),
                        toStatus(reader.getAttributeValue(null, "status"))));
                }
            }
            reader.close();
        }
        return edges;
    }

    private static EdgeStatus toStatus(String status) {
        if ("passed".equals(status)) {
            return EdgeStatus.PASSED;
        }
        if ("failed".equals(status)) {
            return EdgeStatus.FAILED;
        }
        return EdgeStatus.UNTESTED;
    }
}