package com.imitatorModel.imitatorModel;

import com.imitatorModel.bigFraction.BigFraction;

/** Variable that can appear in a linear expression (clock, parameter, rational, int). */
public abstract class NumericVariable extends VariableType implements Term {

    protected NumericVariable(String name) {
        super(name);
    }

    @Override
    public LinearExpr expr() {
        return new LinearExpr(this);
    }

    /** {@code this := value} */
    public Update assign(Term value) {
        return new Update(this, value.expr());
    }

    public Update assign(long value) {
        return assign(new LinearExpr(BigFraction.of(value)));
    }

    public Update assign(BigFraction value) {
        return assign(new LinearExpr(value));
    }
}
