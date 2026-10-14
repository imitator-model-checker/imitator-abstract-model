package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fluent construction of a {@link Transition} from a source location:
 * <pre>
 * l1.on(a).when(t.ge(p), seen.isFalse()).doing(seen.assign(true), x.reset()).goTo(l2);
 * l1.silent().when(t.ge(p)).goTo(l2);
 * </pre>
 * {@link #goTo(Location)} adds the transition to the source location and declares the actions
 * on the source's PTA (IMITATOR rejects a sync on an action the automaton does not declare).
 */
public final class TransitionBuilder {
    private final Location source;
    private final Set<Action> actions;
    private final List<ComplexConstraint> guards = new ArrayList<>();
    private final List<Update> updates = new ArrayList<>();

    TransitionBuilder(Location source, List<Action> actions) {
        this.source = source;
        this.actions = new LinkedHashSet<>(actions);
    }

    /** Add guard conjuncts. Several calls are AND-ed together. */
    public TransitionBuilder when(ComplexConstraint... conditions) {
        guards.addAll(Arrays.asList(conditions));
        return this;
    }

    public TransitionBuilder when(Constraint... conditions) {
        for (Constraint c : conditions) {
            guards.add(new ConstraintNode(c));
        }
        return this;
    }

    /** Add updates, executed in order. */
    public TransitionBuilder doing(Update... updates) {
        this.updates.addAll(Arrays.asList(updates));
        return this;
    }

    public TransitionBuilder doing(List<Update> updates) {
        this.updates.addAll(updates);
        return this;
    }

    public TransitionBuilder doing(ListUpdates updates) {
        return updates == null ? this : doing(updates.getUpdates());
    }

    /** Create the transition to {@code target}, add it to the source location and return it. */
    public Transition goTo(Location target) {
        ComplexConstraint guard = switch (guards.size()) {
            case 0 -> null;
            case 1 -> guards.get(0);
            default -> new AndNode(List.copyOf(guards));
        };
        ListUpdates listUpdates = new ListUpdates(updates);

        Transition transition = actions.size() == 1
                ? new Transition(guard, actions.iterator().next(), listUpdates, target)
                : new Transition(guard, Set.copyOf(actions), listUpdates, Set.of(target));

        source.addTransition(transition);
        source.declareActions(actions);
        return transition;
    }
}
