package com.imitatorModel.imitatorModel;


public class ImiBoolean extends VariableType {

    private Boolean value;

    public ImiBoolean(String name) {
        super(name);
    }

    public ImiBoolean(String name, Boolean value) {
        super(name);
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    public String getIMITATORType() {
        return "bool";
    }

    public boolean is_discrete_initially_0() {
        if (value == null) {
            return true;
        }

        return false;
    }
}
