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

    /** Constraint printed by IMITATOR in the .res file ("True", "False", or a parameter constraint). */
    static String result(String imi, String imiprop) throws Exception {
        String bin = binary();
        assumeTrue(bin != null, "IMITATOR binary not found (set IMITATOR_BIN)");
        Path dir = Files.createTempDirectory("imi-test");
        Path file = dir.resolve("model.imi");
        Path prop = dir.resolve("model.imiprop");
        Files.writeString(file, imi);
        Files.writeString(prop, imiprop);
        Process p = new ProcessBuilder(bin, file.toString(), prop.toString(), "-no-var-autoremove")
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        Path res = dir.resolve("model.res");
        assertTrue(Files.exists(res), "IMITATOR produced no result:\n" + out + "\n--- model ---\n" + imi);
        String text = Files.readString(res);
        int begin = text.indexOf("BEGIN CONSTRAINT");
        int end = text.indexOf("END CONSTRAINT");
        assertTrue(begin >= 0 && end > begin, "no constraint in the result:\n" + text);
        return text.substring(begin + "BEGIN CONSTRAINT".length(), end).trim();
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
