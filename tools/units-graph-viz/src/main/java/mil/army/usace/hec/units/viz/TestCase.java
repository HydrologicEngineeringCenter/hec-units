package mil.army.usace.hec.units.viz;

/**
 * One row of conversions_to_test.csv.
 *
 * The suite uses each row twice: convert `input` from `from` to `to` and expect
 * `expected` within `delta`, then convert the result back and expect the
 * original `input` within `inverseDelta`. The two tolerances are separate
 * because the two directions work at different magnitudes.
 */
record TestCase(String from, String to, double input, double expected,
                double delta, double inverseDelta) {
}
