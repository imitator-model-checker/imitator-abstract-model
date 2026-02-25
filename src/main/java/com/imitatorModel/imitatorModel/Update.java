package com.imitatorModel.imitatorModel;

public class Update {
    private Variable variable;
    private LinearTerm term;

    public Update(Variable variable, LinearTerm term) {
        this.variable = variable;
        this.term = term;
    }

    public Variable getVariable() {
        return variable;
    }

    public LinearTerm getTerm() {
        return term;
    }

    public String toIMITATOR() {
        return variable.toIMITATOR() + " <- " + term.toIMITATOR();
    }

}
