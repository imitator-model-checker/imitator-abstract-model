package com.imitatorModel.imitatorModel;
import com.imitatorModel.bigFraction.BigFraction;

public class Rational extends NumericVariable {
    private BigFraction value;

    public Rational(String name) {
        super(name);
    }

    public Rational(String name, BigFraction value){
        super(name);
        this.value = value;
    }

    @Override
    public BigFraction getValue() {
        return value;
    }

    @Override
    public String valueToIMITATOR() {
        if (value != null && value.isInfinite()) {
            throw new IllegalStateException("Rational constant " + getName() + " cannot be infinite in IMITATOR");
        }
        return super.valueToIMITATOR();
    }

    public String getIMITATORType(){
        return "rational";
    }

    @Override
    public String getUppaalType(UppaalContext ctx) {
        return value == null ? ctx.intRange() : "const int";
    }

    /** Scaled integer: {@code int[lo,hi] r = 0;} or, for a constant 3/2 with time scale 2, {@code const int R = 3;} */
    @Override
    public String declarationToUppaal(UppaalContext ctx) {
        String comment = "   // rational, times " + ctx.timeScale();
        if (value == null) {
            return getUppaalType(ctx) + " " + toUppaal(ctx) + " = 0;" + comment;
        }
        java.math.BigInteger scaled = ctx.scaled(value);
        ctx.checkIntRange(scaled, "rational constant " + getName());
        return getUppaalType(ctx) + " " + toUppaal(ctx) + " = " + scaled + ";" + comment;
    }

    public boolean is_discrete_initially_0(){
        if (value == null){
            return true;
        }
        return false;
    }

}
