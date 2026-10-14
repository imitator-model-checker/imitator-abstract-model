package com.imitatorModel.imitatorModel;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs "imitator model.imi -mode checksyntax" when a binary is available ($IMITATOR_BIN or imitator on PATH), else skips. */
final class ImitatorCheck {
    private ImitatorCheck() {}

    static String binary() {
        String env = System.getenv("IMITATOR_BIN");
        if (env != null && new File(env).canExecute()) {
            return env;
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File f = new File(dir, "imitator");
                if (f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
    }

    static void assertSyntaxOk(String imi) throws Exception {
        String bin = binary();
        assumeTrue(bin != null, "IMITATOR binary not found (set IMITATOR_BIN)");
        Path dir = Files.createTempDirectory("imi-test");
        Path file = dir.resolve("model.imi");
        Files.writeString(file, imi);
        Process p = new ProcessBuilder(bin, file.toString(), "-mode", "checksyntax")
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        assertTrue(out.contains("successfully terminated") && !out.contains("ERROR"),
                "IMITATOR rejected the model:\n" + out + "\n--- model ---\n" + imi);
    }
}
