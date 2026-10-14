package com.imitatorModel.imitatorModel;

import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * Assignment {@code variable := term}, optionally conditional ({@code if condition then ... end}).
 * Prefer the fluent forms: {@code x.reset()}, {@code i.assign(i.plus(1))}, {@code b.assign(true)},
 * {@code u.when(cond)}. A queue push ({@code q.push(e)}) is an update whose term is a side effect: {@link #getVariable()}
 * is the queue, and it prints as {@code queue_push(e, q)}.
 */
public final class Update {

    private final ComplexConstraint condition;        // allows to have conditional updates, e.g., if (x > 0) then x := x + 1 else x := x - 1
    private final VariableType variable;
    private final UpdateTerm term;

    /**
     * Canonical constructor. {@code condition} may be null and must only use discrete variables;
     * a boolean target needs a {@link BooleanExpr}, any other target a {@link LinearExpr}.
     */
    public Update(ComplexConstraint condition, VariableType variable, UpdateTerm term) {
        validateTarget(variable, term);
        validateCondition(condition);
        this.condition = condition;
        this.variable = variable;
        this.term = term;
    }

    public Update(VariableType variable, LinearExpr term) {
        this((ComplexConstraint) null, variable, term);
    }

    public Update(ImiBoolean variable, Boolean term) {
        this(variable, BooleanExpr.of(term));
    }

    public Update(ImiBoolean variable, ImiBoolean term) {
        this(variable, BooleanExpr.of(term));
    }

    public Update(ImiBoolean variable, BooleanExpr term) {
        this((ComplexConstraint) null, variable, term);
    }

    public Update(ImiQueue variable, QueueExpr term) {
        this((ComplexConstraint) null, variable, term);
    }

    public Update(VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        this(variable, new LinearExpr(variables, coefficients, constant));
    }

    public Update(ComplexConstraint condition, VariableType variable, LinearExpr term) {
        this(condition, variable, (UpdateTerm) term);
    }

    public Update(ComplexConstraint condition, ImiBoolean variable, Boolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(ComplexConstraint condition, ImiBoolean variable, ImiBoolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(ComplexConstraint condition, ImiBoolean variable, BooleanExpr term) {
        this(condition, variable, (UpdateTerm) term);
    }

    public Update(ComplexConstraint condition, VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        this(condition, variable, new LinearExpr(variables, coefficients, constant));
    }

    public Update(Constraint condition, VariableType variable, LinearExpr term) {
        this(new ConstraintNode(condition), variable, term);
    }

    public Update(Constraint condition, ImiBoolean variable, Boolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(Constraint condition, ImiBoolean variable, ImiBoolean term) {
        this(condition, variable, BooleanExpr.of(term));
    }

    public Update(Constraint condition, ImiBoolean variable, BooleanExpr term) {
        this(new ConstraintNode(condition), variable, term);
    }

    public Update(Constraint condition, VariableType variable, List<VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        this(new ConstraintNode(condition), variable, variables, coefficients, constant);
    }

    /** Same update, executed only if {@code cond} holds (AND-ed with an existing condition). */
    public Update when(ComplexConstraint cond) {
        return new Update(condition == null ? cond : condition.and(cond), variable, term);
    }

    public Update when(Constraint cond) {
        return when(new ConstraintNode(cond));
    }

    public VariableType getVariable() {
        return variable;
    }

    public UpdateTerm getTerm() {
        return term;
    }

    public ComplexConstraint getCondition() {
        return condition;
    }

    public String toIMITATOR() {
        // a side effect (e.g. queue_push(e, q)) is printed alone, without "variable <- "
        String statement = term.isSideEffect()
                ? termToIMITATOR()
                : variable.toIMITATOR() + " <- " + termToIMITATOR();
        if (condition != null) {
            return "if " + condition.toIMITATOR()  + " then " + statement + " end ";
        }
        else {  
            return statement;
        }
    }

    private String termToIMITATOR() {
        return term.toIMITATOR();
    }

    private static void validateTarget(VariableType variable, UpdateTerm term) {
        if (variable == null || term == null) {
            throw new IllegalArgumentException("Update needs a target variable and a value");
        }
        if (variable instanceof Parameter) {
            throw new IllegalArgumentException("Parameter " + variable.getName() + " cannot be updated");
        }
        if (variable instanceof ImiBoolean && !(term instanceof BooleanExpr)) {
            throw new IllegalArgumentException("Boolean updates require a BooleanExpr term");
        }
        if (!(variable instanceof ImiBoolean) && term instanceof BooleanExpr) {
            throw new IllegalArgumentException(
                "Variable " + variable.getName() + " is not boolean and cannot receive a BooleanExpr");
        }
        if ((variable instanceof ImiQueue) != (term instanceof QueueExpr)) {
            throw new IllegalArgumentException(variable instanceof ImiQueue
                ? "Queue " + variable.getName() + " can only be updated with push, clear or a queue copy"
                : "Variable " + variable.getName() + " is not a queue and cannot receive a queue value");
        }
    }

    // IMITATOR only accepts discrete variables in the condition of an "if" update
    private static void validateCondition(ComplexConstraint condition) {
        if (condition == null) {
            return;
        }
        for (VariableType v : condition.getVariables()) {
            if (v instanceof Clock || v instanceof Parameter) {
                throw new IllegalArgumentException(
                    "Update condition may only use discrete variables, found "
                    + v.getIMITATORType() + " " + v.getName());
            }
        }
    }
}
