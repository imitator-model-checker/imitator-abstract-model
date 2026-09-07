package com.imitatorModel.imitatorModel;

import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

public final class Update {

    private ComplexConstraint condition;        // allows to have conditional updates, e.g., if (x > 0) then x := x + 1 else x := x - 1
    private VariableType variable;
    private UpdateTerm term;

    public Update(VariableType variable, LinearExpr term) {
        validateTerm(variable, term);
        this.variable = variable;
        this.term = term;
    }

    public Update(ImiBoolean variable, Boolean term) {
        this(variable, BooleanExpr.of(term));
    }

    public Update(ImiBoolean variable, ImiBoolean term) {
        this(variable, BooleanExpr.of(term));
    }

    public Update(ImiBoolean variable, BooleanExpr term) {
        this.variable = variable;
        this.term = term;
    }

    public Update(VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        validateLinearVariable(variable);
        this.variable = variable;
        this.term = new LinearExpr(variables, coefficients, constant);
    }

    public Update(ComplexConstraint condition, VariableType variable, LinearExpr term) {
        validateTerm(variable, term);
        this.condition = condition;
        this.variable = variable;
        this.term = term;
    }

    public Update(ComplexConstraint condition, ImiBoolean variable, Boolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(ComplexConstraint condition, ImiBoolean variable, ImiBoolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(ComplexConstraint condition, ImiBoolean variable, BooleanExpr term) {
        this.condition = condition;
        this.variable = variable;
        this.term = term;
    }

    public Update(ComplexConstraint condition, VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        validateLinearVariable(variable);
        this.condition = condition;
        this.variable = variable;
        this.term = new LinearExpr(variables, coefficients, constant);
    }

    public Update(Constraint condition, VariableType variable, LinearExpr term) {
        validateTerm(variable, term);
        this.condition = new ConstraintNode(condition);
        this.variable = variable;
        this.term = term;
    }

    public Update(Constraint condition, ImiBoolean variable, Boolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(Constraint condition, ImiBoolean variable, ImiBoolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(Constraint condition, ImiBoolean variable, BooleanExpr term) {
        this.condition = new ConstraintNode(condition);
        this.variable = variable;
        this.term = term;
    }

    public Update(Constraint condition, VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        validateLinearVariable(variable);
        this.condition = new ConstraintNode(condition);
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
        this.condition = new ConstraintNode(condition);
        this.variable = term1.getTerms().get(0).getFirst();
        this.term = term2;
    }

    public VariableType getVariable() {
        return variable;
    }

    public UpdateTerm getTerm() {
        return term;
    }

    public String toIMITATOR() {
        if (condition != null) {
            return "if " + condition.toIMITATOR()  + " then " + variable.toIMITATOR() + " <- " + termToIMITATOR() + " end ";
        }
        else {  
            return variable.toIMITATOR() + " <- " + termToIMITATOR();
        }
    }

    private String termToIMITATOR() {
        return term.toIMITATOR();
    }

    private static void validateTerm(VariableType variable, LinearExpr term) {
        if (variable instanceof ImiBoolean) {
            throw new IllegalArgumentException("Boolean updates require a BooleanExpr term");
        }
    }

    private static void validateLinearVariable(VariableType variable) {
        if (variable instanceof ImiBoolean) {
            throw new IllegalArgumentException("Boolean updates require a BooleanExpr term");
        }
    }

}
