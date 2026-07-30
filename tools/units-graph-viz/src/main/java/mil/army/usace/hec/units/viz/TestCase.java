package mil.army.usace.hec.units.viz;

/**
 * One row of conversions_to_test.csv: convert `input` from `from` to `to` and
 * the answer should be `expected`, give or take `delta`.
 */
record TestCase(String from, String to, double input, double expected, double delta) {
}
