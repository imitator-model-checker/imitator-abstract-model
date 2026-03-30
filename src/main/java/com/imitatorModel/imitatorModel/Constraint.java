package com.imitatorModel.imitatorModel;

import com.imitatorModel.bigFraction.BigFraction;

public class Constraint {
    // private String constraint;

    // public Constraint(String constraint) {
    //     this.constraint = constraint;
    // }

    // public String toIMITATOR(){
    //     return constraint;
    // }

    private LinearExpr leftTerm;
    private Operator operator;
    private LinearExpr rightTerm;

    public Constraint(LinearExpr leftTerm, Operator operator, LinearExpr rightTerm) {
        this.leftTerm = leftTerm;
        this.operator = operator;
        this.rightTerm = rightTerm;
    }

    public Constraint(LinearExpr leftTerm,  Operator operator) {
        this.leftTerm = leftTerm;
        this.operator = operator;
        this.rightTerm = new LinearExpr(BigFraction.ZERO);
    }

    public Constraint(Operator operator, LinearExpr rightTerm) {
        this.leftTerm = new LinearExpr(BigFraction.ZERO);
        this.operator = operator;
        this.rightTerm = rightTerm;
    }



    public LinearExpr getLeftTerm() {
        return leftTerm;
    }

    public Operator getOperator() {
        return operator;
    }

    public LinearExpr getRightTerm() {
        return rightTerm;
    }

	public String toIMITATOR(){
		return leftTerm.toIMITATOR() + " " + operator.toIMITATOR() + " " + rightTerm.toIMITATOR();
	}

}
