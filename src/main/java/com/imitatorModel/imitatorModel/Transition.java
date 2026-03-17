package com.imitatorModel.imitatorModel;

public class Transition {
    private ConjunctionOfConstraints guard;
    private Action action = null;
    private ListUpdates updates;
    private Location to;

    public Transition(ConjunctionOfConstraints guard, Action action, ListUpdates updates, Location to) {
        this.guard = guard;
        this.action = action;
        this.updates = updates;
        this.to = to;
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
