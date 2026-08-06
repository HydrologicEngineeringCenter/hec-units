package mil.army.usace.hec.units.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import mil.army.usace.hec.graph.viz.model.EdgeStatus;

class TestReportReaderTest {

    @ParameterizedTest(name = "{2}")
    @CsvSource(delimiter = '|', value = {
        "status=\"passed\"        | PASSED   | passed is passed",
        "status=\"failed\"        | FAILED   | failed is failed",
        "status=\"not-tested\"    | UNTESTED | not-tested is untested",
        "status=\"something-new\" | UNTESTED | an unrecognised status reads as untested",
        "                         | UNTESTED | a missing status reads as untested"
    })
    void maps_a_report_status(String attribute, EdgeStatus expected, String rule,
                              @TempDir Path dir) throws Exception {
        Path report = dir.resolve("report.xml");
        Files.writeString(report, """
            <?xml version="1.0" encoding="UTF-8"?>
            <unit-conversions>
              <conversion from="ft" to="m" ATTR/>
            </unit-conversions>
            """.replace("ATTR", attribute == null ? "" : attribute));

        var edges = TestReportReader.read(report);

        assertEquals(1, edges.size());
        assertEquals(expected, edges.get(0).status());
    }

    @Test
    void reads_every_conversion_in_order(@TempDir Path dir) throws Exception {
        Path report = dir.resolve("report.xml");
        Files.writeString(report, """
            <?xml version="1.0" encoding="UTF-8"?>
            <unit-conversions expected="3" passed="1" failed="1" not-tested="1">
              <conversion from="ft" to="m" status="passed"/>
              <conversion from="m" to="ft" status="failed"/>
              <conversion from="C" to="K" status="not-tested"/>
            </unit-conversions>
            """);

        var edges = TestReportReader.read(report);

        assertEquals(3, edges.size());
        assertEquals("ft", edges.get(0).from());
        assertEquals("m", edges.get(0).to());
        assertEquals("C", edges.get(2).from());
        assertEquals("K", edges.get(2).to());
    }

    @Test
    void explains_itself_when_the_report_has_not_been_generated(@TempDir Path dir) {
        var missing = dir.resolve("nope.xml");

        var thrown = assertThrows(IOException.class, () -> TestReportReader.read(missing));

        assertEquals(true, thrown.getMessage().contains(":units:test"));
    }
}
