package mil.army.usace.hec.graph.viz.formula;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Evaluates a postfix (reverse-Polish) expression at a given value of `i`.
 *
 * This exists because a conversion derived by chaining several hops together
 * only ever exposes postfix - asking such a method for infix throws. So the
 * only way to find out what a multi-hop conversion actually computes is to
 * evaluate its postfix form, which is what feeds FormulaRenderer.affineOf.
 *
 * Operand order matters and is not symmetric: for "a b -" the result is a - b,
 * so the second value popped is the left-hand side. Getting that backwards
 * would silently invert every subtraction and division in the project.
 */
public final class PostfixEvaluator {

    private PostfixEvaluator() {
    }

    public static double evaluate(String postfix, double i) {
        if (postfix == null || postfix.isBlank()) {
            throw new IllegalArgumentException("Empty postfix expression");
        }

        Deque<Double> stack = new ArrayDeque<>();
        for (String token : postfix.trim().split("\\s+")) {
            if (token.equals("i")) {
                stack.push(i);
            } else if (isOperator(token)) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException(
                        "Operator '" + token + "' has too few operands in: " + postfix);
                }
                double right = stack.pop();
                double left = stack.pop();
                stack.push(apply(token, left, right));
            } else {
                stack.push(Double.parseDouble(token));   // throws IllegalArgumentException on junk
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException(
                "Postfix expression left " + stack.size() + " values on the stack: " + postfix);
        }
        return stack.pop();
    }

    private static boolean isOperator(String token) {
        return token.length() == 1 && "+-*/^".indexOf(token.charAt(0)) >= 0;
    }

    private static double apply(String operator, double left, double right) {
        switch (operator.charAt(0)) {
            case '+':
                return left + right;
            case '-':
                return left - right;
            case '*':
                return left * right;
            case '/':
                return left / right;
            default:
                return Math.pow(left, right);
        }
    }
}
