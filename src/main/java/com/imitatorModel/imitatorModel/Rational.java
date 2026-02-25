package com.imitatorModel.imitatorModel;

public class Rational extends Variable {
    public Rational(String name) {
        super(name);
    }

    public String getIMITATORType(){
        return "rational";
    }

    public Boolean is_discrete_initially_0(){return true;}

}
