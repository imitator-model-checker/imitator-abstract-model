package com.imitatorModel.imitatorModel;

public class Clock extends Variable {
    public Clock(String name) {
        super(name);
    }

    public String getIMITATORType(){
        return "clock";
    }

    public Boolean is_continuous_initially_0(){return true;}


}
