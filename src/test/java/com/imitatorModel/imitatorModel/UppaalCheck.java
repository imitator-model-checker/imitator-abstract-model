package com.imitatorModel.imitatorModel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Runs verifyta ($VERIFYTA_BIN or verifyta on PATH) on a generated model, else skips the test. */
final class UppaalCheck {
    private UppaalCheck() {}

    enum Verdict { SATISFIED, NOT_SATISFIED, MAY_BE_SATISFIED, ABORTED }

    static String binary() {
        String env = System.getenv("VERIFYTA_BIN");
        if (env != null && new File(env).canExecute()) {
            return env;
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File f = new File(dir, "verifyta");
                if (f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /** Verdict of every query embedded in {@code xml}; fails on a syntax/type error. */
    static List<Verdict> verify(String xml) throws Exception {
        String bin = binary();
        assumeTrue(bin != null, "verifyta not found (set VERIFYTA_BIN)");
        Path dir = Files.createTempDirectory("uppaal-test");
        Path file = dir.resolve("model.xml");
        Files.writeString(file, xml);
        Process p = new ProcessBuilder(bin, "-s", "-q", file.toString())
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        assertFalse(out.contains("[error]") && !out.contains("Aborted"),
                "verifyta rejected the model:\n" + out + "\n--- model ---\n" + xml);
        List<Verdict> verdicts = new ArrayList<>();
        for (String line : out.split("\n")) {
            if (line.contains("Formula is satisfied")) verdicts.add(Verdict.SATISFIED);
            else if (line.contains("Formula is NOT satisfied")) verdicts.add(Verdict.NOT_SATISFIED);
            else if (line.contains("MAY be satisfied")) verdicts.add(Verdict.MAY_BE_SATISFIED);
            else if (line.contains("Aborted")) verdicts.add(Verdict.ABORTED);
        }
        return verdicts;
    }
}
