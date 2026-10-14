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

    public boolean is_discrete_initially_0(){
        if (value == null){
            return true;
        }
        return false;
    }

}
