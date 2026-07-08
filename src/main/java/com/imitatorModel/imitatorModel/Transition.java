package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.imitatorModel.imitatorModel.ComplexConstraint.LogicalOperator;

public class Transition {
    private ComplexConstraint guard;
    private Action action;
    private ListUpdates updates;
    private Location to;

    // Main constructor (handles defaults)
    public Transition(ComplexConstraint guard, Action action, ListUpdates updates, Location to) {
        if (to == null) {
            throw new IllegalArgumentException("Location 'to' cannot be null");
        }

        this.guard = (guard != null) ? guard : new ConstraintNode(Constraint.TRUE); // optional
        this.action = (action != null) ? action : null;
        this.updates = (updates != null) ? updates : new ListUpdates();
        this.to = to;
    }

    // Only required argument
    public Transition(Location to) {
        this(null, null, null, to);
    }

    // Optional combinations (delegating)

    public Transition(ComplexConstraint guard, Location to) {
        this(guard, null, null, to);
    }

    public Transition(Action action, Location to) {
        this(null, action, null, to);
    }

    public Transition(ListUpdates updates, Location to) {
        this(null, null, updates, to);
    }

    public Transition(ComplexConstraint guard, Action action, Location to) {
        this(guard, action, null, to);
    }

    public Transition(ComplexConstraint guard, ListUpdates updates, Location to) {
        this(guard, null, updates, to);
    }

    public Transition(Action action, ListUpdates updates, Location to) {
        this(null, action, updates, to);
    }



    public ComplexConstraint getGuard() {
        return guard;
    }

    public Action getAction() {
        return action;
    }

    public ListUpdates getUpdates() {
        return updates;
    }

    public Location getTo() {
        return to;
    }



    public String toIMITATOR() {
        StringBuilder sb = new StringBuilder();
        // Imitator doesnt support having disjunction in the guards
        // if a guard has disjunction, then split it into multiple transitions, each with one of the disjunct as guard, and the same action, updates, and to location
        // if (this.guard.haveDisjunction()) {
        //     List<Constraint> constraints = this.guard.getConstraints();
        //     List<LogicalOperator> operators = this.guard.getOperators();


        //     Set<Constraint> sharedAndConstraints = new HashSet<Constraint>();
        //     Set<Constraint> orConstraints = new HashSet<Constraint>();


        //     // // First constraint
        //     // sharedAndConstraints.add(constraints.get(0));


        //     for (int i = 0; i < operators.size(); i++) {


        //         LogicalOperator op = operators.get(i);
        //         Constraint curConstraint = constraints.get(i );
        //         Constraint nextConstraint = constraints.get(i + 1);


        //         if (op == LogicalOperator.OR) {
        //             orConstraints.add(curConstraint);
        //             orConstraints.add(nextConstraint);
        //         } else if (op == LogicalOperator.AND) {
        //             sharedAndConstraints.add(curConstraint);
        //         }  
        //     }
            
        //         // Create one transition for each OR constraint
        //     for (Constraint orConstraint : orConstraints) {

        //         List<Constraint> combined = new ArrayList<>(sharedAndConstraints);
        //         combined.add(orConstraint);

        //         ComplexConstraint newGuard = new ComplexConstraint(combined);

        //         Transition newTransi = new Transition(
        //                 newGuard,
        //                 this.action,
        //                 this.updates,
        //                 this.to
        //         );

        //         sb.append(newTransi.toIMITATOR());
        //     }
        // }

        if (this.guard.haveDisjunction()) {

            ComplexConstraint dnfGuard = this.guard.toDNF();

            List<ComplexConstraint> guards = dnfGuard.splitDisjunction();

            for (ComplexConstraint guard : guards) {

                Transition newTransi = new Transition(
                        guard,
                        this.action,
                        this.updates,
                        this.to
                );

                sb.append(newTransi.toIMITATOR());
            }

        }     

        // sb.append("\n\twhen " + (guard.toIMITATOR()) + " sync " + action.toIMITATOR());
        else {
            if (guard instanceof ConstraintNode c &&
                c.getConstraint() == Constraint.FALSE) {
                return ""; // do nothing
            }

            sb.append("\n\twhen " + guard.toIMITATOR());

            if (action != null) {
                sb.append(" sync " + action.toIMITATOR());
            }

            sb.append(updates.toIMITATOR());

            sb.append(" goto " + to.nameToIMITATOR() + ";");
        }

        return sb.toString();
	}

}
