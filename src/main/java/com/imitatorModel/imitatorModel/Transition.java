package com.imitatorModel.imitatorModel;

import java.util.Collections;
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

        StringBuilder sb = new StringBuilder();

        for (ComplexConstraint g : guards) {

            if (g instanceof ConstraintNode c &&
                c.getConstraint() == Constraint.FALSE) {
                continue;
            }

        if (actions.isEmpty()) {
            for (Location dest : destinations) {
                sb.append("\n\twhen ")
                .append(g.toIMITATOR())
                .append(updates.toIMITATOR())
                .append(" goto ")
                .append(dest.nameToIMITATOR())
                .append(";");
            }
        } else {
            for (Action a : actions) {
                for (Location dest : destinations) {
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
