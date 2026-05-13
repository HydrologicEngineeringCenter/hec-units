package mil.army.usace.hec.units;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import mil.army.usace.hec.units.Parameters;

public final class GenUnitDefinitions {
    public static void main(String[] args) {
        System.out.println("Hello wil use " + args[0]);
        final var outputDir = new File(args[0]);

        try {
        generateParameters(outputDir);
        } catch (IOException ex) {
            System.err.println("Unable to write unit information.");
            ex.printStackTrace(System.err);
        }

    }

    private static void generateParameters(File outputDir) throws IOException {
        try (PrintWriter writer = new PrintWriter(new File(outputDir, "parameters_units.def"))) {
            writer.println();
            writer.println("=PARAMETER/UNIT");
            writer.println("#position=100");
            writer.println();

            var parameters = Parameters.load();

            

            int maxParamNameLen = 0;
            int maxUnitAbbrLen = 0;

            for (var parameter: parameters) {
                if (parameter.parameter().length() > maxParamNameLen) {
                    maxParamNameLen = parameter.parameter().length();
                }

                if (parameter.siUnit().length() > maxUnitAbbrLen) {
                    maxUnitAbbrLen = parameter.siUnit().length();
                }
            }

            final var fmt = "%-Xs: %-Ys : %s".replace("X", "" + maxParamNameLen).replace("Y", "" + maxUnitAbbrLen);

            for (var parameter: parameters) {
                writer.println(String.format(fmt, parameter.parameter(), parameter.siUnit(), parameter.usUnit()));
            }
        }
    }
}
