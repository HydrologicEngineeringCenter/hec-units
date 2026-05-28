package mil.army.usace.hec.units;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
        try (var paramStream = Parameters.class.getResourceAsStream("/db/custom/parameters/base_parameters.json");
             var subParamStream = Parameters.class.getResourceAsStream("/db/custom/parameters/initial_sub-parameters.json")) {
            return load(paramStream, subParamStream);
        }
    }

    /**
     * Load parameters from given input stream
     * @param parameterStream
     * @return
     */
    public static List<Parameter> load(InputStream parameterStream, InputStream subParametersStream) throws IOException {
        final var map = new HashMap<String, Parameter>();

        
        final var data = mapper.readTree(parameterStream);
        for (var node: data) {
            final var dbCode = node.get(0).asLong();
            final var param = node.get(1).asText();
            final var paramId = node.get(2).asText();
            map.put(paramId,
                    new Parameter(
                        dbCode,
                        param, // parameter
                        paramId, // id
                        node.get(3).asText(), // friendly name
                        node.get(4).asText(), // storage unit
                        node.get(5).asText(), // si default unit
                        node.get(6).asText(), // us default unit
                        node.get(7).asText() // description
                    )
                );
        }

        final var subParamData = mapper.readTree(subParametersStream);
        for (var node: subParamData) {
            final var baseParamName = node.get(1).asText();
            final var subParamName = node.get(2).asText();

            final var baseParam = map.get(baseParamName);
            if (baseParam == null) {
                throw new IOException("No base parameter named " + baseParamName + " exists in my set. Found in sub parameter " + node.toPrettyString());
            }

            final var name = baseParamName + "-" + subParamName;
            map.put(name,
                new Parameter(
                    node.get(0).asLong(),
                    name,
                    name,
                    name,
                    baseParam.storeUnit(),
                    node.get(4).asText(),
                    node.get(5).asText(),
                    node.get(3).asText()
                )
            );

        }

        return map.values()
                  .stream()
                  .sorted((a,b) -> a.id().compareTo(b.id()))
                  .toList();
    }
}
