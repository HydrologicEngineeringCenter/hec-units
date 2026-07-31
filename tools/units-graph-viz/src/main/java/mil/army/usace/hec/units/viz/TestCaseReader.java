package mil.army.usace.hec.units.viz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the test cases so a cell can show what was actually tried.
 */
final class TestCaseReader {

    private TestCaseReader() {
    }

    // Test cases grouped by the pair they exercise, in the direction written
    static Map<String, List<TestCase>> read(Path csv) throws IOException {
        var byPair = new HashMap<String, List<TestCase>>();
        if (csv == null || !Files.isReadable(csv)) {
            return byPair;
        }

        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {          // row 0 is the header
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 6) {
                continue;
            }
            try {
                var testCase = new TestCase(parts[0].trim(), parts[1].trim(),
                                            number(parts[2]), number(parts[3]),
                                            number(parts[4]), number(parts[5]));
                byPair.computeIfAbsent(key(testCase.from(), testCase.to()), k -> new ArrayList<>())
                      .add(testCase);
            } catch (NumberFormatException e) {
                // A malformed row is the test suite's problem to report, not ours.
            }
        }
        return byPair;
    }

    /**
     * Parses one field, tolerating the quoting JUnit's CSV reader also tolerates.
     */
    private static double number(String field) {
        String value = field.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return Double.parseDouble(value);
    }

    static String key(String from, String to) {
        return from + "\u0000" + to;
    }
}
