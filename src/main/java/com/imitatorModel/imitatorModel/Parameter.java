package com.imitatorModel.imitatorModel;

public class Parameter extends NumericVariable {
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

    /** UPPAAL has no parameters: always records a problem (the model must go to IMITATOR). */
    @Override
    public String getUppaalType(UppaalContext ctx) {
        ctx.problem("parameter " + getName() + ": UPPAAL has no parameters (no parameter synthesis)");
        return null;
    }

}
