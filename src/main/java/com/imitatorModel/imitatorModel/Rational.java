package com.imitatorModel.imitatorModel;
import com.imitatorModel.bigFraction.BigFraction;

public class Rational extends VariableType {
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

    public String getIMITATORType(){
        return "rational";
    }

    public Boolean is_discrete_initially_0(){
        if (value == null){
            return true;
        }
        return false;
    }

}
