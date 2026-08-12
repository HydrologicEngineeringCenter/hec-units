package cwms.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.hobbyscience.database.Conversion;
import net.hobbyscience.database.methods.ForDB;
import net.hobbyscience.database.methods.Function;
import net.hobbyscience.database.methods.InvLinear;
import net.hobbyscience.database.methods.InvUSGS;
import net.hobbyscience.database.methods.Linear;
import net.hobbyscience.database.methods.USGS;


class ConversionRouteTest {

    private static Map<String, String> routes() throws Exception {
        var loader = new Loader();
        var conversions = new ConversionGraph(loader.getConversions()).generateConversions();
        var routes = new TreeMap<String, String>();
        for (Conversion c : conversions) {
            routes.put(c.getFrom().getAbbreviation() + "," + c.getTo().getAbbreviation(),
                       c.getConversionChain());
        }
        return routes;
    }

    /**
     * Two graphs built from the same data must agree
     */
    @Test
    @DisplayName("the same data produces the same routes every time")
    void routesAreReproducible() throws Exception {
        assertEquals(routes(), routes(),
            "Two graphs built from identical data chose different routes.");
    }

    /**
     * a -> b and b -> a must be mirror images
     */
    @Test
    @DisplayName("every route is the mirror of its inverse")
    void routesAreSymmetric() throws Exception {
        var routes = routes();
        var broken = new ArrayList<String>();

        for (var entry : routes.entrySet()) {
            String[] pair = entry.getKey().split(",");
            String back = routes.get(pair[1] + "," + pair[0]);
            if (back == null) {
                continue;
            }
            var forwardHops = Arrays.asList(entry.getValue().split(" -> "));
            var reverseHops = new ArrayList<>(Arrays.asList(back.split(" -> ")));
            java.util.Collections.reverse(reverseHops);
            if (!forwardHops.equals(reverseHops)) {
                broken.add(entry.getKey() + ": " + entry.getValue() + "  vs  " + back);
            }
        }

        assertTrue(broken.isEmpty(),
            () -> broken.size() + " conversions do not mirror their inverse:"
                + System.lineSeparator() + String.join(System.lineSeparator(), broken));
    }

    @ParameterizedTest(name = "{0} -> {1} routes via {2}")
    @CsvSource({
        "g/l,    lbm/l,   g/l -> kg/l -> lbm/l",
        "lbm/l,  g/l,     lbm/l -> kg/l -> g/l",
        "g/l,    lbm/ft3, g/l -> kg/l -> lbm/ft3",
        "mg/l,   lbm/l,   mg/l -> g/l -> kg/l -> lbm/l",
        "ft/hr,  m/s,     ft/hr -> ft/s -> m/s",
    })
    @DisplayName("routes with lossy alternatives stay pinned")
    void pinnedRoutes(String from, String to, String expectedChain) throws Exception {
        assertEquals(expectedChain, routes().get(from + "," + to),
            "Route selection for " + from + " -> " + to + " changed.");
    }

    @Test
    @DisplayName("equal Units share a hash code")
    void unitHashCodeContract() throws Exception {
        var a = new Unit("Length", "ft", "English", "Feet", "feet", List.of("foot"));
        var b = new Unit("Length", "ft", "English", "Feet", "feet", List.of("foot"));

        assertEquals(a, b, "Units built from identical fields should be equal.");
        assertEquals(a.hashCode(), b.hashCode(),
            "Equal Units must share a hash code or they land in different buckets.");
    }

    @Test
    @DisplayName("equal ConversionMethods share a hash code")
    void conversionMethodHashCodeContract() {
        assertHashContract(new Linear(1.0000001, 0.0), new Linear(1.0000002, 0.0), "Linear");
        assertHashContract(new InvLinear(1.0000001, 0.0), new InvLinear(1.0000002, 0.0), "InvLinear");
        assertHashContract(new USGS(1.0, 2.0, 3.0, 4.0), new USGS(1.0, 2.0, 3.0, 4.0), "USGS");
        assertHashContract(new InvUSGS(1.0, 2.0, 3.0, 4.0), new InvUSGS(1.0, 2.0, 3.0, 4.0), "InvUSGS");
        assertHashContract(new Function("i * 2.0"), new Function("i * 2.0"), "Function");
        assertHashContract(new ForDB("2.0 i *"), new ForDB("2.0 i *"), "ForDB");
    }

    private static void assertHashContract(Object a, Object b, String name) {
        assertEquals(a, b, name + " instances with the same algebra should be equal.");
        assertEquals(a.hashCode(), b.hashCode(),
            "Equal " + name + " instances must share a hash code.");
    }
}
