package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Transition {
    private ComplexConstraint guard;
    private final Set<Location> destinations;
    private final Set<Action> actions;
    private ListUpdates updates;

    // Main constructor (handles defaults)
    public Transition(
            ComplexConstraint guard,
            Set<Action> actions,
            ListUpdates updates,
            Set<Location> destinations) {

        this.guard = (guard != null)
                ? guard
                : new ConstraintNode(Constraint.TRUE);

        this.actions = (actions != null)
                ? actions
                : Collections.emptySet();

        this.updates = (updates != null)
                ? updates
                : new ListUpdates();

        this.destinations = (destinations != null)
                ? destinations
                : Collections.emptySet();
    }

    public Transition(
            ComplexConstraint guard,
            Action action,
            ListUpdates updates,
            Location to) {

        this(
            guard,
            action != null ? Set.of(action) : Collections.emptySet(),
            updates,
            to != null ? Set.of(to) : Collections.emptySet()
        );
    }

    public ComplexConstraint getGuard() {
        return guard;
    }

    public Set<Action> getAction() {
        return actions;
    }

    public ListUpdates getUpdates() {
        return updates;
    }

    public Set<Location> getTo() {
        return destinations;
    }

    /**
     * UPPAAL {@code <transition>} elements from {@code source} (one per action, destination and disjunct, as for
     * IMITATOR). The sync label follows the route of the action ({@link UppaalContext}): none for an action of a
     * single automaton, {@code a!} for the first declarer, {@code a?} for the others; edges on an action that
     * IMITATOR removes are dropped. A disjunctive guard over clocks is split (UPPAAL accepts disjunctions only
     * over discrete variables).
     */
    public List<String> toUppaal(UppaalContext ctx, Location source) {
        PTA pta = source.getOwner();
        List<ComplexConstraint> guards;
        if (guard.haveDisjunction() && guard.usesClocks()) {
            ComplexConstraint dnf = guard.toDNF();
            guards = dnf.haveDisjunction() ? dnf.splitDisjunction() : List.of(dnf);
        } else {
            guards = List.of(guard);
        }
        List<String> guardTexts = new ArrayList<>();
        for (ComplexConstraint g : guards) {
            String text = g.toUppaal(ctx);
            if (!text.equals("false")) {
                guardTexts.add(text.equals("true") ? "" : text);
            }
        }

        List<Location> sortedDestinations = destinations.stream()
                .sorted(Comparator.comparing(Location::getName))
                .toList();
        List<String> syncs = new ArrayList<>();
        if (actions.isEmpty()) {
            syncs.add("");
        } else {
            for (Action a : actions.stream().sorted(Comparator.comparing(Action::getName)).toList()) {
                UppaalContext.Route route = ctx.route(a);
                if (route == null || route.kind() == UppaalContext.RouteKind.DEAD) {
                    continue;   // undeclared (problem already recorded) or removed by IMITATOR
                }
                if (route.kind() == UppaalContext.RouteKind.LOCAL) {
                    syncs.add("");
                } else {
                    syncs.add(route.channel() + (route.sender().equals(pta.getName()) ? "!" : "?"));
                }
            }
        }
        if (guardTexts.isEmpty() || syncs.isEmpty()) {
            return List.of();
        }
        // one helper function per conditional update: print the updates once, reuse the text on every edge
        String assignment = updates.toUppaal(ctx);

        List<String> edges = new ArrayList<>();
        for (String g : guardTexts) {
            for (String sync : syncs) {
                for (Location dest : sortedDestinations) {
                    edges.add(edge(ctx, pta, source, dest, g, sync, assignment));
                }
            }
        }
        return edges;
    }

    private static String edge(UppaalContext ctx, PTA pta, Location source, Location dest,
                               String guard, String sync, String assignment) {
        int[] p = ctx.position(pta, source);
        int[] q = ctx.position(pta, dest);
        boolean selfLoop = source.equals(dest);
        int[] mid = null;
        if (p != null && q != null) {
            mid = selfLoop ? new int[] {p[0], p[1] - 70} : new int[] {(p[0] + q[0]) / 2, (p[1] + q[1]) / 2};
        }
        StringBuilder sb = new StringBuilder("\t\t<transition>\n");
        sb.append("\t\t\t<source ref=\"").append(ctx.locationId(pta, source)).append("\"/>\n");
        sb.append("\t\t\t<target ref=\"").append(ctx.locationId(pta, dest)).append("\"/>\n");
        int line = 0;
        if (!guard.isEmpty()) {
            sb.append(label("guard", guard, mid, line++));
        }
        if (!sync.isEmpty()) {
            sb.append(label("synchronisation", sync, mid, line++));
        }
        if (!assignment.isEmpty()) {
            sb.append(label("assignment", assignment, mid, line++));
        }
        if (selfLoop && p != null) {
            sb.append("\t\t\t<nail x=\"").append(p[0] - 30).append("\" y=\"").append(p[1] - 60).append("\"/>\n");
            sb.append("\t\t\t<nail x=\"").append(p[0] + 30).append("\" y=\"").append(p[1] - 60).append("\"/>\n");
        }
        sb.append("\t\t</transition>\n");
        return sb.toString();
    }

    private static String label(String kind, String text, int[] mid, int line) {
        String xy = mid == null ? "" : " x=\"" + mid[0] + "\" y=\"" + (mid[1] - 17 * (3 - line)) + "\"";
        return "\t\t\t<label kind=\"" + kind + "\"" + xy + ">" + UppaalContext.xml(text) + "</label>\n";
    }

    public String toIMITATOR() {
    // Imitator doesnt support having disjunction in the guards
    // if a guard has disjunction, then split it into multiple transitions, each with one of the disjunct as guard, and the same action, updates, and to location
        ComplexConstraint dnfGuard = guard.toDNF();

        List<ComplexConstraint> guards;

        if (dnfGuard.haveDisjunction()) {
            guards = dnfGuard.splitDisjunction();
        } else {
            guards = List.of(dnfGuard);
        }

        // Sorted by name so that the output does not depend on Set iteration order
        List<Action> sortedActions = actions.stream()
                .sorted(Comparator.comparing(Action::getName))
                .toList();
        List<Location> sortedDestinations = destinations.stream()
                .sorted(Comparator.comparing(Location::getName))
                .toList();

        StringBuilder sb = new StringBuilder();

        for (ComplexConstraint g : guards) {

            if (g instanceof ConstraintNode c &&
                c.getConstraint().isFalse()) {
                continue;
            }

        if (actions.isEmpty()) {
            for (Location dest : sortedDestinations) {
                sb.append("\n\twhen ")
                .append(g.toIMITATOR())
                .append(updates.toIMITATOR())
                .append(" goto ")
                .append(dest.nameToIMITATOR())
                .append(";");
            }
        } else {
            for (Action a : sortedActions) {
                for (Location dest : sortedDestinations) {
                    sb.append("\n\twhen ")
                    .append(g.toIMITATOR())
                    .append(" sync ")
                    .append(a.toIMITATOR())
                    .append(updates.toIMITATOR())
                    .append(" goto ")
                    .append(dest.nameToIMITATOR())
                    .append(";");
                }
            }
        }
        }

        return sb.toString();
    }
}
