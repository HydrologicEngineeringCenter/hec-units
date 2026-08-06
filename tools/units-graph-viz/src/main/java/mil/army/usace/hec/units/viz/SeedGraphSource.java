package mil.army.usace.hec.units.viz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

import cwms.units.Loader;
import mil.army.usace.hec.graph.viz.formula.AffineForm;
import mil.army.usace.hec.graph.viz.formula.FormulaRenderer;
import mil.army.usace.hec.graph.viz.model.Edge;
import mil.army.usace.hec.graph.viz.model.Graph;
import mil.army.usace.hec.graph.viz.model.Node;

/**
 * The pre-algorithm graph: the hand-written conversions exactly as authored,
 * before ConversionGraph derives anything.
 */
public final class SeedGraphSource {

    private SeedGraphSource() {
    }

    public static Graph load(Loader loader) throws IOException {
        RawSeedData raw = RawSeedData.load();

        var units = loader.getUnits();
        var names = new HashMap<String, String>();
        units.forEach((abbreviation, unit) -> names.put(abbreviation, unit.getName()));

        var nodeIds = new LinkedHashSet<String>();
        var edges = new ArrayList<Edge>();
        var skipped = new ArrayList<String>();

        for (RawSeedData.Row row : raw.rows()) {
            if (!units.containsKey(row.from()) || !units.containsKey(row.to())) {
                skipped.add(row.from() + " -> " + row.to());
                continue;
            }
            nodeIds.add(row.from());
            nodeIds.add(row.to());

            String kind = row.method().contains(":")
                ? row.method().substring(0, row.method().indexOf(':')).trim().toLowerCase(Locale.ROOT)
                : "function";

            String symbolic = FormulaRenderer.symbolic(row.method());
            AffineForm form = FormulaRenderer.affineOf(
                FormulaRenderer.substitute(symbolic, raw.constants()).expression());

            String label = kind + "|" + (form == null ? "" : form.m())
                                + "|" + (form == null ? "" : form.b());
            edges.add(new Edge(row.from(), row.to(), null, label,
                SeedFormula.render(row.method(), row.from(), row.to(), raw.constants(), names)));
        }

        if (!skipped.isEmpty()) {
            System.err.println("warning: skipped " + skipped.size()
                + " direct conversions naming undefined units: " + skipped);
        }

        var nodes = new ArrayList<Node>();
        for (String id : nodeIds) {
            var unit = units.get(id);
            nodes.add(new Node(id, unit.getName(), unit.getAbstractParameter(), unit.getSystem()));
        }
        return new Graph(nodes, edges);
    }
}
