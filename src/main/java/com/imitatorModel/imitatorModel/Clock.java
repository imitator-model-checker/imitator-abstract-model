package com.imitatorModel.imitatorModel;

public class Clock extends VariableType {
    public Clock(String name) {
        super(name);
    }

    public String getIMITATORType(){
        return "clock";
    }

    public boolean is_continuous_initially_0(){return true;}


}
