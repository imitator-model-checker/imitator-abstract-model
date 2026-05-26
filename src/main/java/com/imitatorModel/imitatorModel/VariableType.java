package com.imitatorModel.imitatorModel;

import java.util.Objects;

public abstract class VariableType {
    private final String name;

    public VariableType(String name) {
        this.name = name;
    }

    
    public abstract String getIMITATORType();

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableType)) return false;

        VariableType other = (VariableType) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
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

