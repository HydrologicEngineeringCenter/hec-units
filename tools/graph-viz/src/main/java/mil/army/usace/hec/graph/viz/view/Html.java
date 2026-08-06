package mil.army.usace.hec.graph.viz.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Markup helpers, so views read as HTML rather than as string concatenation.
 */
public final class Html {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private Html() {
    }

    // Escapes text for element content or a double-quoted attribute
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        // & first, or the ampersands introduced below get escaped again.
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    public static Template fill(String template) {
        return new Template(template);
    }

    public static Tag tag(String name) {
        return new Tag(name);
    }

    // Renders each item and concatenates the results
    public static <T> String each(Collection<T> items, Function<T, String> render) {
        return items.stream().map(render).collect(Collectors.joining());
    }


    public static final class Template {

        private final String source;
        private final Map<String, String> values = new LinkedHashMap<>();

        private Template(String source) {
            this.source = source;
        }

        // Fills a hole with escaped text
        public Template put(String name, Object value) {
            values.put(name, escape(String.valueOf(value)));
            return this;
        }

        // Fills a hole with markup that is already built.
        public Template raw(String name, String markup) {
            values.put(name, markup == null ? "" : markup);
            return this;
        }

        public String render() {
            var out = new StringBuilder();
            var filled = new HashSet<String>();

            Matcher matcher = PLACEHOLDER.matcher(source);
            while (matcher.find()) {
                String name = matcher.group(1);
                String value = values.get(name);
                if (value == null) {
                    throw new IllegalStateException("{{" + name + "}} was never filled");
                }
                filled.add(name);
                String nested = indent(value, indentAt(matcher.start()));
                matcher.appendReplacement(out, Matcher.quoteReplacement(nested));
            }
            matcher.appendTail(out);

            var unused = new ArrayList<>(values.keySet());
            unused.removeAll(filled);
            if (!unused.isEmpty()) {
                throw new IllegalStateException("no placeholder for " + unused);
            }
            return out.toString();
        }

        /**
         * Account for manual string indenting, keeps the generated index html clean
        */
        private String indentAt(int start) {
            String before = source.substring(source.lastIndexOf('\n', start - 1) + 1, start);
            return before.isBlank() ? before : "";
        }

        private static String indent(String markup, String indent) {
            if (indent.isEmpty() || markup.isBlank()) {
                return markup;
            }

            return markup.strip().replace("\n", "\n" + indent);
        }
    }

    // One element. An attribute with a null value is omitted entirely
    public static final class Tag {

        private final String name;
        private final StringBuilder attributes = new StringBuilder();
        private String body = "";

        private Tag(String name) {
            this.name = name;
        }

        public Tag attr(String key, Object value) {
            if (value != null) {
                attributes.append(' ').append(key)
                          .append("=\"").append(escape(oneLine(String.valueOf(value)))).append('"');
            }
            return this;
        }

        private static String oneLine(String value) {
            return value.indexOf('\n') < 0 ? value : value.replaceAll("\\s*\\n\\s*", " ");
        }

        // Sets escaped text content
        public Tag text(Object value) {
            body = value == null ? "" : escape(String.valueOf(value));
            return this;
        }

        // Sets content that is already markup
        public Tag html(String markup) {
            body = markup == null ? "" : markup;
            return this;
        }

        @Override
        public String toString() {
            return "<" + name + attributes + ">" + body + "</" + name + ">";
        }
    }
}
