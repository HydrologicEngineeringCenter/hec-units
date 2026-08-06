package mil.army.usace.hec.units.viz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The conversion data as written, before Loader touches it.
 */
final class RawSeedData {

    private static final Pattern ROW = Pattern.compile(
        "\\[\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*]");
    private static final Pattern ENTRY = Pattern.compile(
        "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");

    // One row of conversions.json: from, to, and the method text as written
    record Row(String from, String to, String method) {
    }

    private final List<Row> rows;
    private final Map<String, String> constants;

    private RawSeedData(List<Row> rows, Map<String, String> constants) {
        this.rows = rows;
        this.constants = constants;
    }

    static RawSeedData load() throws IOException {
        var rows = new ArrayList<Row>();
        Matcher rowMatcher = ROW.matcher(
            resource("db/custom/units_and_parameters/conversions.json"));
        while (rowMatcher.find()) {
            rows.add(new Row(rowMatcher.group(1), rowMatcher.group(2),
                             rowMatcher.group(3).trim()));
        }

        var constants = new LinkedHashMap<String, String>();
        Matcher entryMatcher = ENTRY.matcher(
            resource("db/custom/units_and_parameters/conversion_constants.json"));
        while (entryMatcher.find()) {
            constants.putIfAbsent(entryMatcher.group(1), entryMatcher.group(2));
        }

        if (rows.isEmpty() || constants.isEmpty()) {
            throw new IOException("could not read the conversion data from the classpath");
        }
        return new RawSeedData(rows, constants);
    }

    List<Row> rows() {
        return rows;
    }

    Map<String, String> constants() {
        return constants;
    }

    // Reads a :units resource with comments stripped, so commented-out rows are excluded
    private static String resource(String path) throws IOException {
        try (InputStream in = RawSeedData.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is not on the classpath - is :units a dependency?");
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return text.replaceAll("(?s)/\\*.*?\\*/", "")
                       .replaceAll("(?m)//.*$", "");
        }
    }
}
