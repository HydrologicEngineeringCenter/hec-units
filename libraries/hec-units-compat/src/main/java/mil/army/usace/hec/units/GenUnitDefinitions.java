package mil.army.usace.hec.units;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cwms.units.Loader;
import cwms.units.Unit;
import mil.army.usace.hec.units.Parameter;
import mil.army.usace.hec.units.Parameters;
import net.hobbyscience.database.Conversion;
import net.hobbyscience.database.methods.Linear;


public final class GenUnitDefinitions {
    public static void main(String[] args) {
        final var outputDir = new File(args[0]);

        try {
            outputDir.mkdirs();
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
            writer.println("#position=1000");
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
writer.println("// ';'-delimited; '//' and blank lines ignored.");
  writer.println("// A line with '>' is a conversion, otherwise a unit definition:");
  writer.println("//   Definition:  System;CanonicalName[;Alias;Alias;...]        (System = SI|English)");
  writer.println("//   Conversion:  FromSys;FromUnit>ToSys;ToUnit;Function");
  writer.println("//     Function = scale factor or RPN expression");
  writer.println("//                        where \"ARG 0\" is the input  (e.g. ARG 0|1.8|*|32|+)");



            final var units = loader.getUnits().values();
            final var abstractParameters = loader.getAbstractParameters();
            final var conversions = loader.getConversions();

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

                writer.println("//" + abstractParameter);


                final var currentUnits = units.stream()
                                        .filter(u -> u.getAbstractParameter().equals(abstractParameter))
                                        .toList();

                final Map<String, List<Unit>> unitsToRender = new HashMap<>();

                for (var system: systems) {
                    final var systemUnits = currentUnits.stream()
                                                  .filter(u -> u.getSystem().equals(system))
                                                  .toList();
                    for (var unit: systemUnits) {
                        if (allSystems.contains(system)) {
                            unitsToRender.computeIfAbsent(system, s -> new ArrayList<>()).add(unit);
                        } else {
                            for (var namedSystem: allSystems) {
                                unitsToRender.computeIfAbsent(namedSystem, s -> new ArrayList<>()).add(unit);
                            }
                        }
                    }
                }

                unitsToRender.forEach((system, unitsList) -> {
                    for (var unit: unitsList) {
                        renderUnit(writer, system, unit);
                    }
                });

                writer.println();
                writer.println("//" + abstractParameter + " Conversions");

                unitsToRender.forEach((system, fromUnitsList) -> {
                    final var currentConversions = conversions.stream()
                                                              .filter(c -> fromUnitsList.contains(c.getFrom()))
                                                              .toList();
                    for (var conversion: currentConversions) {
                        renderConversion(writer, conversion, system);
                    }
                    if (currentConversions.isEmpty()) { // render a simple 1.0 conversion
                        for (var otherSystem: allSystems) {
                            if (!otherSystem.equals(system)) {
                                for (var unit: fromUnitsList) {
                                    writer.print(system + ";" + unit.getAbbreviation());
                                    writer.print(">");
                                    writer.print(otherSystem + ";" + unit.getAbbreviation());
                                    writer.println(";1.0");
                                }
                            }
                        }
                    }
                });

                
                
                writer.println();

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

    private static void renderConversion(PrintWriter writer, Conversion conversion, String system) {
        final var from = conversion.getFrom();
        final var to = conversion.getTo();

        final var fromSystemToUse = from.getSystem().equalsIgnoreCase("NULL") ? system : from.getSystem();
        final var toSystemToUse = to.getSystem().equalsIgnoreCase("NULL") ? system : to.getSystem();

        writer.print(fromSystemToUse + ";" + from.getAbbreviation());
        writer.print(">");
        writer.print(toSystemToUse + ";" + to.getAbbreviation());

        final var conv = conversion.getMethod();

        String convStr = "";
        if (conv instanceof Linear linearConv) {
            if (linearConv.getB() == 0.0) {
                convStr = "" + linearConv.getA();
            } else {
                convStr = convertPostfix(linearConv.getPostfix());
            }
        } else {
            convStr = convertPostfix(conv.getPostfix());
        }
        
        writer.print(";" + convStr);    
        writer.println();
    }

    /**
     * Converts the generic postfix format to the expected units def format.
     * @param postfix
     * @return
     */
    private static String convertPostfix(String postfix) {
        return postfix.replaceAll("\\s+", "|").replace("i", "ARG 0");
    }
}
