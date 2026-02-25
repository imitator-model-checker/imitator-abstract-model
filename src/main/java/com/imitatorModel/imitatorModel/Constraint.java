package com.imitatorModel.imitatorModel;

public class Constraint {
    private LinearTerm term;
    private Operator operator;

    public Constraint(LinearTerm term, Operator operator) {
        this.term = term;
        this.operator = operator;
    }

    public LinearTerm getTerm() {
        return term;
    }

    public Operator getOperator() {
        return operator;
    }

	public String toIMITATOR(){
		return term.toIMITATOR() + " " + operator.toIMITATOR() + " 0";
	}

}
