package mil.army.usace.hec.graph.viz.formula;

/**
 * Evaluates an infix expression at a given value of `i`.
 */
public final class ExpressionEvaluator {

    private final String expr;
    private int pos;

    private ExpressionEvaluator(String expr) {
        this.expr = expr;
    }

    public static double evaluate(String expr, double i) {
        var evaluator = new ExpressionEvaluator(expr);
        double result = evaluator.parseExpr(i);
        evaluator.skipWhitespace();
        if (evaluator.pos != expr.length()) {
            throw new IllegalArgumentException(
                "Unexpected character at position " + evaluator.pos + " in: " + expr);
        }
        return result;
    }

    // expr := term (('+' | '-') term)*
    private double parseExpr(double i) {
        double value = parseTerm(i);
        while (true) {
            skipWhitespace();
            if (peek('+')) {
                pos++;
                value += parseTerm(i);
            } else if (peek('-')) {
                pos++;
                value -= parseTerm(i);
            } else {
                return value;
            }
        }
    }

    // term := unary (('*' | '/') unary)*
    private double parseTerm(double i) {
        double value = parseUnary(i);
        while (true) {
            skipWhitespace();
            if (peek('*')) {
                pos++;
                value *= parseUnary(i);
            } else if (peek('/')) {
                pos++;
                value /= parseUnary(i);
            } else {
                return value;
            }
        }
    }

    // unary := '-' unary | power
    private double parseUnary(double i) {
        skipWhitespace();
        if (peek('-')) {
            pos++;
            return -parseUnary(i);
        }
        return parsePower(i);
    }

    // power := primary ('^' unary)?      (right-associative: 2^3^2 == 2^(3^2))
    private double parsePower(double i) {
        double base = parsePrimary(i);
        skipWhitespace();
        if (peek('^')) {
            pos++;
            double exponent = parseUnary(i);
            return Math.pow(base, exponent);
        }
        return base;
    }

    // primary := NUMBER | 'i' | '(' expr ')'
    private double parsePrimary(double i) {
        skipWhitespace();
        if (peek('(')) {
            pos++;
            double value = parseExpr(i);
            skipWhitespace();
            expect(')');
            return value;
        }
        if (peek('i') && !isIdentifierChar(peekAt(pos + 1))) {
            pos++;
            return i;
        }
        return parseNumber();
    }

    private double parseNumber() {
        int start = pos;
        while (pos < expr.length() && isNumberChar(pos)) {
            pos++;
        }
        if (pos == start) {
            throw new IllegalArgumentException("Expected a number at position " + pos + " in: " + expr);
        }
        return Double.parseDouble(expr.substring(start, pos));
    }

    private boolean isNumberChar(int index) {
        char c = expr.charAt(index);
        if (Character.isDigit(c) || c == '.') {
            return true;
        }
        if (c == 'e' || c == 'E') {
            return true;
        }
        if ((c == '+' || c == '-') && index > 0) {
            char prev = expr.charAt(index - 1);
            return prev == 'e' || prev == 'E';
        }
        return false;
    }

    private void skipWhitespace() {
        while (pos < expr.length() && Character.isWhitespace(expr.charAt(pos))) {
            pos++;
        }
    }

    private boolean peek(char c) {
        return pos < expr.length() && expr.charAt(pos) == c;
    }

    private char peekAt(int index) {
        return index < expr.length() ? expr.charAt(index) : '\0';
    }

    private boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw new IllegalArgumentException("Expected '" + c + "' at position " + pos + " in: " + expr);
        }
        pos++;
    }
}