package com.imitatorModel.imitatorModel;

import java.util.Objects;

public final class BooleanExpr implements UpdateTerm {
    private final Boolean value;
    private final ImiBoolean variable;
    private final boolean negated;

    private BooleanExpr(Boolean value, ImiBoolean variable, boolean negated) {
        this.value = value;
        this.variable = variable;
        this.negated = negated;
    }

    public static BooleanExpr of(boolean value) {
        return new BooleanExpr(value, null, false);
    }

    public static BooleanExpr of(ImiBoolean variable) {
        return new BooleanExpr(null, Objects.requireNonNull(variable), false);
    }

    public BooleanExpr negate() {
        return new BooleanExpr(value, variable, !negated);
    }

    public String toIMITATOR() {
        String expression = value != null
                ? (value ? "True" : "False")
                : variable.toIMITATOR();
        return negated ? "not (" + expression + ")" : expression;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BooleanExpr other)) {
            return false;
        }
        return Objects.equals(value, other.value)
                && Objects.equals(variable, other.variable)
                && negated == other.negated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, variable, negated);
    }
}
