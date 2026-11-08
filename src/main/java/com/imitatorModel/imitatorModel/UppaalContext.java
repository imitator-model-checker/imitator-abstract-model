package com.imitatorModel.imitatorModel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * State of one IMITATOR → UPPAAL translation, passed to every {@code toUppaal(ctx)} method.
 * <ul>
 * <li><b>Names.</b> UPPAAL has one global namespace for variables, channels and templates, and a location shadows a
 * global of the same name inside its template (silently: a guard {@code r == 0} then reads the location {@code r}).
 * IMITATOR allows all these to coincide, so names are made unique here; variables keep their name when they can.</li>
 * <li><b>Synchronisation.</b> IMITATOR synchronises an action on <em>every</em> automaton declaring it, updates of the
 * top-most automaton first. Per action: 1 declarer → internal edge; 2 → binary {@code chan}, sender = first declarer
 * (exact); more → {@code broadcast chan} (exact only if the receivers can always take part, else a warning);
 * declared but unused by some declarer → IMITATOR removes the action, so its edges are dropped.</li>
 * <li><b>Scaling.</b> UPPAAL clocks are compared to integers only, so every time-like quantity (clocks, rationals and
 * their constants) is multiplied by {@link #timeScale()}. IMITATOR {@code int}s never meet clocks or rationals and are
 * not scaled. The translation runs twice: the first pass only collects the denominators.</li>
 * <li><b>Problems.</b> Anything UPPAAL cannot express (parameters first of all) is recorded, not thrown, so that
 * {@link ImitatorModel#uppaalProblems} can list every reason; {@link ImitatorModel#toUppaal} throws if any.</li>
 * </ul>
 */
public final class UppaalContext {

    /** Sort of a linear expression: time-like (scaled), integer (not scaled), or constant only. */
    enum Sort { RATIONAL, INT, CONST }

    enum RouteKind { LOCAL, BINARY, BROADCAST, DEAD }

    record Route(RouteKind kind, String sender, String channel) {}

    /**
     * UPPAAL keywords, built-in types/constants/functions and query words: never used as names. Checked with
     * verifyta 5.0.0: {@code log}, {@code exp}, {@code abs}, {@code random}, {@code inf}, {@code control},
     * {@code int32_t}, {@code Pr}, ... are rejected as template/variable/location names; {@code A}, {@code E},
     * {@code M}, {@code tau}, {@code self}, {@code switch}, {@code case} are accepted, so they are not listed.
     */
    static final Set<String> RESERVED = Set.of(
        "chan", "clock", "int", "bool", "double", "const", "urgent", "broadcast", "commit", "init", "process",
        "state", "invariant", "location", "guard", "sync", "assign", "system", "trans", "deadlock", "and", "or",
        "not", "imply", "true", "false", "for", "forall", "exists", "sum", "while", "do", "if", "else", "return",
        "typedef", "struct", "rate", "meta", "priority", "progress", "scalar", "select", "void", "default",
        "string", "hybrid", "before_update", "after_update", "import",
        "Pr", "minE", "maxE", "inf", "sup", "control", "control_t", "simulate", "strategy",
        "under", "saveStrategy", "loadStrategy", "random", "fabs", "fmod", "fmax", "fmin", "exp",
        "exp2", "ln", "log", "log2", "log10", "pow", "sqrt", "cbrt", "hypot", "sin", "cos", "tan", "asin",
        "acos", "atan", "atan2", "sinh", "cosh", "tanh", "erf", "ceil", "floor", "round", "trunc", "fint",
        "abs", "INT8_MIN", "INT8_MAX", "UINT8_MAX", "INT16_MIN", "INT16_MAX", "UINT16_MAX", "INT32_MIN",
        "INT32_MAX", "int8_t", "uint8_t", "int16_t", "uint16_t", "int32_t", "M_PI", "M_E", "DBL_MAX", "DBL_MIN",
        "FLT_MAX", "FLT_MIN");

    /** Prefix of the generated helper names (queue types, conditional updates). */
    static final String HELPER_PREFIX = "imi__";
    static final String QUEUE_CAPACITY = "IMI__QUEUE_CAP";

    /** UPPAAL rejects clock constraints whose bound reaches this magnitude ({@code Out of range clock constraint}). */
    static final BigInteger CLOCK_BOUND_LIMIT = BigInteger.valueOf(1_073_741_822L);

    private final ImitatorModel model;
    private final UppaalOptions options;

    private final Set<String> problems = new LinkedHashSet<>();
    private final Set<String> warnings = new LinkedHashSet<>();
    private final List<String> notes = new ArrayList<>();

    private final Map<String, String> variableNames = new HashMap<>();
    private final Map<String, String> templateNames = new HashMap<>();
    private final Map<String, Map<String, String>> locationNames = new HashMap<>();
    private final Map<String, Map<String, String>> locationIds = new HashMap<>();
    private final Map<String, Route> routes = new LinkedHashMap<>();
    private final Map<String, Map<String, int[]>> positions = new HashMap<>();

    // scaling
    private boolean collecting;
    private BigInteger denominatorLcm = BigInteger.ONE;
    private long timeScale = 1;

    // generated per pass
    private final Set<ImiQueue.ElementType> queueTypes = EnumSet.noneOf(ImiQueue.ElementType.class);
    private final List<String> helperFunctions = new ArrayList<>();

    private UppaalContext(ImitatorModel model, UppaalOptions options) {
        this.model = model;
        this.options = options;
        assignGlobalNames();
        computeRoutes();          // names the channels: globals too
        assignLocationNames();    // last: a location must avoid every global
    }

    // ------------------------------------------------------------------
    // Translation driver
    // ------------------------------------------------------------------

    /** Outcome of a translation: the spec, or null when {@code problems} is not empty. */
    record Result(UppaalSpec spec, List<String> problems) {}

    static Result translate(ImitatorModel model, UppaalOptions options, List<Property> properties) {
        UppaalContext ctx = new UppaalContext(model, options);

        for (Parameter p : model.getUsedParameters()) {
            ctx.problem("parameter " + p.getName() + ": UPPAAL has no parameters (no parameter synthesis)");
        }
        for (Property property : properties) {
            if (property.usesParameters()) {
                ctx.problem("property `" + property + "` projects on parameters");
            }
        }

        // pass 1: collect the denominators of the time-like constants
        ctx.beginPass(true);
        model.renderUppaal(ctx, properties);
        if (options.timeScale() > 0) {
            ctx.timeScale = options.timeScale();
        } else {
            try {
                ctx.timeScale = ctx.denominatorLcm.longValueExact();
            } catch (ArithmeticException e) {
                ctx.problem("time scale " + ctx.denominatorLcm + " does not fit in a long");
            }
        }

        // pass 2: print
        ctx.beginPass(false);
        String[] rendered = model.renderUppaal(ctx, properties);

        if (!ctx.problems.isEmpty()) {
            return new Result(null, List.copyOf(ctx.problems));
        }
        return new Result(new UppaalSpec(rendered[0], rendered[1], ctx.timeScale, List.copyOf(ctx.warnings),
                properties, ctx.queryOwners, ctx.names()), List.of());
    }

    private List<Integer> queryOwners = List.of();

    /** IMITATOR → UPPAAL names chosen for this translation. */
    private UppaalSpec.Names names() {
        Map<String, Map<String, String>> locations = new LinkedHashMap<>();
        for (PTA pta : model.getPTAs()) {
            Map<String, String> m = new LinkedHashMap<>();
            for (Location l : pta.getLocations()) {
                m.put(l.getName(), locationNames.get(pta.getName()).get(l.getName()));
            }
            locations.put(pta.getName(), m);
        }
        Map<String, String> variables = new LinkedHashMap<>();
        for (VariableType v : model.getVariables()) {
            variables.put(v.getName(), variableNames.get(v.getName()));
        }
        Map<String, String> automata = new LinkedHashMap<>();
        for (PTA pta : model.getPTAs()) {
            automata.put(pta.getName(), templateNames.get(pta.getName()));
        }
        return new UppaalSpec.Names(variables, automata, locations);
    }

    void setQueryOwners(List<Integer> owners) {
        this.queryOwners = List.copyOf(owners);
    }

    private void beginPass(boolean collecting) {
        this.collecting = collecting;
        queueTypes.clear();
        helperFunctions.clear();
    }

    // ------------------------------------------------------------------
    // Accessors used by the toUppaal methods
    // ------------------------------------------------------------------

    public ImitatorModel model() {
        return model;
    }

    public UppaalOptions options() {
        return options;
    }

    /** Factor applied to clocks and rationals (1 until the first pass is over). */
    public long timeScale() {
        return timeScale;
    }

    /** Record a reason why the model cannot go to UPPAAL. */
    public void problem(String message) {
        problems.add(message);
    }

    /** Record an inexact translation. */
    public void warning(String message) {
        warnings.add(message);
    }

    List<String> notes() {
        return notes;
    }

    Set<String> warningsView() {
        return warnings;
    }

    String variableName(VariableType v) {
        if (v instanceof Parameter) {
            problem("parameter " + v.getName() + ": UPPAAL has no parameters (no parameter synthesis)");
            return v.getName();
        }
        String name = variableNames.get(v.getName());
        if (name == null) {
            problem("variable " + v.getName() + " is used but not declared in the model");
            return v.getName();
        }
        return name;
    }

    public String templateName(PTA pta) {
        String name = templateNames.get(pta.getName());
        if (name == null) {
            problem("automaton " + pta.getName() + " is not part of the model");
            return pta.getName();
        }
        return name;
    }

    public String locationName(PTA pta, Location location) {
        String name = locationNames.getOrDefault(pta.getName(), Map.of()).get(location.getName());
        if (name == null) {
            problem("location " + location.getName() + " is not a location of automaton " + pta.getName());
            return location.getName();
        }
        return name;
    }

    String locationId(PTA pta, Location location) {
        String id = locationIds.getOrDefault(pta.getName(), Map.of()).get(location.getName());
        if (id == null) {
            problem("location " + location.getName() + " is not a location of automaton " + pta.getName());
            return "missing";
        }
        return id;
    }

    /** Coordinates of a location in the GUI, or null without layout. */
    int[] position(PTA pta, Location location) {
        if (!options.layout()) {
            return null;
        }
        return positions.getOrDefault(pta.getName(), Map.of()).get(location.getName());
    }

    Route route(Action action) {
        return routes.get(action.getName());
    }

    Collection<Route> routes() {
        return routes.values();
    }

    Set<ImiQueue.ElementType> queueTypes() {
        return queueTypes;
    }

    List<String> helperFunctions() {
        return helperFunctions;
    }

    /** Name of the struct type standing for a queue of {@code type} (declared once in the global declarations). */
    String queueType(ImiQueue.ElementType type) {
        queueTypes.add(type);
        return HELPER_PREFIX + type.toUppaal() + "queue_t";
    }

    String queueFunction(ImiQueue.ElementType type, String operation) {
        queueTypes.add(type);
        return HELPER_PREFIX + type.toUppaal() + "queue_" + operation;
    }

    /** Wrap {@code statement} in a generated function {@code void imi__cond_N() { if (cond) { statement; } }} and return its call. */
    String conditionalHelper(String condition, String statement) {
        String name = HELPER_PREFIX + "cond_" + helperFunctions.size();
        helperFunctions.add("void " + name + "() {\n    if (" + condition + ") {\n        " + statement + ";\n    }\n}");
        return name + "()";
    }

    // ------------------------------------------------------------------
    // Numbers
    // ------------------------------------------------------------------

    /** Integer standing for the time-like constant {@code c} (c times the time scale). */
    BigInteger scaled(BigFraction c) {
        if (c.isInfinite()) {
            problem("infinite constant cannot be printed in UPPAAL");
            return BigInteger.ZERO;
        }
        if (collecting) {
            BigInteger d = c.denominator();
            denominatorLcm = denominatorLcm.divide(denominatorLcm.gcd(d)).multiply(d);
            return c.numerator();
        }
        BigFraction s = c.multiply(BigFraction.of(timeScale));
        if (!s.denominator().equals(BigInteger.ONE)) {
            problem("constant " + c + " is not an integer once scaled by the time scale " + timeScale);
            return s.numerator();
        }
        return s.numerator();
    }

    /** Integer constant of an IMITATOR {@code int} expression (not scaled). */
    BigInteger integer(BigFraction c, String where) {
        if (c.isInfinite()) {
            problem("infinite constant in " + where);
            return BigInteger.ZERO;
        }
        if (!c.denominator().equals(BigInteger.ONE)) {
            problem("non-integer constant " + c + " in the int expression " + where);
            return c.numerator();
        }
        return c.numerator();
    }

    /** Check that a constant bound of a clock constraint stays in the range UPPAAL accepts. */
    void checkClockBound(BigInteger value, String where) {
        if (!collecting && value.abs().compareTo(CLOCK_BOUND_LIMIT) >= 0) {
            problem("bound " + value + " of the clock constraint " + where + " is too large for UPPAAL (|bound| < 2^30); lower the time scale");
        }
    }

    void checkIntRange(BigInteger value, String where) {
        if (!collecting && (value.compareTo(BigInteger.valueOf(options.intMin())) < 0
                || value.compareTo(BigInteger.valueOf(options.intMax())) > 0)) {
            problem("value " + value + " of " + where + " is out of the int range [" + options.intMin() + ", "
                    + options.intMax() + "] (UppaalOptions.withIntRange)");
        }
    }

    String intRange() {
        return "int[" + options.intMin() + "," + options.intMax() + "]";
    }

    /** Sort of an expression over {@code variables}; mixing IMITATOR int with clocks/rationals is a problem. */
    Sort sortOf(Collection<VariableType> variables, String where) {
        boolean time = false, integer = false;
        for (VariableType v : variables) {
            if (v instanceof Clock || v instanceof Rational || v instanceof Parameter) {
                time = true;
            } else if (v instanceof Int) {
                integer = true;
            } else {
                problem(v.getIMITATORType() + " " + v.getName() + " cannot appear in the linear expression " + where);
            }
        }
        if (time && integer) {
            problem("expression " + where + " mixes int and clock/rational variables (IMITATOR rejects it too)");
        }
        return time ? Sort.RATIONAL : integer ? Sort.INT : Sort.CONST;
    }

    // ------------------------------------------------------------------
    // Names
    // ------------------------------------------------------------------

    private void assignGlobalNames() {
        Set<String> global = new HashSet<>();

        for (VariableType v : model.getVariables()) {
            variableNames.put(v.getName(), fresh(v.getName(), global));
        }
        for (PTA pta : model.getPTAs()) {
            templateNames.put(pta.getName(), fresh(pta.getName(), global));
        }
        // channels are named in computeRoutes(), after the variables and templates
        this.globalNames = global;
    }

    private void assignLocationNames() {
        Set<String> global = globalNames;
        int id = 0;
        for (PTA pta : model.getPTAs()) {
            Set<String> local = new HashSet<>();
            Map<String, String> names = new HashMap<>();
            Map<String, String> ids = new HashMap<>();
            Map<String, int[]> pos = new HashMap<>();
            for (Location l : pta.getLocations()) {
                int i = pos.size();
                pos.put(l.getName(), new int[] {(i % 4) * 240, (i / 4) * 200});
                // a location must not shadow a global used in the guards of its template
                String name = fresh(l.getName(), local, global);
                names.put(l.getName(), name);
                ids.put(l.getName(), "id" + id++);
            }
            locationNames.put(pta.getName(), names);
            locationIds.put(pta.getName(), ids);
            positions.put(pta.getName(), pos);
        }
    }

    private Set<String> globalNames;

    private static String fresh(String base, Set<String> taken) {
        return fresh(base, taken, Set.of());
    }

    // A legal UPPAAL identifier, not reserved, not in taken nor alsoAvoid; added to taken
    private static String fresh(String base, Set<String> taken, Set<String> alsoAvoid) {
        String name = base.replaceAll("[^A-Za-z0-9_]", "_");
        if (name.isEmpty() || Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        if (name.startsWith(HELPER_PREFIX) || name.startsWith(QUEUE_CAPACITY)) {
            name = "u_" + name;
        }
        String candidate = name;
        int i = 1;
        while (RESERVED.contains(candidate) || taken.contains(candidate) || alsoAvoid.contains(candidate)) {
            candidate = name + "_" + i++;
        }
        taken.add(candidate);
        return candidate;
    }

    // ------------------------------------------------------------------
    // Synchronisation
    // ------------------------------------------------------------------

    private void computeRoutes() {
        Map<String, List<PTA>> declarers = new LinkedHashMap<>();
        for (PTA pta : model.getPTAs()) {
            for (Action a : pta.getActions().stream().sorted((x, y) -> x.getName().compareTo(y.getName())).toList()) {
                declarers.computeIfAbsent(a.getName(), k -> new ArrayList<>()).add(pta);
            }
        }

        for (PTA pta : model.getPTAs()) {
            for (Location l : pta.getLocations()) {
                for (Transition t : l.getTransitions()) {
                    for (Action a : t.getAction()) {
                        if (!pta.getActions().contains(a)) {
                            problem("automaton " + pta.getName() + " synchronises on action " + a.getName()
                                    + " without declaring it (IMITATOR rejects it too)");
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, List<PTA>> entry : declarers.entrySet()) {
            String action = entry.getKey();
            List<PTA> ds = entry.getValue();
            Action a = new Action(action);

            if (ds.size() == 1) {
                routes.put(action, new Route(RouteKind.LOCAL, ds.get(0).getName(), null));
                continue;
            }
            List<String> idle = ds.stream().filter(p -> !uses(p, a)).map(PTA::getName).toList();
            if (!idle.isEmpty()) {
                routes.put(action, new Route(RouteKind.DEAD, null, null));
                notes.add("action " + action + " is declared but unused by " + idle
                        + ": IMITATOR removes it from the whole model, its transitions are dropped");
                continue;
            }
            String channel = fresh(action, globalNames);
            if (ds.size() == 2) {
                routes.put(action, new Route(RouteKind.BINARY, ds.get(0).getName(), channel));
                continue;
            }
            routes.put(action, new Route(RouteKind.BROADCAST, ds.get(0).getName(), channel));
            for (PTA receiver : ds.subList(1, ds.size())) {
                if (!alwaysReceives(receiver, a)) {
                    warning("action " + action + " is shared by " + ds.size() + " automata "
                            + ds.stream().map(PTA::getName).toList() + ": UPPAAL broadcast lets "
                            + ds.get(0).getName() + " take it even when " + receiver.getName()
                            + " cannot, whereas IMITATOR blocks it");
                }
            }
        }
    }

    private static boolean uses(PTA pta, Action a) {
        return pta.getLocations().stream()
                .flatMap(l -> l.getTransitions().stream())
                .anyMatch(t -> t.getAction().contains(a));
    }

    // Every location has an unguarded a-transition into a location without invariant
    private static boolean alwaysReceives(PTA pta, Action a) {
        for (Location l : pta.getLocations()) {
            if (l.getIsWaiting()) {
                continue;
            }
            boolean ok = l.getTransitions().stream().anyMatch(t ->
                    t.getAction().contains(a)
                    && isTrue(t.getGuard())
                    && t.getTo().stream().allMatch(dst -> isTrue(dst.getInvariant())));
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTrue(ComplexConstraint c) {
        return c instanceof ConstraintNode n && n.getConstraint().isTrue();
    }

    // ------------------------------------------------------------------
    // XML
    // ------------------------------------------------------------------

    static String xml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
