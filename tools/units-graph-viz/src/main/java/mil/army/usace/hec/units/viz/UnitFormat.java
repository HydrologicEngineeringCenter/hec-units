package mil.army.usace.hec.units.viz;

import mil.army.usace.hec.graph.viz.view.Html;

/** Typesets a unit abbreviation: "m3" becomes m with a superscript 3. */
final class UnitFormat {

    private UnitFormat() {
    }

    /**
     * A run of digits is an exponent only when it directly follows a letter.
     *
     * That single rule separates "m3" and "cfs/mi2", where the digits are powers,
     * from "1000 acre" and "1/ft", where they are part of the name itself.
     */
    static String symbol(String abbreviation) {
        if (abbreviation == null) {
            return "";
        }
        var out = new StringBuilder();
        int i = 0;
        while (i < abbreviation.length()) {
            char c = abbreviation.charAt(i);
            if (Character.isDigit(c) && i > 0 && Character.isLetter(abbreviation.charAt(i - 1))) {
                int start = i;
                while (i < abbreviation.length() && Character.isDigit(abbreviation.charAt(i))) {
                    i++;
                }
                out.append("<sup>").append(Html.escape(abbreviation.substring(start, i))).append("</sup>");
            } else {
                out.append(Html.escape(String.valueOf(c)));
                i++;
            }
        }
        return out.toString();
    }

    /** The typeset symbol wrapped so CSS can style it as a unit rather than prose. */
    static String unit(String abbreviation) {
        return "<span class=\"u\">" + symbol(abbreviation) + "</span>";
    }
}
