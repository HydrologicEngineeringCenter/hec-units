package mil.army.usace.hec.units.viz;

/**
 * One row of conversions_to_test.csv.
 */
record TestCase(String from, String to, double input, double expected,
                double delta, double inverseDelta) {
}
