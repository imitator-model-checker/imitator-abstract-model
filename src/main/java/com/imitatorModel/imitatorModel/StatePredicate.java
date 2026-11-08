package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * State predicate of a {@link Property}: location tests and comparisons over discrete variables,
 * combined with and / or / not. Printed for IMITATOR ({@code loc[monitor] = l_match}) or UPPAAL ({@code monitor.l_match}).
 * <pre>
 * StatePredicate sp = StatePredicate.loc(monitor, lMatch).and(StatePredicate.of(count.ge(2)));
 * </pre>
 */
public sealed interface StatePredicate {

    /** {@code loc[pta] = location} (location must belong to pta). */
    static StatePredicate loc(PTA pta, Location location) {
        if (location.getOwner() != null && !location.getOwner().equals(pta)) {
            throw new IllegalArgumentException(
                "Location " + location.getName() + " belongs to " + location.getOwner().getName() + ", not to " + pta.getName());
        }
        if (location.getOwner() == null && !pta.hasLocation(location.getName())) {
            throw new IllegalArgumentException("Location " + location.getName() + " is not a location of " + pta.getName());
        }
        return new InLocation(pta, location);
    }

    /** Comparison over discrete variables only (IMITATOR rejects clocks and parameters in state predicates). */
    static StatePredicate of(ComplexConstraint constraint) {
        for (VariableType v : constraint.getVariables()) {
            if (v instanceof Clock || v instanceof Parameter) {
                throw new IllegalArgumentException(
                    "State predicates may only use discrete variables, found " + v.getIMITATORType() + " " + v.getName());
            }
        }
        return new Discrete(constraint);
    }

    /** IMITATOR {@code accepting}: some automaton is in an accepting location. */
    static StatePredicate accepting() {
        return new Accepting();
    }

    static StatePredicate constant(boolean value) {
        return new Const(value);
    }

    default StatePredicate and(StatePredicate... others) {
        List<StatePredicate> all = new ArrayList<>();
        all.add(this);
        all.addAll(List.of(others));
        return new And(all);
    }

    default StatePredicate or(StatePredicate... others) {
        List<StatePredicate> all = new ArrayList<>();
        all.add(this);
        all.addAll(List.of(others));
        return new Or(all);
    }

    default StatePredicate not() {
        return new Not(this);
    }

    String toIMITATOR();

    String toUppaal(UppaalContext ctx);

    record InLocation(PTA pta, Location location) implements StatePredicate {
        public String toIMITATOR() {
            return "loc[" + pta.getName() + "] = " + location.nameToIMITATOR();
        }

        public String toUppaal(UppaalContext ctx) {
            return ctx.templateName(pta) + "." + ctx.locationName(pta, location);
        }
    }

    record Discrete(ComplexConstraint constraint) implements StatePredicate {
        public String toIMITATOR() {
            return constraint.toIMITATOR();
        }

        public String toUppaal(UppaalContext ctx) {
            return constraint.toUppaal(ctx);
        }
    }

    record Accepting() implements StatePredicate {
        public String toIMITATOR() {
            return "accepting";
        }

        public String toUppaal(UppaalContext ctx) {
            List<String> locs = new ArrayList<>();
            for (PTA pta : ctx.model().getPTAs()) {
                for (Location l : pta.getLocations()) {
                    if (l.getAccepting() && !l.getIsWaiting()) {
                        locs.add(ctx.templateName(pta) + "." + ctx.locationName(pta, l));
                    }
                }
            }
            return locs.isEmpty() ? "false" : "(" + String.join(" || ", locs) + ")";
        }
    }

    record Const(boolean value) implements StatePredicate {
        public String toIMITATOR() {
            return value ? "True" : "False";
        }

        public String toUppaal(UppaalContext ctx) {
            return value ? "true" : "false";
        }
    }

    record And(List<StatePredicate> children) implements StatePredicate {
        public String toIMITATOR() {
            return children.stream().map(c -> "(" + c.toIMITATOR() + ")").collect(Collectors.joining(" & "));
        }

        public String toUppaal(UppaalContext ctx) {
            return children.stream().map(c -> "(" + c.toUppaal(ctx) + ")").collect(Collectors.joining(" && "));
        }
    }

    record Or(List<StatePredicate> children) implements StatePredicate {
        public String toIMITATOR() {
            return children.stream().map(c -> "(" + c.toIMITATOR() + ")").collect(Collectors.joining(" | "));
        }

        public String toUppaal(UppaalContext ctx) {
            return children.stream().map(c -> "(" + c.toUppaal(ctx) + ")").collect(Collectors.joining(" || "));
        }
    }

    record Not(StatePredicate child) implements StatePredicate {
        public String toIMITATOR() {
            return "not (" + child.toIMITATOR() + ")";
        }

        public String toUppaal(UppaalContext ctx) {
            return "!(" + child.toUppaal(ctx) + ")";
        }
    }
}
