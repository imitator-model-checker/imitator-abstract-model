package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Property checked on a model: printed as an IMITATOR {@code .imiprop} ({@link #toIMITATOR()}) or as UPPAAL
 * queries ({@link #toUppaalQueries(UppaalContext)}).
 * <pre>
 * Property p = Property.EF(StatePredicate.loc(monitor, lMatch));            // property := #synth EF(loc[monitor] = l_match);
 * Property q = Property.AGnot(StatePredicate.loc(monitor, lBad)).witness();  // UPPAAL: A[] !(monitor.l_bad)
 * Property v = Property.value(StatePredicate.loc(monitor, lComputed), curComputed);  // value of curComputed there
 * </pre>
 * Only the forms both tools understand are provided: {@code EF}, {@code AG}, {@code AGnot}, {@code AF},
 * {@code DeadlockFree}, and {@link #value} (reachability plus the values of variables in the target states).
 * UPPAAL symbolic queries have no until ({@code E(p) U (q)}).
 */
public final class Property {

    public enum Kind { EF, AG, AGNOT, AF, DEADLOCK_FREE, VALUE }

    /** IMITATOR algorithm mode; UPPAAL ignores it (a query is always a yes/no check). */
    public enum Mode {
        SYNTH("#synth"), WITNESS("#witness"), EXEMPLIFY("#exemplify");

        private final String keyword;

        Mode(String keyword) {
            this.keyword = keyword;
        }
    }

    private final Kind kind;
    private final StatePredicate predicate;   // null for DEADLOCK_FREE
    private final Mode mode;
    private final List<Parameter> projection;
    private final List<VariableType> observed; // VALUE only

    private Property(Kind kind, StatePredicate predicate, Mode mode, List<Parameter> projection,
                     List<VariableType> observed) {
        this.kind = kind;
        this.predicate = predicate;
        this.mode = mode;
        this.projection = List.copyOf(projection);
        this.observed = List.copyOf(observed);
    }

    private Property(Kind kind, StatePredicate predicate) {
        this(kind, predicate, Mode.SYNTH, List.of(), List.of());
    }

    public static Property EF(StatePredicate sp)    { return new Property(Kind.EF, sp); }
    public static Property AG(StatePredicate sp)    { return new Property(Kind.AG, sp); }
    public static Property AGnot(StatePredicate sp) { return new Property(Kind.AGNOT, sp); }
    public static Property AF(StatePredicate sp)    { return new Property(Kind.AF, sp); }
    public static Property deadlockFree()           { return new Property(Kind.DEADLOCK_FREE, null); }

    /**
     * Is {@code target} reachable, and which values do {@code variables} take in the reachable target states?
     * <ul>
     * <li>IMITATOR: {@code property := #synth EF(target);}, the values being read from the accepting states
     * ({@code -states-description-accepting}), as CherLog does for {@code curComputed}.</li>
     * <li>UPPAAL: three queries, {@code E<> target}, {@code sup{target}: v1, v2, ...} and
     * {@code inf{target}: v1, v2, ...}; {@link UppaalResult#value} combines them. Without parameters the log is
     * a single run, so a deterministic monitor gives {@code sup == inf}: the value.</li>
     * </ul>
     * Variables: {@code int}, {@code rational}, {@code bool} (0/1), or a clock <em>stopped</em> in the target
     * location (a running clock has no upper bound: a warning, and {@code sup} is infinite). Queues cannot be read
     * this way (not scalars).
     */
    public static Property value(StatePredicate target, VariableType... variables) {
        if (variables.length == 0) {
            throw new IllegalArgumentException("value(...) needs at least one variable");
        }
        for (VariableType v : variables) {
            if (v instanceof ImiQueue) {
                throw new IllegalArgumentException("queue " + v.getName() + " is not a scalar: its content cannot be read with sup/inf");
            }
        }
        return new Property(Kind.VALUE, target, Mode.SYNTH, List.of(), List.of(variables));
    }

    public Property synth()     { return new Property(kind, predicate, Mode.SYNTH, projection, observed); }
    public Property witness()   { return new Property(kind, predicate, Mode.WITNESS, projection, observed); }
    public Property exemplify() { return new Property(kind, predicate, Mode.EXEMPLIFY, projection, observed); }

    /** {@code projectresult(p1, ...);} (parametric, so IMITATOR only). */
    public Property projectResult(Parameter... parameters) {
        List<Parameter> all = new ArrayList<>(projection);
        all.addAll(List.of(parameters));
        return new Property(kind, predicate, mode, all, observed);
    }

    public Kind getKind() {
        return kind;
    }

    public StatePredicate getPredicate() {
        return predicate;
    }

    public Mode getMode() {
        return mode;
    }

    public List<Parameter> getProjection() {
        return projection;
    }

    /** Variables whose values a {@link #value} property reads (empty for the other kinds). */
    public List<VariableType> getObserved() {
        return observed;
    }

    /** True if the property mentions a parameter ({@code projectresult}, or a parameter whose value is asked). */
    public boolean usesParameters() {
        return !projection.isEmpty() || observed.stream().anyMatch(v -> v instanceof Parameter);
    }

    /** Content of the {@code .imiprop} file. */
    public String toIMITATOR() {
        String formula = switch (kind) {
            case EF, VALUE -> "EF(" + predicate.toIMITATOR() + ")";
            case AG -> "AG(" + predicate.toIMITATOR() + ")";
            case AGNOT -> "AGnot(" + predicate.toIMITATOR() + ")";
            case AF -> "AF(" + predicate.toIMITATOR() + ")";
            case DEADLOCK_FREE -> "DeadlockFree";
        };
        StringBuilder sb = new StringBuilder("property := ").append(mode.keyword).append(" ").append(formula).append(";\n");
        if (!projection.isEmpty()) {
            sb.append("projectresult(")
              .append(projection.stream().map(Parameter::toIMITATOR).collect(Collectors.joining(", ")))
              .append(");\n");
        }
        return sb.toString();
    }

    /** UPPAAL queries (lines of a {@code .q} file): one, or three for {@link #value}. */
    public List<String> toUppaalQueries(UppaalContext ctx) {
        if (!projection.isEmpty()) {
            ctx.problem("property projects the result on parameters " + projection + " (parameter synthesis)");
        }
        return switch (kind) {
            case EF -> List.of("E<> " + predicate.toUppaal(ctx));
            case AG -> List.of("A[] " + predicate.toUppaal(ctx));
            case AGNOT -> List.of("A[] !(" + predicate.toUppaal(ctx) + ")");
            case AF -> List.of("A<> " + predicate.toUppaal(ctx));
            case DEADLOCK_FREE -> List.of("A[] !deadlock");
            case VALUE -> valueQueries(ctx);
        };
    }

    /** The UPPAAL queries joined with newlines (a single query except for {@link #value}). */
    public String toUppaal(UppaalContext ctx) {
        return String.join("\n", toUppaalQueries(ctx));
    }

    private List<String> valueQueries(UppaalContext ctx) {
        String target = predicate.toUppaal(ctx);
        List<String> names = new ArrayList<>();
        for (VariableType v : observed) {
            if (v instanceof Parameter || v instanceof ImiQueue) {
                ctx.problem("the value of " + v.getIMITATORType() + " " + v.getName() + " cannot be read in UPPAAL");
            } else if (v instanceof Clock c && !stoppedIn(c, predicate)) {
                ctx.warning("clock " + v.getName() + " is not stopped in the target " + predicate.toIMITATOR()
                        + ": it keeps growing there, so its sup is infinite (only its inf, the entry value, is useful)");
            }
            names.add(v.toUppaal(ctx));
        }
        String list = String.join(", ", names);
        return List.of("E<> " + target, "sup{" + target + "}: " + list, "inf{" + target + "}: " + list);
    }

    // The target pins a location (alone or in a conjunction) where the clock is stopped (stop{} or flow 0)
    private static boolean stoppedIn(Clock clock, StatePredicate target) {
        if (target instanceof StatePredicate.InLocation in) {
            Location l = in.location();
            return l.getStop().contains(clock)
                    || l.getRate().stream().anyMatch(r -> r.getFirst().equals(clock) && r.getSecond().equals(LinearExpr.ZERO));
        }
        if (target instanceof StatePredicate.And and) {
            return and.children().stream().anyMatch(child -> stoppedIn(clock, child));
        }
        return false;
    }

    @Override
    public String toString() {
        String s = toIMITATOR().trim();
        return kind == Kind.VALUE
                ? s + " (values of " + observed.stream().map(VariableType::getName).collect(Collectors.joining(", ")) + ")"
                : s;
    }
}
