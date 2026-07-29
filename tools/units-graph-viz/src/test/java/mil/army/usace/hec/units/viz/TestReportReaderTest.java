package mil.army.usace.hec.units.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import mil.army.usace.hec.graph.viz.model.EdgeStatus;

class TestReportReaderTest {

    @Test
    void reads_each_status_the_report_can_contain(@TempDir Path dir) throws Exception {
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
        assertEquals(EdgeStatus.PASSED, edges.get(0).status());

        assertEquals(EdgeStatus.FAILED, edges.get(1).status());
        assertEquals(EdgeStatus.UNTESTED, edges.get(2).status());
    }

    /** An unrecognised status must look uncovered, never falsely green. */
    @Test
    void treats_an_unknown_status_as_untested(@TempDir Path dir) throws Exception {
        Path report = dir.resolve("report.xml");
        Files.writeString(report, """
            <unit-conversions>
              <conversion from="ft" to="m" status="something-new"/>
              <conversion from="m" to="ft"/>
            </unit-conversions>
            """);

        var edges = TestReportReader.read(report);

        assertEquals(EdgeStatus.UNTESTED, edges.get(0).status());
        assertEquals(EdgeStatus.UNTESTED, edges.get(1).status());
    }

    @Test
    void explains_itself_when_the_report_has_not_been_generated(@TempDir Path dir) {
        var missing = dir.resolve("nope.xml");

        var thrown = assertThrows(IOException.class, () -> TestReportReader.read(missing));

        assertEquals(true, thrown.getMessage().contains(":units:test"));
    }
}