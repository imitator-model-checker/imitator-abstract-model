package com.imitatorModel.imitatorModel;

public class Transition {
    private ConjunctionOfConstraints guard;
    private Action action;
    private ListUpdates updates;
    private Location to;

    // Main constructor (handles defaults)
    public Transition(ConjunctionOfConstraints guard, Action action, ListUpdates updates, Location to) {
        if (to == null) {
            throw new IllegalArgumentException("Location 'to' cannot be null");
        }

        this.guard = (guard != null) ? guard : new ConjunctionOfConstraints(); // optional
        this.action = (action != null) ? action : null;
        this.updates = (updates != null) ? updates : new ListUpdates();
        this.to = to;
    }

    // Only required argument
    public Transition(Location to) {
        this(null, null, null, to);
    }

    // Optional combinations (delegating)

    public Transition(ConjunctionOfConstraints guard, Location to) {
        this(guard, null, null, to);
    }

    public Transition(Action action, Location to) {
        this(null, action, null, to);
    }

    public Transition(ListUpdates updates, Location to) {
        this(null, null, updates, to);
    }

    public Transition(ConjunctionOfConstraints guard, Action action, Location to) {
        this(guard, action, null, to);
    }

    public Transition(ConjunctionOfConstraints guard, ListUpdates updates, Location to) {
        this(guard, null, updates, to);
    }

    public Transition(Action action, ListUpdates updates, Location to) {
        this(null, action, updates, to);
    }



    // public Transition(Action action, Location to) {
    //     this.action = action;
    //     this.to = to;
    //     this.updates = new ArrayList<>();
    //     this.guard = new ConjunctionOfConstraints();
    // }

    // public void addUpdate(Update update){
    //     this.updates.add(update);
    // }

    // public void addConstraint(String constraint){
    //     this.guard.add(constraint);
    // }

    public ConjunctionOfConstraints getGuard() {
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
        // sb.append("\n\twhen " + (guard.toIMITATOR()) + " sync " + action.toIMITATOR());
        sb.append("\n\twhen " + guard.toIMITATOR());

        if (action != null) {
            sb.append(" sync " + action.toIMITATOR());
        }

        sb.append(updates.toIMITATOR());

        sb.append(" goto " + to.nameToIMITATOR() + ";");
        return sb.toString();
	}

}
