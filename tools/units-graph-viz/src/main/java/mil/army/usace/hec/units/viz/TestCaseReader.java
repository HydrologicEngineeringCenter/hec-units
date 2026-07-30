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
 * Reads the conversion test cases so a cell can show what was actually tried.
 *
 * The XML report says only whether a pair passed. That answers "did it work" but
 * never "why not" - and a red cell you cannot explain is not much better than no
 * cell at all. The inputs and expected values live only in this CSV, so it gets
 * read directly rather than going through the test report.
 */
final class TestCaseReader {

    private TestCaseReader() {
    }

    /** Test cases grouped by the pair they exercise, in the direction written. */
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
            if (parts.length < 5) {
                continue;
            }
            try {
                var testCase = new TestCase(parts[0].trim(), parts[1].trim(),
                                            Double.parseDouble(parts[2].trim()),
                                            Double.parseDouble(parts[3].trim()),
                                            Double.parseDouble(parts[4].trim()));
                byPair.computeIfAbsent(key(testCase.from(), testCase.to()), k -> new ArrayList<>())
                      .add(testCase);
            } catch (NumberFormatException e) {
                // A malformed row is the test suite's problem to report, not ours.
            }
        }
        return byPair;
    }

    static String key(String from, String to) {
        return from + "\u0000" + to;
    }
}
