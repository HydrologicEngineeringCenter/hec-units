package mil.army.usace.hec.graph.viz.formula;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Evaluates a postfix (reverse-Polish) expression at a given value of `i`.
 *
 * Multi-hop conversions expose only postfix - asking them for infix throws.
 * Operand order is not symmetric: for "a b -" the answer is a - b.
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
