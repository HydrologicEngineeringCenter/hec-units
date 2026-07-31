package mil.army.usace.hec.units.viz;

/**
 * One row of conversions_to_test.csv.
 *
 * The suite uses it twice: convert and check within `delta`, then convert back
 * and check within `inverseDelta`.
 */
record TestCase(String from, String to, double input, double expected,
                double delta, double inverseDelta) {
}
