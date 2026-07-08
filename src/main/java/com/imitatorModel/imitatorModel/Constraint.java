package com.imitatorModel.imitatorModel;

import java.util.concurrent.atomic.AtomicLong;

public final class Constraint {
    private final LinearExpr leftTerm;
    private final Operator operator;
    private final LinearExpr rightTerm;
    private final Boolean truthConst ;

    private static final AtomicLong NEXT_ID = new AtomicLong();

    public static final Constraint TRUE = new Constraint(LinearExpr.ZERO, Operator.EQ, LinearExpr.ZERO, true);
    public static final Constraint FALSE = new Constraint(LinearExpr.ZERO, Operator.NE, LinearExpr.ZERO, false);

  
    private final long id = NEXT_ID.incrementAndGet();

    public long getId() {
        return id;
    }

    public Constraint(LinearExpr leftTerm, Operator operator, LinearExpr rightTerm, Boolean truthConst) {
        this.leftTerm = leftTerm;
        this.operator = operator;
        this.rightTerm = rightTerm;
        if ("clock".equals(leftTerm.getTerms().get(0).getFirst().getIMITATORType()) && (operator == Operator.LT || operator == Operator.LE) && rightTerm.equals(LinearExpr.ZERO)) {
            this.truthConst = false;
        } 
        else if ("clock".equals(leftTerm.getTerms().get(0).getFirst().getIMITATORType()) && (operator == Operator.LT || operator == Operator.LE) && rightTerm.equals(LinearExpr.INFINITY)) {
            this.truthConst = true;
        }
        else {
            this.truthConst = truthConst;
        }
    }

    public Constraint(LinearExpr leftTerm, Operator operator, LinearExpr rightTerm) {
        this(leftTerm,operator,rightTerm,null);
    }

    public Constraint(LinearExpr leftTerm,  Operator operator) {
        this(leftTerm,operator,LinearExpr.ZERO,null);

    }

    public Constraint(Operator operator, LinearExpr rightTerm) {
        this(LinearExpr.ZERO,operator,rightTerm,null);

    }



    public Boolean getTruthConst() {
        return truthConst;
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

    public Boolean isTruthConst(){
        return this.truthConst;
    }

    public Constraint negate(){
        return new Constraint(this.leftTerm, this.operator.getInverse() ,this.rightTerm,!this.truthConst);
    }

	public String toIMITATOR(){
        if (truthConst) {
            return "True";
        }
        if (!truthConst) {
            return "False";
        }
		return leftTerm.toIMITATOR() + " " + operator.toIMITATOR() + " " + rightTerm.toIMITATOR();
	}

}
