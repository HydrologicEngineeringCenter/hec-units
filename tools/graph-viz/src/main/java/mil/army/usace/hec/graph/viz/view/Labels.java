package mil.army.usace.hec.graph.viz.view;

/**
 * Typesets exponents in a label: "m3" reads as m with a raised 3.
 */
public final class Labels {

    private static final String SUPERSCRIPTS = "⁰¹²³⁴"
                                             + "⁵⁶⁷⁸⁹";

    private Labels() {
    }

    public static String html(String label) {
        return build(label, true);
    }

    public static String plain(String label) {
        return build(label, false);
    }

    /**
     * A run of digits is an exponent only when it directly follows a letter.
     */
    private static String build(String label, boolean markup) {
        if (label == null) {
            return "";
        }
        var out = new StringBuilder();
        int i = 0;
        while (i < label.length()) {
            char c = label.charAt(i);
            if (Character.isDigit(c) && i > 0 && Character.isLetter(label.charAt(i - 1))) {
                int start = i;
                while (i < label.length() && Character.isDigit(label.charAt(i))) {
                    i++;
                }
                out.append(raise(label.substring(start, i), markup));
            } else {
                out.append(markup ? Html.escape(String.valueOf(c)) : c);
                i++;
            }
        }
        return out.toString();
    }

    private static String raise(String digits, boolean markup) {
        if (markup) {
            return "<sup>" + digits + "</sup>";
        }
        var out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            out.append(SUPERSCRIPTS.charAt(digits.charAt(i) - '0'));
        }
        return out.toString();
    }
}
