package mil.army.usace.hec.graph.viz.formula;

/**
 * Saves affine form equations: y = mx + b
 * Essentially just one multiply by m, and one add by b
 */
public record AffineForm(double m, double b) {

}
