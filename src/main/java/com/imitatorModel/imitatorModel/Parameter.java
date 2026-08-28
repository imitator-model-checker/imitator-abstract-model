package com.imitatorModel.imitatorModel;

public class Parameter extends VariableType {
    private Constraint constraint = null; 

    public Parameter(String name) {
        super(name);
    }

    public Constraint geConstraint(){
        return this.constraint;
    }
    
    public void setConstraints(Constraint constraint){
        this.constraint = constraint;
    }

    public String getIMITATORType(){
        return "parameter";
    }

}
