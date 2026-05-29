package mil.army.usace.hec.units;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import cwms.units.Loader;
import mil.army.usace.hec.units.Parameters;

public class GenUnitsOfMeasure {
    public static void main(String[] args) {
        final var outputDir = new File(args[0]);

        try {
            outputDir.mkdirs();
            final var loader = new Loader();
            final var parameters = Parameters.load();

            final var abstractParameters = loader.getAbstractParameters();

            try (var writer = new PrintWriter(new File(outputDir, "HecDimensions.java"))) {
                writer.println("import tech.units.indriya.unit.UnitDimension;");
                writer.println("import javax.measure.Dimension;");
                writer.println();

                writer.println("public class HecDimensions {");
                for (var abstractParameter: abstractParameters) {

                    writer.println(String.format("\tpublic static final Dimension %s = UnitDimension.parse('%s');", abstractParameter.toUpperCase().replace(" ", "_"), abstractParameter.charAt(0)));
                    writer.println();
                }

                writer.println("}");
                writer.println("");
            }
        } catch (IOException ex) {
            System.err.println("Unable to write unit information.");
            ex.printStackTrace(System.err);
        }

    }
}
