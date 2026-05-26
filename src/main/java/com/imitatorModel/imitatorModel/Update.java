package com.imitatorModel.imitatorModel;

import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

public class Update {

    private ComplexConstraint condition;        // allows to have conditional updates, e.g., if (x > 0) then x := x + 1 else x := x - 1
    private VariableType variable;
    private LinearExpr term;

    public Update(VariableType variable, LinearExpr term) {
        this.variable = variable;
        this.term = term;
    }

    public Update(VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        this.variable = variable;
        this.term = new LinearExpr(variables, coefficients, constant);
    }

    public Update(ComplexConstraint condition, VariableType variable, LinearExpr term) {
        this.condition = condition;
        this.variable = variable;
        this.term = term;
    }

    public Update(ComplexConstraint condition, VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        this.condition = condition;
        this.variable = variable;
        this.term = new LinearExpr(variables, coefficients, constant);
    }

    public Update(Constraint condition, VariableType variable, LinearExpr term) {
        this.condition = new ComplexConstraint(condition);
        this.variable = variable;
        this.term = term;
    }

    public Update(Constraint condition, VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        this.condition = new ComplexConstraint(condition);
        this.variable = variable;
        this.term = new LinearExpr(variables, coefficients, constant);
    }

    public VariableType getVariable() {
        return variable;
    }

    public LinearExpr getTerm() {
        return term;
    }

    public String toIMITATOR() {
        if (condition != null) {
            return "if " + condition.toIMITATOR()  + " then " + variable.toIMITATOR() + " <- " + term.toIMITATOR() + " end ";
        }
        else {  
            return variable.toIMITATOR() + " <- " + term.toIMITATOR();
        }
    }

}
