package com.imitatorModel.imitatorModel;

public class Clock extends NumericVariable {
    public Clock(String name) {
        super(name);
    }

    public String getIMITATORType(){
        return "clock";
    }

    public boolean is_continuous_initially_0(){return true;}

    /** {@code this := 0} */
    public Update reset() {
        return assign(0);
    }
}
