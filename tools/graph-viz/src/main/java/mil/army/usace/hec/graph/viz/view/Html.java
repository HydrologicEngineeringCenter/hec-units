package mil.army.usace.hec.graph.viz.view;

public final class Html {

    /**
     * Escapes text for element content or a double-quoted attribute.
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}