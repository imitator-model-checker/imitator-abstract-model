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

    // public Transition(
    //     ComplexConstraint guard,
    //     List<Action> actions,
    //     ListUpdates updates,
    //     Location to) {
    //         this(guard,actions,updates,List.of(to));
    //     }

    // public Transition(
    //     ComplexConstraint guard,
    //     Action action,
    //     ListUpdates updates,
    //      List<Location>  destinations) {
    //         this(guard,List.of(action),updates,destinations);
    //     }

    // // Only required argument
    // public Transition(Location to) {
    //     this(null, null, null, List.of(to));
    // }
    // public Transition(List<Location> destinations) {
    //     this(null, null, null, destinations);
    // }
    // ///////////////////////////////////////////////////////////////////
    // // Optional combinations (delegating)

    // public Transition(ComplexConstraint guard, Location to) {
    //     this(guard, null, null, List.of(to));
    // }

    // public Transition(ComplexConstraint guard, List<Location> destinations) {
    //     this(guard, null, null, destinations);
    // }
    // ///////////////////////////////////////////////////////////////////

    // public Transition(Action action, Location to) {
    //     this(null, List.of(action), null, List.of(to));
    // }

    // public Transition(Action action, List<Location> destinations) {
    //     this(null, List.of(action), null, destinations);
    // }

    // public Transition(List<Action> actions, Location to) {
    //     this(null, actions, null, List.of(to));
    // }

    // public Transition(List<Action> actions, List<Location> destinations) {
    //     this(null, actions, null, destinations);
    // }
    // ///////////////////////////////////////////////////////////////////

    // public Transition(ListUpdates updates, Location to) {
    //     this(null, null, updates, List.of(to));
    // }

    // public Transition(ListUpdates updates, List<Location> destinations) {
    //     this(null, null, updates, destinations);
    // }
    // ///////////////////////////////////////////////////////////////////

    // public Transition(ComplexConstraint guard, Action action, Location to) {
    //     this(guard, List.of(action), null, List.of(to));
    // }

    // public Transition(ComplexConstraint guard, List<Action> actions, Location to) {
    //     this(guard, actions, null, List.of(to));
    // }

    // public Transition(ComplexConstraint guard, Action action, List<Location> destinations) {
    //     this(guard, List.of(action), null, destinations);
    // }

    // public Transition(ComplexConstraint guard, List<Action> actions, List<Location> destinations) {
    //     this(guard, actions, null, destinations);
    // }
    // ///////////////////////////////////////////////////////////////////

    // public Transition(ComplexConstraint guard, ListUpdates updates, Location to) {
    //     this(guard, null, updates, List.of(to));
    // }

    // public Transition(ComplexConstraint guard, ListUpdates updates,  List<Location> destinations) {
    //     this(guard, null, updates, destinations);
    // }
    // ///////////////////////////////////////////////////////////////////

    // public Transition(Action action, ListUpdates updates, Location to) {
    //     this(null, List.of(action), updates, List.of(to));
    // }

    // public Transition(List<Action> actions, ListUpdates updates, Location to) {
    //     this(null, actions, updates, List.of(to));
    // }
    // ///////////////////////////////////////////////////////////////////



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

            for (Action a : actions) {
                for (Location dest : destinations) {

                    sb.append("\n\twhen ")
                    .append(g.toIMITATOR());

                    if (a != null) {
                        sb.append(" sync ")
                        .append(a.toIMITATOR());
                    }

                    sb.append(updates.toIMITATOR());

                    sb.append(" goto ")
                    .append(dest.nameToIMITATOR())
                    .append(";");
                }
            }
        }

        return sb.toString();
    }
}
