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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


class UnitConversionTest {
    private static final Logger log = Logger.getLogger(UnitConversionTest.class.getName());

    private static HashSet<Conversion> conversions;
    private static final HashSet<String> expected_conversion_pairs = new HashSet<>();
    private static final HashSet<String> expected_test_pairs = new HashSet<>();

    private static Map<String, AtomicInteger> conversion_count = new HashMap<>();

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
        final HashSet<String> conversionKeySet = new HashSet<>(conversion_count.keySet());
        
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
    static void check_all_conversions_have_test() {
        boolean missingTestConversion = false;
        
        final var sb = new StringBuilder();
        sb.append("Not all possible conversions were performed.").append(System.lineSeparator());
        sb.append("The following conversions have no test:").append(System.lineSeparator());
        final var actualConversions = conversion_count.keySet();
        for (var expected_conversion: expected_conversion_pairs) {
            if (!actualConversions.contains(expected_conversion)) {
                var parts = expected_conversion.split("_");
                sb.append("\t")
                    .append(parts[0])
                    .append(" -> ")
                    .append(parts[1])
                    .append(System.lineSeparator());
                missingTestConversion = true;
            }
        }
        if (missingTestConversion) {
            fail(() -> sb.toString());
        }
    }

    private static void update_conversion_count(String from, String to) {
        conversion_count.computeIfAbsent(toConversionKey(from, to), k -> new AtomicInteger(0)).incrementAndGet();
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
        assertTrue(Double.isFinite(forward), () -> "Forward conversion produced non-finite value using " + conversion.toString());
        assertEquals(expected, forward, delta, () -> "Unable to perform forward conversion using " + conversion.toString() + " within " + delta);
        update_conversion_count(from, to);

        log.finest(()->"Inverse conversion " + inverseConversion.toString());
        double inverse = SimpleInfixCalculator.calculate(inverseInfix, forward);
        assertTrue(Double.isFinite(inverse), () -> "Inverse conversion produced non-finite value using " + inverseConversion.toString());
        assertEquals(in, inverse, inverseDelta, () -> "Unable to perform inverse conversion using " + inverseConversion.toString() + " within " + inverseDelta);
        update_conversion_count(to, from);
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
