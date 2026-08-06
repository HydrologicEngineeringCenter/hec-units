package mil.army.usace.hec.graph.viz.view;

/**
 * Typesets exponents in a label: "m3" reads as m with a raised 3.
 *
 * Two forms, because the page draws labels two different ways. Markup is
 * better wherever HTML reaches; a canvas or an attribute value cannot carry
 * a tag, so those take the single-character form instead.
 */
public final class Labels {

    private static final String SUPERSCRIPTS = "⁰¹²³⁴"
                                             + "⁵⁶⁷⁸⁹";

    private Labels() {
    }

    /** For markup: m3 becomes m&lt;sup&gt;3&lt;/sup&gt;, escaped and ready to insert. */
    public static String html(String label) {
        return build(label, true);
    }

    /** For a canvas label or an attribute value: m3 becomes m³. */
    public static String plain(String label) {
        return build(label, false);
    }

    /**
     * A run of digits is an exponent only when it directly follows a letter.
     *
     * That single rule separates "m3" and "cfs/mi2", where the digits are
     * powers, from "1000 acre" and "1/ft", where they are part of the name.
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
