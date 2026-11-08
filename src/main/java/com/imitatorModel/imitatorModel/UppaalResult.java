package com.imitatorModel.imitatorModel;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * Verdicts and values read from a verifyta run on a {@link UppaalSpec}, mapped back to the {@link Property}s and
 * to IMITATOR units (time-like values divided by the time scale).
 * <pre>
 * Property v = Property.value(StatePredicate.loc(monitor, lComputed), curComputed);
 * UppaalSpec spec = model.toUppaalSpec(UppaalOptions.defaults(), v);
 * UppaalResult r = UppaalResult.run("verifyta", spec);
 * r.isSatisfied(v);                                       // is l_computed reachable?
 * BigFraction c = r.value(v, curComputed).orElseThrow().single();   // its value there
 * </pre>
 * Values come from {@code sup{target}: v} and {@code inf{target}: v}. A model without parameters has a single log
 * run, so a deterministic monitor gives {@code sup == inf} ({@link Value#isSingle()}); otherwise the bounds of
 * all the values reached are returned. With stopwatches verifyta over-approximates ("MAY be satisfied"):
 * {@link Value#exact()} and {@link #isExact} are then false.
 */
public final class UppaalResult {

    public enum Verdict { SATISFIED, NOT_SATISFIED, MAY_BE_SATISFIED, MAY_NOT_BE_SATISFIED, ABORTED }

    /** One bound printed by a sup/inf query: {@code v <= 7}, {@code v < ∞}, ... */
    record Bound(BigFraction value, boolean strict) {}

    record Outcome(Verdict verdict, List<Bound> bounds) {}

    /**
     * Values of a variable over the reachable target states, in IMITATOR units: {@code min} (from inf) to
     * {@code max} (from sup), each possibly strict or infinite.
     */
    public record Value(BigFraction min, boolean minStrict, BigFraction max, boolean maxStrict, boolean exact) {

        /** A single value: finite, non-strict and {@code min == max}. */
        public boolean isSingle() {
            return !minStrict && !maxStrict && !min.isInfinite() && min.equals(max);
        }

        /** The single value. @throws IllegalStateException if the bounds differ (several values) or are open/infinite. */
        public BigFraction single() {
            if (!isSingle()) {
                throw new IllegalStateException("not a single value: " + this);
            }
            return min;
        }

        @Override
        public String toString() {
            String s = isSingle() ? min.toString()
                    : (minStrict ? "(" : "[") + min + ", " + max + (maxStrict ? ")" : "]");
            return exact ? s : s + " (over-approximation)";
        }
    }

    private final UppaalSpec spec;
    private final List<Outcome> outcomes;
    private final String output;

    private UppaalResult(UppaalSpec spec, List<Outcome> outcomes, String output) {
        this.spec = spec;
        this.outcomes = outcomes;
        this.output = output;
    }

    // ------------------------------------------------------------------
    // Running and parsing
    // ------------------------------------------------------------------

    /** Run {@code verifyta -s -q} on the spec (written to a temporary file, deleted afterwards). */
    public static UppaalResult run(String verifyta, UppaalSpec spec) throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("uppaal");
        Path xml = dir.resolve("model.xml");
        try {
            return run(verifyta, spec, xml);
        } finally {
            Files.deleteIfExists(xml);
            Files.deleteIfExists(dir);
        }
    }

    /** Write the spec to {@code xmlFile} (kept) and run {@code verifyta -s -q} on it. */
    public static UppaalResult run(String verifyta, UppaalSpec spec, Path xmlFile) throws IOException, InterruptedException {
        spec.writeTo(xmlFile, null);
        Process p = new ProcessBuilder(verifyta, "-s", "-q", xmlFile.toString())
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        return parse(spec, out);
    }

    /**
     * Concrete trace of the (first) query of {@code property}, as printed by
     * {@code verifyta -s -q -t1 --query-index i}: a witness for {@code E<>} (e.g. the run reaching the monitor
     * location where the timestamps are recorded), a counterexample for a violated {@code A[]}. The spec must
     * already be written to {@code xmlFile} (see {@link #run(String, UppaalSpec, Path)}). Returns the whole
     * output: the {@code State:} blocks list the locations ({@code ( log_1.l3 monitor.l_detected )}) and the
     * values ({@code sampleTime=7 q.data[0]=2 q.len=1 x=3.5}) in UPPAAL names and units ({@link UppaalSpec#names()},
     * {@link UppaalSpec#timeScale()}).
     */
    public static String trace(String verifyta, UppaalSpec spec, Property property, Path xmlFile)
            throws IOException, InterruptedException {
        int index = spec.queryIndices(property).get(0);
        Process p = new ProcessBuilder(verifyta, "-s", "-q", "-t1", "--query-index", String.valueOf(index),
                xmlFile.toString())
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        return out;
    }

    private static final Pattern FORMULA = Pattern.compile("^Verifying formula (\\d+)\\b");
    private static final Pattern BOUND_HEADER = Pattern.compile("^(sup|inf): .*:$");
    private static final Pattern BOUND_LINE = Pattern.compile("^\\S+ (<=|<|>=|>) (-?∞|-?\\d+)$");

    /** Parse the standard output of {@code verifyta -s -q model.xml}. */
    public static UppaalResult parse(UppaalSpec spec, String verifytaOutput) {
        int expected = spec.queryOwners().size();
        List<Outcome> outcomes = new ArrayList<>();
        Verdict verdict = null;
        List<Bound> bounds = null;
        boolean inBounds = false;

        for (String raw : verifytaOutput.split("\\R")) {
            String line = raw.strip();
            Matcher f = FORMULA.matcher(line);
            if (f.find()) {
                if (bounds != null) {
                    outcomes.add(new Outcome(verdict, bounds));
                }
                verdict = null;
                bounds = new ArrayList<>();
                inBounds = false;
                continue;
            }
            if (bounds == null) {
                continue;
            }
            if (line.contains("Formula is satisfied")) {
                verdict = Verdict.SATISFIED;
            } else if (line.contains("Formula is NOT satisfied")) {
                verdict = Verdict.NOT_SATISFIED;
            } else if (line.contains("MAY NOT be satisfied")) {
                verdict = Verdict.MAY_NOT_BE_SATISFIED;
            } else if (line.contains("MAY be satisfied")) {
                verdict = Verdict.MAY_BE_SATISFIED;
            } else if (line.startsWith("Aborted") || line.contains("-- Aborted")) {
                verdict = Verdict.ABORTED;
            } else if (BOUND_HEADER.matcher(line).matches()) {
                inBounds = true;
            } else if (inBounds) {
                Matcher b = BOUND_LINE.matcher(line);
                if (b.matches()) {
                    bounds.add(new Bound(number(b.group(2)), b.group(1).length() == 1));
                } else {
                    inBounds = false;
                }
            }
        }
        if (bounds != null) {
            outcomes.add(new Outcome(verdict, bounds));
        }
        if (outcomes.size() != expected || outcomes.stream().anyMatch(o -> o.verdict() == null)) {
            throw new IllegalStateException("verifyta output does not match the " + expected + " queries:\n" + verifytaOutput);
        }
        return new UppaalResult(spec, List.copyOf(outcomes), verifytaOutput);
    }

    private static BigFraction number(String s) {
        return switch (s) {
            case "∞" -> BigFraction.INFINITY;
            case "-∞" -> BigFraction.MINUS_INFINITY;
            default -> new BigFraction(new BigInteger(s));
        };
    }

    // ------------------------------------------------------------------
    // Results
    // ------------------------------------------------------------------

    /** Raw verifyta output. */
    public String output() {
        return output;
    }

    /** Verdict of the (first) query of the property; for a {@link Property#value} property, of {@code E<> target}. */
    public Verdict verdict(Property property) {
        return outcomes.get(spec.queryIndices(property).get(0)).verdict();
    }

    /** Satisfied, possibly only in verifyta's over-approximation (MAY): see {@link #isExact}. */
    public boolean isSatisfied(Property property) {
        Verdict v = verdict(property);
        if (v == Verdict.ABORTED) {
            throw new IllegalStateException("verifyta aborted the query of " + property + ":\n" + output);
        }
        return v == Verdict.SATISFIED || v == Verdict.MAY_BE_SATISFIED;
    }

    /** False if verifyta answered MAY (over-approximation, e.g. with stopwatches) to some query of the property. */
    public boolean isExact(Property property) {
        return spec.queryIndices(property).stream()
                .map(i -> outcomes.get(i).verdict())
                .noneMatch(v -> v == Verdict.MAY_BE_SATISFIED || v == Verdict.MAY_NOT_BE_SATISFIED);
    }

    /**
     * Values of {@code variable} in the target states of a {@link Property#value} property, in IMITATOR units;
     * empty if the target is not reachable.
     */
    public Optional<Value> value(Property property, VariableType variable) {
        int k = property.getObserved().indexOf(variable);
        if (property.getKind() != Property.Kind.VALUE || k < 0) {
            throw new IllegalArgumentException(variable.getName() + " is not observed by " + property);
        }
        List<Integer> q = spec.queryIndices(property);    // E<>, sup, inf
        Outcome reach = outcomes.get(q.get(0)), sup = outcomes.get(q.get(1)), inf = outcomes.get(q.get(2));
        for (Outcome o : List.of(reach, sup, inf)) {
            if (o.verdict() == Verdict.ABORTED) {
                throw new IllegalStateException("verifyta aborted a query of " + property + ":\n" + output);
            }
        }
        if (reach.verdict() == Verdict.NOT_SATISFIED || reach.verdict() == Verdict.MAY_NOT_BE_SATISFIED) {
            return Optional.empty();
        }
        Bound high = sup.bounds().get(k), low = inf.bounds().get(k);
        // on an empty set verifyta prints the bounds of the variable's range, reversed (sup = low end, inf = high end)
        if (low.value().compareTo(high.value()) > 0) {
            return Optional.empty();
        }
        boolean exact = isExact(property);
        return Optional.of(new Value(unscale(low.value(), variable), low.strict(),
                unscale(high.value(), variable), high.strict(), exact));
    }

    /** {@link #value} of every observed variable, keyed by IMITATOR name; empty if the target is not reachable. */
    public Map<String, Value> values(Property property) {
        Map<String, Value> values = new LinkedHashMap<>();
        for (VariableType v : property.getObserved()) {
            Optional<Value> value = value(property, v);
            if (value.isEmpty()) {
                return Map.of();
            }
            values.put(v.getName(), value.get());
        }
        return values;
    }

    private BigFraction unscale(BigFraction value, VariableType variable) {
        if (value.isInfinite() || !(variable instanceof Clock || variable instanceof Rational)) {
            return value;
        }
        return value.divide(BigFraction.of(spec.timeScale()));
    }
}
