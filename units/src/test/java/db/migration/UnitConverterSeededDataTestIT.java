/*
 * Verifies that conversions seeded by V7.5__engineering_units_seed.sql are
 * consistent with the HEC units library.  The original test (from OpenDCS)
 * ran against a live database; this version replaces the database lookup with
 * the in-process ConversionGraph so no database is required.
 *
 * Unit abbreviations are mapped from OpenDCS-legacy style to CWMS-standard
 * where the two differ (e.g. "degC" -> "C", "m^3/s" -> "cms", "ft^2" ->
 * "ft2").  Conversions that involve units not present in the HEC library
 * (e.g. pa/Pascal, cM/s, time chain) are omitted.
 */
package db.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cwms.units.ConversionGraph;
import cwms.units.Loader;
import net.hobbyscience.SimpleInfixCalculator;
import net.hobbyscience.database.Conversion;

class UnitConverterSeededDataTestIT {

    private static HashSet<Conversion> conversions;

    private static final double DELTA = 1e-4;

    @BeforeAll
    static void setup() throws Exception {
        var loader = new Loader();
        ConversionGraph graph = new ConversionGraph(loader.getConversions());
        conversions = graph.generateConversions();
        assertTrue(conversions.size() > 0);
    }

    // -------------------------------------------------------------------------
    // Length  (OpenDCS legacy abbrs mapped: none needed for this set)
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "1.0,  ft,  m,    0.3048",
        "1.0,  ft,  in,  12.0",
        "1.0,  mi,  ft,   5280.0",
        "1.0,  m,   mm,   1000.0"
    })
    void test_length_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------
    // Temperature  (OpenDCS "degC"/"degF"/"degK" -> HEC "C"/"F"/"K")
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        // C -> F: Y = (9/5)*X + 32
        "0.0,    C,  F,   32.0",
        "100.0,  C,  F,  212.0",
        "-40.0,  C,  F,  -40.0",
        // C -> K: Y = X + 273.15
        "0.0,    C,  K,  273.15",
        "100.0,  C,  K,  373.15"
    })
    void test_temperature_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------
    // Flow  (OpenDCS "m^3/s" -> HEC "cms")
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "1.0,  cms,   cfs,   35.31466247",
        "1.0,  kcfs,  cfs,  1000.0"
    })
    void test_flow_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------
    // Power  (same abbreviations in both systems)
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "1.0,  kW,  W,   1000.0",
        "1.0,  MW,  kW,  1000.0",
        "1.0,  GW,  MW,  1000.0",
        "1.0,  TW,  GW,  1000.0"
    })
    void test_power_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------
    // Area  (OpenDCS "ft^2"/"M^2" -> HEC "ft2"/"m2")
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "1.0,  acre,  ft2,    43560.0",
        "1.0,  ft2,   m2,     0.09290304",
        "1.0,  ha,    m2,  10000.0"
    })
    void test_area_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------
    // Volume  (OpenDCS "acre*ft"/"ft^3"/"m^3" -> HEC "ac-ft"/"ft3"/"m3")
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "1.0,  ac-ft,  ft3,  43560.0",
        "1.0,  ac-ft,  m3,   1233.4818",
        "1.0,  kaf,    ac-ft, 1000.0"
    })
    void test_volume_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------
    // Velocity  (OpenDCS "ft/s -> M/s" -> HEC "ft/s -> m/s")
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "1.0,  ft/s,  m/s,  0.3048"
    })
    void test_velocity_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------
    // Concentration  (same abbreviations in both systems)
    // -------------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "1.0,  g/l,  mg/l,      1000.0",
        "1.0,  g/l,  ug/l,  1000000.0"
    })
    void test_concentration_conversions(double input, String from, String to, double expected) {
        assertConversion(input, from, to, expected);
    }

    // -------------------------------------------------------------------------

    private void assertConversion(double input, String from, String to, double expected) {
        Optional<Conversion> conv = conversions.stream()
            .filter(c -> c.getFrom().getAbbreviation().equals(from)
                      && c.getTo().getAbbreviation().equals(to))
            .findFirst();

        assertTrue(conv.isPresent(),
            () -> String.format("No seeded converter found for %s -> %s", from, to));

        var infix = conv.get().getMethod().getPostfix();
        double result = SimpleInfixCalculator.calculate(infix, input);

        assertEquals(expected, result, DELTA,
            () -> String.format("%s -> %s: convert(%s) expected %s but was %s",
                                from, to, input, expected, result));
    }
}
