package edu.itc.salesreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Architecture test: the core packages must not depend on a UI toolkit.
 * Surefire runs with the project folder as working directory.
 */
class UiIndependenceTest {
    private static final Path ROOT = Path.of("src/main/java/edu/itc/salesreport");

    /** Toolkit-free packages. ui/fx (and ui/fx/legacy) are deliberately absent. */
    private static final List<String> CORE_PACKAGES =
            List.of("model", "ingest", "engine", "ui", "ui/console", "ui/mvp");

    private static final List<String> FORBIDDEN =
            List.of("import javafx.", "import java.awt.", "import javax.swing.");

    @Test
    void corePackagesDoNotImportAToolkit() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        for (String pkg : CORE_PACKAGES) {
            Path dir = ROOT.resolve(pkg);
            if (!Files.isDirectory(dir)) continue;          // ui/mvp arrives in Task 5
            // Files.list (direct children), not Files.walk: ui must not include ui/fx.
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    for (String line : Files.readAllLines(file)) {
                        String trimmed = line.strip();
                        if (FORBIDDEN.stream().anyMatch(trimmed::startsWith)) {
                            offenders.add(file + ": " + trimmed);
                        }
                    }
                }
            }
        }
        assertTrue(scanned > 0, "no sources found: wrong working directory?");
        assertEquals(List.of(), offenders);
    }
}
