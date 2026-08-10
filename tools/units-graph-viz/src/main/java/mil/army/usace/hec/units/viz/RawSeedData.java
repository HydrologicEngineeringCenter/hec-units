package mil.army.usace.hec.units.viz;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The conversion data as written, before Loader touches it.

 */
final class RawSeedData {

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
        var mapper = new ObjectMapper();
        mapper.enable(Feature.ALLOW_COMMENTS);

        var rows = new ArrayList<Row>();
        for (JsonNode row : read(mapper, "conversions.json")) {
            if (row.size() >= 3) {
                rows.add(new Row(row.get(0).asText(), row.get(1).asText(),
                                 row.get(2).asText().trim()));
            }
        }

        var constants = new LinkedHashMap<String, String>();
        read(mapper, "conversion_constants.json").fields().forEachRemaining(
            entry -> constants.put(entry.getKey(), entry.getValue().asText()));

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

    private static JsonNode read(ObjectMapper mapper, String name) throws IOException {
        String path = "db/custom/units_and_parameters/" + name;
        try (InputStream in = RawSeedData.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is not on the classpath - is :units a dependency?");
            }
            return mapper.readTree(in);
        }
    }
}
