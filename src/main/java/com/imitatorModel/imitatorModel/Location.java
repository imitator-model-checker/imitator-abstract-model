package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

public class Location {
    private String name;
    private ConjunctionOfConstraints invariant;
    private List<Pair<Variable, LinearExpr>> rate;
    private List<Transition> transitions;
    private Boolean isUrgent;

    public Location(String name) {
        this.name = name;
        this.invariant = new ConjunctionOfConstraints();
        this.transitions = new ArrayList<Transition>();
        this.rate = new ArrayList<>();
        isUrgent = false;
    }

    public String getName() {
        return name;
    }

    public Boolean getUrgent() {
        return isUrgent;
    }

    public void setInvariant( ConjunctionOfConstraints invariant){
        this.invariant = invariant;
    }

    public ConjunctionOfConstraints getInvariant() {
        return invariant;
    }

    public void setUrgent() {
        this.isUrgent = true;
    }

    public void addTransition(Transition transition) {
        transitions.add(transition);
    }

    public void addTransitions(List<Transition> transition) {

        transitions.addAll(transitions);
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void addRate(Variable variable, LinearExpr linearTerm) {
        rate.add(new Pair<>(variable, linearTerm));
    }

    public List<Pair<Variable, LinearExpr>> getRate() {
        return rate;
    }

	public String nameToIMITATOR(){
		return name;
	}

	public String toIMITATOR(){
        StringBuilder sb = new StringBuilder();

        if(isUrgent){
            sb.append("urgent ");
        }

        sb.append("loc " + nameToIMITATOR() + ": invariant " + invariant.toIMITATOR());
        if(!rate.isEmpty()){
            sb.append(" flow{");
            for (int i = 0; i < rate.size(); i++) {
                Variable variable = rate.get(i).getFirst();
                LinearExpr lt = rate.get(i).getSecond();
                sb.append(variable.toIMITATOR() + "' = " + lt.toIMITATOR());

                if (i < rate.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
        }

		for (Transition transition : transitions) {
            sb.append(transition.toIMITATOR()).append("\n");  // Adding a newline after each PTA for readability
        }
        return sb.toString().trim();  // Remove the last newline
	}
}
