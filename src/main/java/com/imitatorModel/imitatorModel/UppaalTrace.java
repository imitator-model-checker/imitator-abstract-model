package com.imitatorModel.imitatorModel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.imitatorModel.bigFraction.BigFraction;

/**
 * Reads values out of a {@code verifyta -t1} trace ({@link UppaalResult#trace}). {@code sup}/{@code inf} only
 * bound scalars, so an IMITATOR queue (a struct {@code {data[CAP]; len}} in UPPAAL) is read here instead, from the
 * last {@code State:} block of the trace.
 *
 * <p>A trace is <b>one concrete run</b> among many. That is exact for the monitor variables of a model without
 * parameters, because the log is a single run and fixes them; it is not canonical for variables that depend on a
 * delay choice (the clocks).
 */
public final class UppaalTrace {

    private static final Pattern DATA = Pattern.compile("^(\\w+)\\.data\\[(\\d+)\\]=(-?\\d+)$");
    private static final Pattern LEN = Pattern.compile("^(\\w+)\\.len=(\\d+)$");

    private UppaalTrace() {}

    /**
     * The content of every queue of {@code model}, in the last state of {@code trace}, keyed by IMITATOR name. Only
     * the first {@code len} elements are meaningful (the rest of the array is 0 padding). Rational queues are divided
     * by {@link UppaalSpec#timeScale()} (UPPAAL stores them scaled); int queues are not; a bool queue gives 0 / 1.
     * Empty when the trace has no state block (target not reachable).
     */
    public static Map<String, List<BigFraction>> queues(ImitatorModel model, UppaalSpec spec, String trace) {
        String values = lastValuesLine(trace);
        if (values == null) {
            return Map.of();
        }
        Map<String, Map<Integer, BigInteger>> data = new LinkedHashMap<>();
        Map<String, Integer> length = new LinkedHashMap<>();
        for (String token : values.trim().split("\\s+")) {
            Matcher d = DATA.matcher(token);
            Matcher l = LEN.matcher(token);
            if (d.matches()) {
                data.computeIfAbsent(d.group(1), k -> new LinkedHashMap<>())
                        .put(Integer.parseInt(d.group(2)), new BigInteger(d.group(3)));
            } else if (l.matches()) {
                length.put(l.group(1), Integer.parseInt(l.group(2)));
            }
        }
        BigFraction scale = BigFraction.of(spec.timeScale());
        Map<String, List<BigFraction>> queues = new LinkedHashMap<>();
        for (VariableType v : model.getVariables()) {
            if (!(v instanceof ImiQueue queue)) {
                continue;
            }
            String uppaalName = spec.names().variables().get(v.getName());
            Integer len = length.get(uppaalName);
            if (len == null) {
                continue;
            }
            boolean timeLike = queue.getElementType() == ImiQueue.ElementType.RATIONAL;
            Map<Integer, BigInteger> elements = data.getOrDefault(uppaalName, Map.of());
            List<BigFraction> items = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                BigFraction x = new BigFraction(elements.getOrDefault(i, BigInteger.ZERO));
                items.add(timeLike ? x.divide(scale) : x);
            }
            queues.put(v.getName(), items);
        }
        return queues;
    }

    /** The line of values (the second line) of the last {@code State:} block, or null. */
    static String lastValuesLine(String trace) {
        String[] lines = trace.split("\\R");
        String found = null;
        for (int i = 0; i + 1 < lines.length; i++) {
            if (lines[i].strip().equals("State:")) {
                String locations = lines[i + 1].strip();
                if (locations.startsWith("(") && locations.endsWith(")")) {
                    found = i + 2 < lines.length ? lines[i + 2] : "";
                }
            }
        }
        return found;
    }
}
