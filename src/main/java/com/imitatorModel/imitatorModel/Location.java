package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Location {
    private String name;
    private ComplexConstraint invariant ;
    private List<Pair<VariableType, LinearExpr>> rate;
    private List<Transition> transitions = new ArrayList<>();
    private Boolean isUrgent ;
    private Boolean isAccepting;
    private List<Clock> stop ;
    private Boolean isWaiting ; //for online log only

    private PTA owner;                                            // set by PTA.addLocation
    private final Set<Action> actionsToDeclare = new LinkedHashSet<>(); // used by the builder before the location has an owner

    public Location(String name, ComplexConstraint invariant, List<Pair<VariableType, LinearExpr>> rate, Boolean isUrgent, Boolean isWaiting, Boolean isAccepting) {
        this.name = name;
        this.invariant = (invariant != null) ? invariant : new ConstraintNode(Constraint.TRUE);
        this.rate = (rate != null) ? rate : new ArrayList<>();
        this.isUrgent = (isUrgent != null) ? isUrgent : false;
        this.isWaiting = (isWaiting != null) ? isWaiting : false;
        this.isAccepting = (isAccepting != null) ? isAccepting : false;
        this.stop = new ArrayList<>();
    }

    public Location(String name) {
        this(name, null, null, null,null,null);
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

    public void addTransitions(List<Transition> transitions) {
        this.transitions.addAll(transitions);
    }

    public void addRate(VariableType variable, LinearExpr linearTerm) {
        rate.add(new Pair<>(variable, linearTerm));
    }

    public void setAccepting(Boolean isAccepting) {
        this.isAccepting = isAccepting;
    }

    public Boolean getAccepting() {
        return isAccepting;
    }

    // ---------------------------------------------------------------
    // Fluent builder methods (return this)
    // ---------------------------------------------------------------

    public Location invariant(ComplexConstraint invariant) {
        setInvariant(invariant);
        return this;
    }

    public Location invariant(Constraint invariant) {
        return invariant(new ConstraintNode(invariant));
    }

    public Location urgent() {
        setUrgent(true);
        return this;
    }

    public Location accepting() {
        setAccepting(true);
        return this;
    }

    public Location stop(Clock... clocks) {
        stop.addAll(Arrays.asList(clocks));
        return this;
    }

    public Location flow(Clock clock, Term rate) {
        addRate(clock, rate.expr());
        return this;
    }

    /** Start a transition synchronized on {@code actions} (one IMITATOR transition per action). */
    public TransitionBuilder on(Action... actions) {
        if (actions.length == 0) {
            throw new IllegalArgumentException("on(...) needs at least one action; use silent() for an unsynchronized transition");
        }
        return new TransitionBuilder(this, Arrays.asList(actions));
    }

    /** Start an unsynchronized (epsilon) transition. */
    public TransitionBuilder silent() {
        return new TransitionBuilder(this, List.of());
    }

    /**
     * Start a transition synchronized on each action of {@code actions} (one IMITATOR transition per action).
     * An empty collection gives an unsynchronized transition, like {@code new Transition} with no action.
     */
    public TransitionBuilder on(Collection<Action> actions) {
        return new TransitionBuilder(this, List.copyOf(actions));
    }

    // Declare actions used by a builder transition on the owning PTA (now or when the location is added)
    void declareActions(Iterable<Action> actions) {
        for (Action action : actions) {
            if (owner == null) {
                actionsToDeclare.add(action);
            } else if (owner.isAutoDeclareActions()) {
                owner.addAction(action);
            }
        }
    }

    void setOwner(PTA owner) {
        this.owner = owner;
        if (owner.isAutoDeclareActions()) {
            actionsToDeclare.forEach(owner::addAction);
        }
        actionsToDeclare.clear();
    }

    public PTA getOwner() {
        return owner;
    }

	public String nameToIMITATOR(){
		return name;
	}

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location other)) return false;
        return this.name.equals(other.name);
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

        if (isAccepting){
            sb.append("accepting ");
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
