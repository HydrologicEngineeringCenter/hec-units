package mil.army.usace.hec.units;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class Parameters {
    
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(Feature.ALLOW_COMMENTS);
    }

    /**
     * Load parameters from default resource location
     * @return
     * @throws IOException if unable to load or process the parameters data
     */
    public static List<Parameter> load() throws IOException {
        try (var stream = Parameters.class.getResourceAsStream("/db/custom/parameters/base_parameters.json")) {
            return load(stream);
        }
    }

    /**
     * Load parameters from given input stream
     * @param parameterStream
     * @return
     */
    public static List<Parameter> load(InputStream parameterStream) throws IOException {
        final var ret = new ArrayList<Parameter>();
        final var data = mapper.readTree(parameterStream);
        for (var node: data) {
            ret.add(
                new Parameter(
                    node.get(0).asLong(),
                    node.get(1).asText(),
                    node.get(2).asText(),
                    node.get(2).asText(),
                    node.get(3).asText(),
                    node.get(4).asText(),
                    node.get(5).asText(),
                    node.get(6).asText()
            ));
        }

        return ret;
    }

}
