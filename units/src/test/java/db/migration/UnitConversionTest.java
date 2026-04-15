/*
 * Copyright 2022 Michael Neilson
 * Licensed Under MIT License. https://github.com/MikeNeilson/housedb/LICENSE.md
 */

package db.migration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import cwms.units.ConversionGraph;
import cwms.units.Loader;
import cwms.units.Unit;

import org.opendcs.jas.core.Mode;
import net.hobbyscience.SimpleInfixCalculator;
import net.hobbyscience.database.Conversion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.logging.Logger;


class UnitConversionTest {
    private static final Logger log = Logger.getLogger(UnitConversionTest.class.getName());
    private static final double ROUND_TRIP_INPUT = 1.23456789d;

    private static HashSet<Conversion> conversions;

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
    }

    @ParameterizedTest /*(name="[{index}] {arguments}")*/
    @CsvFileSource(resources = "/units/conversions_to_test.csv", useHeadersInDisplayName = false, numLinesToSkip = 1)
    void test_units(String from, String to, double in, double expected, double delta, double inverseDelta) {
        var fromUnit = getUnit(from);
        var toUnit = getUnit(to);
        var conversion = getConversion(fromUnit,toUnit);
        var inverseConversion = getConversion(toUnit, fromUnit);
        double forward = evaluateConversion(conversion, in, "Forward");
        assertEquals(expected, forward, delta, () -> "Unable to perform forward conversion using " + conversion.toString() + " within " + delta);

        double inverse = evaluateConversion(inverseConversion, forward, "Inverse");
        assertEquals(in, inverse, inverseDelta, () -> "Unable to perform inverse conversion using " + inverseConversion.toString() + " within " + inverseDelta);
    }

    @Test
    void test_all_generated_conversions_evaluate() {
        int exercisedConversions = 0;
        for (var conversion : conversions) {
            var inverseConversion = getConversion(conversion.getTo(), conversion.getFrom());
            double forward = evaluateConversion(conversion, ROUND_TRIP_INPUT, "Forward");
            evaluateConversion(inverseConversion, forward, "Inverse");
            exercisedConversions++;
        }

        assertEquals(conversions.size(), exercisedConversions, "Not all Unit conversions have been exercised.");
    }

    private Conversion getConversion(Unit from, Unit to) {
        return conversions.stream()
                          .filter( c -> c.getFrom().equals(from) 
                                     && c.getTo().equals(to))
                          .findFirst().get();
    }

    private double evaluateConversion(Conversion conversion, double input, String direction) {
        var infix = conversion.getMethod().getPostfix();
        log.finest(() -> direction + " conversion " + conversion.toString());
        double output = SimpleInfixCalculator.calculate(infix, input);
        assertTrue(Double.isFinite(output), () -> direction + " conversion produced non-finite value using " + conversion.toString());
        return output;
    }

    private Unit getUnit(String unit) {
        return conversions.stream()
                          .filter(c -> c.getFrom().getAbbreviation().equals(unit))
                          .findFirst()
                          .get().getFrom();
    }
}
