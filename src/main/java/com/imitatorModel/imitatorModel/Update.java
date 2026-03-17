package com.imitatorModel.imitatorModel;

import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

public class Update {
    // private String update;

    // public Update(String update) {
    //     this.update = update;
    // }

    // // public Update() {
    // //     this.update = null;
    // // }

    // public String toIMITATOR(){
    //     return update;
    // }


    private Variable variable;
    private LinearExpr term;

    public Update(Variable variable, LinearExpr term) {
        this.variable = variable;
        this.term = term;
    }

    public Update(Variable variable, List<Variable> variables, List<BigFraction> coefficients, BigFraction constant) {
        this.variable = variable;
        this.term = new LinearExpr(variables, coefficients, constant);
    }

    public Variable getVariable() {
        return variable;
    }

    public LinearExpr getTerm() {
        return term;
    }

    public String toIMITATOR() {
        return variable.toIMITATOR() + " <- " + term.toIMITATOR();
    }

}
