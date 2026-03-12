package com.imitatorModel.imitatorModel;

import java.util.List;
import java.util.ArrayList;

import javax.swing.SpringLayout.Constraints;

public class Transition {
    private ConjunctionOfConstraints guard;
    private Action action = null;
    private List<Update> updates;
    private Location to;

    public Transition(ConjunctionOfConstraints guard, Action action, List<Update> updates, Location to) {
        this.guard = guard;
        this.action = action;
        this.updates = updates;
        this.to = to;
    }


    public Transition(Action action, Location to) {
        this.action = action;
        this.to = to;
        this.updates = new ArrayList<>();
        this.guard = new ConjunctionOfConstraints();
    }

    public void addUpdate(Update update){
        this.updates.add(update);
    }

    public void addConstraint(String constraint){
        this.guard.add(constraint);
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
        // sb.append("\n\twhen " + (guard.toIMITATOR()) + " sync " + action.toIMITATOR());
        sb.append("\n\twhen " + guard.toIMITATOR());

        if (action != null) {
            sb.append(" sync " + action.toIMITATOR());
        }
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
