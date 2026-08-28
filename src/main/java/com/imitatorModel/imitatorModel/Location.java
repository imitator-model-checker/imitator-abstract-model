package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Location {
    private String name;
    private ComplexConstraint invariant ;
    private List<Pair<VariableType, LinearExpr>> rate;
    private List<Transition> transitions = new ArrayList<>();
    private Boolean isUrgent ;
    private List<Clock> stop ;
    private Boolean isWaiting ; //for online log only

    public Location(String name, ComplexConstraint invariant, List<Pair<VariableType, LinearExpr>> rate, Boolean isUrgent, Boolean isWaiting) {
        this.name = name;
        this.invariant = (invariant != null) ? invariant : new ConstraintNode(Constraint.TRUE);
        this.rate = (rate != null) ? rate : new ArrayList<>();
        this.isUrgent = (isUrgent != null) ? isUrgent : false;
        this.isWaiting = (isWaiting != null) ? isWaiting : false;
        this.stop = new ArrayList<>();
    }

    public Location(String name) {
        this(name, null, null, null,null);
    }

    public Boolean getIsWaiting() {
        return isWaiting;
    }

    public void setIsWaiting(Boolean isWaiting) {
        this.isWaiting = isWaiting;
    }

    public List<Clock> getStop() {
        return stop;
    }
    
    public void setStop(List<Clock> clocks) {
        this.stop = clocks;
    }

    public void setStop(Clock clock) {
        this.stop.add(clock) ;
    }
    
    public String getName() {
        return name;
    }

    public Boolean getUrgent() {
        return isUrgent;
    }

    public ComplexConstraint getInvariant() {
        return invariant;
    }


    public List<Transition> getTransitions() {
        return transitions;
    }

    public List<Pair<VariableType, LinearExpr>> getRate(){
        return rate;
    }

    public void setInvariant( ComplexConstraint invariant){
        this.invariant = invariant;
    }

    public void setUrgent(Boolean isUrgent) {
        this.isUrgent = isUrgent;
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

    
    public boolean equals(Location o) {
        return this.name.equals(o.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

	public String toIMITATOR(){
        if (isWaiting){
            return "";
        }

        // need this function because imitator guard doesnt support OR under any circumsance, so we need to raise error if we have disjunction in invariant
        if(invariant.haveDisjunction()){
            throw new IllegalStateException("Invariant contains disjunction, which is not supported in IMITATOR");
        }

        StringBuilder sb = new StringBuilder();

        if(isUrgent){
            sb.append("urgent ");
        }

        sb.append("loc " + nameToIMITATOR() + ": invariant " + invariant.toIMITATOR());
        if(!stop.isEmpty()){
            sb.append(" stop{");
            for (int i = 0; i < stop.size(); i++) {
                sb.append(stop.get(i).toIMITATOR());

                if (i < stop.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
        }

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
