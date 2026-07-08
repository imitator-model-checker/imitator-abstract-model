package com.imitatorModel.imitatorModel;

public class Parameter extends VariableType {
    Constraint constraint = null; 
    public Parameter(String name) {
        super(name);
    }

    public void setConstraints(Constraint constraint){
        this.constraint = constraint;
    }

    public String getIMITATORType(){
        return "parameter";
    }

}
