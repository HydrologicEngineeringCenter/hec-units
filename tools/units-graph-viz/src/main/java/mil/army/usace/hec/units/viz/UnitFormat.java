package mil.army.usace.hec.units.viz;

import mil.army.usace.hec.graph.viz.view.Labels;

/** Typesets a unit abbreviation: "m3" becomes m with a superscript 3. */
final class UnitFormat {

    private UnitFormat() {
    }

    static String symbol(String abbreviation) {
        return Labels.html(abbreviation);
    }

    /** The typeset symbol wrapped so CSS can style it as a unit rather than prose. */
    static String unit(String abbreviation) {
        return "<span class=\"u\">" + symbol(abbreviation) + "</span>";
    }
}
