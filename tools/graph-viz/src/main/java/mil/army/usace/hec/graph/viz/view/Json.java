package mil.army.usace.hec.graph.viz.view;

public final class Json {

    private Json() {
    }

    public static String str(String text) {
        if (text == null) {
            return "null";
        }
        var out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '<') {
                out.append("\\u003c");          // cannot close the enclosing script tag
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }

    public static String num(double value) {
        return Double.isFinite(value) ? Double.toString(value) : "null";
    }
}
