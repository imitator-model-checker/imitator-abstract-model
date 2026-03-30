package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

public class Location {
    private String name;
    private ConjunctionOfConstraints invariant ;
    private List<Pair<VariableType, LinearExpr>> rate;
    private List<Transition> transitions = new ArrayList<>();
    private Boolean isUrgent ;

    public Location(String name, ConjunctionOfConstraints invariant, List<Pair<VariableType, LinearExpr>> rate, Boolean isUrgent) {
        this.name = name;
        this.invariant = (invariant != null) ? invariant : new ConjunctionOfConstraints();
        this.rate = (rate != null) ? rate : new ArrayList<>();
        this.isUrgent = (isUrgent != null) ? isUrgent : false;
    }

    public Location(String name) {
        this(name, null, null, null);
    }

    public Location(String name, ConjunctionOfConstraints invariant) {
        this(name, invariant, null, null);
    }

    public Location(String name, Boolean isUrgent) {
        this(name, null, null, isUrgent);
    }

    public Location(String name, List<Pair<VariableType, LinearExpr>> rate) {
        this(name, null, rate, null);
    }

    public Location(String name, ConjunctionOfConstraints invariant, Boolean isUrgent) {
        this(name, invariant, null, isUrgent );
    }

    public Location(String name, List<Pair<VariableType, LinearExpr>> rate, Boolean isUrgent) {
        this(name, null, rate, null );
    }

    public Location(String name, ConjunctionOfConstraints invariant, List<Pair<VariableType, LinearExpr>> rate) {
        this(name, invariant, rate, null );
    }

    public String getName() {
        return name;
    }

    public Boolean getUrgent() {
        return isUrgent;
    }

    public ConjunctionOfConstraints getInvariant() {
        return invariant;
    }


    public List<Transition> getTransitions() {
        return transitions;
    }

    public List<Pair<VariableType, LinearExpr>> getRate(){
        return rate;
    }

    public void setInvariant( ConjunctionOfConstraints invariant){
        this.invariant = invariant;
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

    public void addRate(VariableType variable, LinearExpr linearTerm) {
        rate.add(new Pair<>(variable, linearTerm));
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
                VariableType variable = rate.get(i).getFirst();
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
