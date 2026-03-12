package com.imitatorModel.imitatorModel;

public class Update {
    private String update;

    public Update(String update) {
        this.update = update;
    }

    // public Update() {
    //     this.update = null;
    // }

    public String toIMITATOR(){
        return update;
    }

    // private Variable variable;
    // private LinearExpr term;

    // public Update(Variable variable, LinearExpr term) {
    //     this.variable = variable;
    //     this.term = term;
    // }

    // public Variable getVariable() {
    //     return variable;
    // }

    // public LinearExpr getTerm() {
    //     return term;
    // }

    // public String toIMITATOR() {
    //     return variable.toIMITATOR() + " <- " + term.toIMITATOR();
    // }

}
