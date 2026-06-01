package mil.army.usace.hec.units;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import cwms.units.Loader;
import cwms.units.Unit;
import mil.army.usace.hec.units.Parameters;

public class GenUnitsOfMeasure {
    public static void main(String[] args) {
        final var outputDir = new File(args[0]);

        try {
            outputDir.mkdirs();
            final var loader = new Loader();
            final var parameters = Parameters.load();

            final var abstractParameters = loader.getAbstractParameters();
            final var units = loader.getUnits();

            generateDimensions(outputDir, abstractParameters);
            generateUnits(outputDir, units);
            
        } catch (IOException ex) {
            System.err.println("Unable to write unit information.");
            ex.printStackTrace(System.err);
        }
    }


    private static void generateUnits(File outputDir, Map<String, Unit> units) throws IOException {
        try (var writer = new PrintWriter(new File(outputDir, "HecUnits.java"))) {
            writer.println("import tech.units.indriya.unit.UnitDimension;");
            writer.println("import tech.units.indriya.unit.BaseUnit;");
            writer.println("import javax.measure.Dimension;");
            writer.println("import javax.measure.quantity.*;");
            writer.println("import javax.measure.Unit;");
            writer.println();

            writer.println("public class HecUnits {");

            for (var unit: units.values()) {
                final String name = unit.getName();
                final String symbol = unit.getAbbreviation();
                if (unit.getSystem().equalsIgnoreCase("NULL") || unit.getSystem().equalsIgnoreCase("SI")) {
                    writer.println(String.format("\tpublic static final Unit<Length> %s = new BaseUnit<>(\"%s\", \"%s\");",
                                                 name.toUpperCase().replace(" ", "_").replace("-","_").replace("1000", "Thousand"),
                                                 symbol,
                                                 name
                                                ));
                }
            }

            writer.println("}");
            writer.println();
        }
    }


    private static void generateDimensions(File outputDir, List<AbstractParameter> abstractParameters) throws IOException {
        try (var writer = new PrintWriter(new File(outputDir, "HecDimensions.java"))) {
            writer.println("import tech.units.indriya.unit.UnitDimension;");
            writer.println("import javax.measure.Dimension;");
            writer.println();

            writer.println("public class HecDimensions {");
            for (var abstractParameter: abstractParameters) {
                final String name = abstractParameter.name();
                final String symbol = abstractParameter.symbol();
                writer.println(String.format("\tpublic static final Dimension %s = UnitDimension.parse('%s');",
                                                name.toUpperCase().replace(" ", "_"),
                                                symbol.length() == 1 ? symbol : "_"));
                writer.println();
            }

            writer.println("}");
            writer.println("");
        }
    }
}
