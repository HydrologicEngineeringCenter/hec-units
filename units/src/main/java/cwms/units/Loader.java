

package cwms.units;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.hobbyscience.database.Conversion;
import net.hobbyscience.database.ConversionMethod;
import net.hobbyscience.database.methods.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Loader {
    

    private ArrayList<String> abstractParameters = new ArrayList<>();
    private Map<String,Unit> unitDefinitions = null;
    private HashSet<Conversion> conversions = new HashSet<>();
    private Map<String,String> constants = null;
   

    public Loader() throws IOException {
        this.loadData();
    }    

    private void loadData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(Feature.ALLOW_COMMENTS);

        abstractParameters = mapper.readValue(
            getData("db/custom/units_and_parameters/abstract_parameters.json"),
            new TypeReference<ArrayList<String>>(){});
        

        unitDefinitions = mapper.readValue(
            getData("db/custom/units_and_parameters/unit_definitions.json"),
            new TypeReference<ArrayList<Unit>>(){})
            .stream()
            .collect(
                Collectors.toMap(
                    Unit::getAbbreviation,
                    Function.identity(),
                    (o1,o2) -> o1,
                    HashMap::new
            ));

        constants = mapper.readValue(
            getData("db/custom/units_and_parameters/conversion_constants.json"),
            new TypeReference<HashMap<String,String>>(){});

        JsonNode tmpConversions = mapper.readTree(
            getData("db/custom/units_and_parameters/conversions.json"));
        HashSet<String> existing = new HashSet<>();
        tmpConversions.forEach(conversion -> {
            System.out.println(conversion.toPrettyString());
            Unit from = unitDefinitions.get(conversion.get(0).asText());
            Unit to = unitDefinitions.get(conversion.get(1).asText());
            if (from != null && to != null) {
                final String tracker = from.getAbbreviation() + "_" + to.getAbbreviation();
                if (existing.contains(tracker)) {
                    throw new UnitException(
                        "Found duplication conversion from " + from.getAbbreviation() + " to " + to.getAbbreviation() + 
                        ". To reduce confusion remove one of the entries."
                    );
                }
                String[] parts = conversion.get(2).asText().split(":");
                String type = parts[0];
                String function = parts[1].trim();
                ConversionMethod method = null;
                if( "linear".equalsIgnoreCase(type)){
                   method = new Linear(substituteVariables(function));
                } else if( "function".equalsIgnoreCase(type)){
                   method = new net.hobbyscience.database.methods.Function(substituteVariables(function));
                } else {
                    throw new UnitException("Invalid conversion method: " + type);
                }

                Conversion c = new Conversion(from,to, method);
                conversions.add(c);
                existing.add(tracker);
            }
            else
            {
                throw new UnitException("Invalid conversion '" + conversion.toPrettyString() + "'. Either the from or to unit is not defined.");
            }

        });
    }

 

    private String substituteVariables(String conversion) {
        String tmp = conversion;
        for (String constant: constants.keySet()) {
            tmp = tmp.replace(constant,constants.get(constant));
        }
        return tmp;
    }

    public HashSet<Conversion> getConversions() {
        return conversions;
    }

    private InputStream getData(String resourceName) {
        return this.getClass().getClassLoader().getResourceAsStream(resourceName);
    }

}