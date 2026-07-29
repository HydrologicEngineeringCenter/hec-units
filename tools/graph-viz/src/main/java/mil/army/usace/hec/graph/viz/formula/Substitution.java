package mil.army.usace.hec.graph.viz.formula;

import java.util.List;

/**
 * Result of resolving named constants within an expression
 */
public record Substitution(String expression, List<String> used) {

}
