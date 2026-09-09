package com.imitatorModel.imitatorModel;

public class Int extends VariableType {
    private Integer value;

    public Int(String name) {
        super(name);
    }

    public Int(String name, Integer value) {
        super(name);
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public String getIMITATORType() {
        return "int";
    }

    public boolean is_discrete_initially_0() {
        return value == null;
    }
}
