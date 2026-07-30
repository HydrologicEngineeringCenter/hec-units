/*
 * Copyright 2022 Michael Neilson
 * Licensed Under MIT License. https://github.com/MikeNeilson/housedb/LICENSE.md
 */

package db.migration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import cwms.units.ConversionGraph;
import cwms.units.Loader;
import cwms.units.Unit;

import org.opendcs.jas.core.Mode;
import net.hobbyscience.SimpleInfixCalculator;
import net.hobbyscience.database.Conversion;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


class UnitConversionTest {
    private static final Logger log = Logger.getLogger(UnitConversionTest.class.getName());

    private static HashSet<Conversion> conversions;
    private static final HashSet<String> expected_conversion_pairs = new HashSet<>();
    private static final HashSet<String> expected_test_pairs = new HashSet<>();

    // For debugging visualizer, it will be helpful to differentiate between FAILED and NOT-TESTED
    private enum Status {
        PASSED,
        FAILED
    }

    private static final Map<String, Status> results = new HashMap<>();

    @BeforeAll
    static void setup() throws Exception {
        Mode.DEBUG = true;
        Mode.FRACTION = true;

        var loader = new Loader();

        ConversionGraph graph = new ConversionGraph(loader.getConversions());
        conversions = graph.generateConversions();
        log.finest(() -> { 
            StringBuilder sb = new StringBuilder();
            conversions.forEach(c-> sb.append(c.toString()).append(System.lineSeparator()));
            return sb.toString();
        });
        
        assertTrue(conversions.size() > 0);

        for (var conversion: conversions) {
            expected_conversion_pairs.add(toConversionKey(conversion.getFrom(), conversion.getTo()));
            expected_conversion_pairs.add(toConversionKey(conversion.getTo(), conversion.getFrom()));
        }
        try (var data = UnitConversionTest.class.getResourceAsStream("/units/conversions_to_test.csv")) {
            assertNotNull(data, "Unable to load /units/conversions_to_test.csv");
            try (var reader = new BufferedReader(new InputStreamReader(data, StandardCharsets.UTF_8))) {
                // Skip header.
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        expected_test_pairs.add(toConversionKey(parts[0].trim(),parts[1].trim()));
                    }
                }
            }
        }
    }

    @AfterAll
    static void check_provided_tests_passed() {
        final HashSet<String> conversionKeySet = new HashSet<>();
        results.forEach((pair, status) -> {
            if (status == Status.PASSED) {
                conversionKeySet.add(pair);
            }
        });
        
        boolean failedConversions = false;
        final var sb = new StringBuilder();
        sb.append("Not all CSV conversion pairs passed.").append(System.lineSeparator());
        sb.append("The following tests did not register as successful:").append(System.lineSeparator());
        for(var expected_test: expected_test_pairs) {
            if (!conversionKeySet.contains(expected_test)) {
                final var parts = expected_test.split("_");
                sb.append("\t")
                    .append(parts[0])
                    .append(" -> ")
                    .append(parts[1])
                    ;
                failedConversions = true;
            }
        }
        
        if (failedConversions) {
            fail(() -> sb.toString());
        }
        
    }

    @AfterAll
    static void create_unit_conversion_report() throws Exception {
        var xmlFactory = XMLOutputFactory.newInstance();
                
        var buffer = new StringWriter();
    
        var writer = xmlFactory.createXMLStreamWriter(buffer);
        // allows us to discern between tests that failed, and conversions that are not covered by testing suite.
        int passed = 0;
        int failed = 0;
        for (var status : results.values()) {
            if (status == Status.PASSED) {
                passed++;
            } else {
                failed++;
            }
        }
        final int totalExpected = expected_conversion_pairs.size();
        final int numNotTested = totalExpected - passed - failed;
        final float percentTested = passed / (float) totalExpected * 100;

        writer.writeStartDocument();
        writer.writeStartElement("unit-conversions");
        writer.writeAttribute("expected", "" + totalExpected);
        writer.writeAttribute("actual", "" + passed);
        writer.writeAttribute("passed", "" + passed);
        writer.writeAttribute("failed", "" + failed);
        writer.writeAttribute("not-tested", "" + numNotTested);
        writer.writeAttribute("percent-tested", String.format("%.01f", percentTested));

        for (var pair : new TreeSet<>(expected_conversion_pairs)) {
            var parts = pair.split("_");
            var status = results.get(pair);
            writer.writeStartElement("conversion");
            writer.writeAttribute("from", parts[0]);
            writer.writeAttribute("to", parts[1]);
            writer.writeAttribute("status", 
                status == null ? "not-tested" : status.name().toLowerCase());
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        try (var file = new FileOutputStream("build/reports/unit_conversion_report.xml"))
        {
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new StreamSource(new StringReader(buffer.toString())),
                                  new StreamResult(file));
        }
    }

    private static void record(String from, String to, boolean passed) {
        String key = toConversionKey(from, to);
        if (passed) {
            results.putIfAbsent(key, Status.PASSED); // only if both ways pass do we allow a pass
        } else {
            results.put(key, Status.FAILED);
        }
    }

    @ParameterizedTest /*(name="[{index}] {arguments}")*/
    @CsvFileSource(resources = "/units/conversions_to_test.csv", useHeadersInDisplayName = false, numLinesToSkip = 1)
    void test_units(String from, String to, double in, double expected, double delta, double inverseDelta) {
        var fromUnit = getUnit(from);
        var toUnit = getUnit(to);
        var conversion = getConversion(fromUnit,toUnit);
        var inverseConversion = getConversion(toUnit, fromUnit);
        var infix = conversion.getMethod().getPostfix();
        var inverseInfix = inverseConversion.getMethod().getPostfix();

        log.finest(()->"Forward conversion " + conversion.toString());
        double forward = SimpleInfixCalculator.calculate(infix, in);
        boolean forwardOk = false; // assume false unless we pass
        try {
            assertTrue(Double.isFinite(forward), () -> "Forward conversion produced non-finite value using " + conversion.toString());
            assertEquals(expected, forward, delta, () -> "Unable to perform conversion using " + conversion.toString() + " within " + delta);
            forwardOk = true;
        } finally {
            record(from, to, forwardOk);
        }

        log.finest(()->"Inverse conversion " + inverseConversion.toString());
        double inverse = SimpleInfixCalculator.calculate(inverseInfix, forward);
        boolean inverseOk = false;
        try {
            assertTrue(Double.isFinite(inverse), () -> "Inverse conversion produced non-finite value using " + inverseConversion.toString());
            assertEquals(in, inverse, inverseDelta, () -> "Unable to perform inverse conversion using " + inverseConversion.toString() + " within " + inverseDelta);
            inverseOk = true;
        } finally {
            record(to, from, inverseOk);
        }
    }

    private Conversion getConversion(Unit from, Unit to) {
        return conversions.stream()
                          .filter( c -> c.getFrom().equals(from) 
                                     && c.getTo().equals(to))
                          .findFirst().get();
    }

    private Unit getUnit(String unit) {
        return conversions.stream()
                          .filter(c -> c.getFrom().getAbbreviation().equals(unit))
                          .findFirst()
                          .get().getFrom();
    }


    private static String toConversionKey(Unit from, Unit to) {
        return toConversionKey(from.getAbbreviation(), to.getAbbreviation());
    }

    private static String toConversionKey(String from, String to) {
        return from + "_" + to;
    }
}
