package com.imitatorModel.imitatorModel;

import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

public final class Update {

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

    public Update(LinearExpr term1, LinearExpr term2) {
        // take the first variable as the first one to be seen in the Linear expression 1
        this.variable = term1.getTerms().get(0).getFirst();
        this.term = term2;
    }

    public Update(ComplexConstraint condition, LinearExpr term1, LinearExpr term2) {
        this.condition = condition;
        this.variable = term1.getTerms().get(0).getFirst();
        this.term = term2;
    }

    public Update(Constraint condition, LinearExpr term1, LinearExpr term2) {
        this.condition = new ComplexConstraint(condition);
        this.variable = term1.getTerms().get(0).getFirst();
        this.term = term2;
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
