package com.imitatorModel.imitatorModel;

public abstract class Variable {
    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public abstract String getIMITATORType();

    public String getName() {
        return name;
    }

    // True if need to be added to the initial state with continuous value 0
    public Boolean is_continuous_initially_0(){return false;}

    // True if need to be added to the initial state with discrete value 0
    public  Boolean is_discrete_initially_0(){return false;}

    public String toIMITATOR(){
		return getName();
	}

    @Override
    public String toString() {
        return getName();
    }

}

