package com.imitatorModel.imitatorModel;

public class Int extends NumericVariable {
    private Integer value;

    public Int(String name) {
        super(name);
    }

    public Int(String name, Integer value) {
        super(name);
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public String getIMITATORType() {
        return "int";
    }

    public boolean is_discrete_initially_0() {
        return value == null;
    }

    @Override
    public String getUppaalType(UppaalContext ctx) {
        return value == null ? ctx.intRange() : "const int";
    }

    /** {@code int[lo,hi] i = 0;} or, for a constant, {@code const int N = 3;} (IMITATOR ints are not scaled). */
    @Override
    public String declarationToUppaal(UppaalContext ctx) {
        if (value == null) {
            return getUppaalType(ctx) + " " + toUppaal(ctx) + " = 0;";
        }
        ctx.checkIntRange(java.math.BigInteger.valueOf(value), "int constant " + getName());
        return getUppaalType(ctx) + " " + toUppaal(ctx) + " = " + value + ";";
    }
}
