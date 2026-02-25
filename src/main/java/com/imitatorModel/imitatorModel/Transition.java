package com.imitatorModel.imitatorModel;

import java.util.List;

public class Transition {
    private ConjunctionOfConstraints guard;
    private Action action;
    private List<Update> updates;
    private Location to;

    public Transition(ConjunctionOfConstraints guard, Action action, List<Update> updates, Location to) {
        this.guard = guard;
        this.action = action;
        this.updates = updates;
        this.to = to;
    }

    public ConjunctionOfConstraints getGuard() {
        return guard;
    }

    public Action getAction() {
        return action;
    }

    public List<Update> getUpdates() {
        return updates;
    }

    public Location getTo() {
        return to;
    }

    public String toIMITATOR() {

        StringBuilder sb = new StringBuilder();
        sb.append("\n\twhen " + (guard.toIMITATOR()) + " sync " + action.toIMITATOR());
        if(!updates.isEmpty()){
            StringBuilder sb_updates = new StringBuilder();

            for (int i = 0; i < updates.size(); i++) {
                sb_updates.append(updates.get(i).toIMITATOR());

                if (i < updates.size() - 1) {
                    sb_updates.append("; ");
                }
            }
            sb.append(" do {" + sb_updates.toString() + "}");
        }
        sb.append(" goto " + to.nameToIMITATOR() + ";");
        return sb.toString();
	}

}
