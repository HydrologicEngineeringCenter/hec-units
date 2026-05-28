package mil.army.usace.hec.units;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import cwms.units.Loader;
import cwms.units.Unit;


public final class GenUnitDefinitions {
    public static void main(String[] args) {
        System.out.println("Hello wil use " + args[0]);
        final var outputDir = new File(args[0]);



        try {
            final var loader = new Loader();
            final var parameters = Parameters.load();

            generateParameters(outputDir, parameters);
            generateUnitConversions(outputDir, parameters, loader);
        } catch (IOException ex) {
            System.err.println("Unable to write unit information.");
            ex.printStackTrace(System.err);
        }

    }

    private static void generateParameters(File outputDir, List<Parameter> parameters) throws IOException {
        try (PrintWriter writer = new PrintWriter(new File(outputDir, "parameters_units.def"))) {
            writer.println();
            writer.println("=PARAMETER/UNIT");
            writer.println("#position=100");
            writer.println();



            int maxParamNameLen = 0;
            int maxUnitAbbrLen = 0;

            for (var parameter: parameters) {
                if (parameter.id().length() > maxParamNameLen) {
                    maxParamNameLen = parameter.id().length();
                }

                if (parameter.siUnit().length() > maxUnitAbbrLen) {
                    maxUnitAbbrLen = parameter.siUnit().length();
                }
            }

            final var fmt = "%-Xs: %-Ys : %s".replace("X", "" + maxParamNameLen).replace("Y", "" + maxUnitAbbrLen);
            for (var parameter: parameters) {
                writer.println(String.format(fmt, parameter.id(), parameter.siUnit(), parameter.usUnit()));
            }
        }
    }


    private static void generateUnitConversions(File outputDir, List<Parameter> parameters, Loader loader) throws IOException {
        try(var writer = new PrintWriter(new File(outputDir, "unitConversions.def"))) {
            writer.println("// Generated from hec-units" );
            writer.println("// UNIT DEFINITIONS");
            writer.println("//  UnitSystem;UnitName;UnitAliases...;...;");


            final var units = loader.getUnits().values();
            final var abstractParameters = loader.getAbstractParameters();

            var systems = units.stream()
                               .map(u -> u.getSystem())
                               .distinct()
                               .sorted()
                               .toList();
            var allSystems = systems.stream()
                                    .filter(s -> s != null)
                                    .filter(s -> !"NULL".equalsIgnoreCase(s))
                                    .toList();


            for (var abstractParameter: abstractParameters) {
                writer.println();

                writer.println("// " + abstractParameter);


                var currentUnits = units.stream()
                                        .filter(u -> u.getAbstractParameter().equals(abstractParameter))
                                        .toList();

                for (var system: systems) {
                    var systemUnits = currentUnits.stream()
                                                  .filter(u -> u.getSystem().equals(system))
                                                  .toList();
                    for (var unit: systemUnits) {
                        if (allSystems.contains(system)) {
                            renderUnit(writer, system, unit);
                        } else {
                            for (var namedSystem: allSystems) {
                                renderUnit(writer, namedSystem, unit);
                            }
                        }
                    }
                    writer.println();
                }

                writer.println("// " + abstractParameter + " Conversions");

                writer.println();
            }

        }
    }


    private static void renderUnit(PrintWriter writer, String system, Unit unit) {
        writer.print(system + ";" + unit.getAbbreviation());
        for (var alias: unit.getAliases()) {
            writer.print(";" + alias);
        }
        writer.println();
    }
}
