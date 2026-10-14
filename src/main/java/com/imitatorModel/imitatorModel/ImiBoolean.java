package com.imitatorModel.imitatorModel;


public class ImiBoolean extends VariableType {

    private Boolean value;

    public ImiBoolean(String name) {
        super(name);
    }

    public ImiBoolean(String name, Boolean value) {
        super(name);
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public String valueToIMITATOR() {
        return value == null ? null : (value ? "True" : "False");
    }

    public String getIMITATORType() {
        return "bool";
    }

    public boolean is_discrete_initially_0() {
        if (value == null) {
            return true;
        }

        return false;
    }

    @Override
    public String initialValueToIMITATOR() {
        return "False";
    }

    public BooleanExpr negate() {
        return BooleanExpr.of(this).negate();
    }

    /** Guard/invariant atom {@code b}. */
    public ComplexConstraint isTrue() {
        return new ConstraintNode(new Constraint(this));
    }

    /** Guard/invariant atom {@code not (b)}. */
    public ComplexConstraint isFalse() {
        return new ConstraintNode(new Constraint(negate()));
    }

    /** {@code this := value} */
    public Update assign(boolean value) {
        return new Update(this, value);
    }

    public Update assign(ImiBoolean value) {
        return new Update(this, value);
    }

    public Update assign(BooleanExpr value) {
        return new Update(this, value);
    }
}
