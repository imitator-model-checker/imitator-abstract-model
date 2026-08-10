package com.imitatorModel.imitatorModel;

import java.util.Objects;

import com.imitatorModel.bigFraction.BigFraction;

public final class Constraint {
    private final LinearExpr leftTerm;
    private final Operator operator;
    private final LinearExpr rightTerm;
    private final Boolean truthConst ;


    public static final Constraint TRUE = new Constraint(LinearExpr.ZERO, Operator.EQ, LinearExpr.ZERO, true);
    public static final Constraint FALSE = new Constraint(LinearExpr.ZERO, Operator.GT, LinearExpr.ZERO, false);

    public Constraint(LinearExpr leftTerm, Operator operator, LinearExpr rightTerm, Boolean truthConst) {
        this.leftTerm = leftTerm;
        this.operator = operator;
        this.rightTerm = rightTerm;

        // this.truthConst = truthConst;

        boolean allPositiveClocksLeft =
            !leftTerm.getTerms().isEmpty()
            && leftTerm.getConstant().compareTo(BigFraction.ZERO) >= 0
            && leftTerm.getTerms().stream().allMatch(term ->
                "clock".equals(term.getFirst().getIMITATORType())
                && term.getSecond().compareTo(BigFraction.ZERO) > 0
            );

        boolean allPositiveClocksRight =
            !rightTerm.getTerms().isEmpty()
            && rightTerm.getConstant().compareTo(BigFraction.ZERO) >= 0
            && rightTerm.getTerms().stream().allMatch(term ->
                "clock".equals(term.getFirst().getIMITATORType())
                && term.getSecond().compareTo(BigFraction.ZERO) > 0
            );

        if (allPositiveClocksLeft && operator == Operator.LT && rightTerm.equals(LinearExpr.ZERO)) {
                this.truthConst = false;
           } 
        else if (allPositiveClocksLeft && operator == Operator.GE && rightTerm.equals(LinearExpr.ZERO)) {
                this.truthConst = true;
           } 
        else if (allPositiveClocksRight && operator == Operator.LE && leftTerm.equals(LinearExpr.ZERO)) {
                this.truthConst = true;
           }
        else if (allPositiveClocksRight && operator == Operator.GT && leftTerm.equals(LinearExpr.ZERO)) {
                this.truthConst = false;
           }
        // Infinity vs infinity
        else if (leftTerm.equals(LinearExpr.INFINITY)
                && rightTerm.equals(LinearExpr.INFINITY)) {
            this.truthConst = switch (operator) {
                case EQ, LE, GE -> true;
                case LT, GT -> false;  //, NE 
            };
        }
        else if (leftTerm.equals(LinearExpr.MINUS_INFINITY)
                && rightTerm.equals(LinearExpr.MINUS_INFINITY)) {
            this.truthConst = switch (operator) {
                case EQ, LE, GE -> true;
                case LT, GT -> false;  //, NE
            };
        }
        else if (leftTerm.equals(LinearExpr.INFINITY)
                && rightTerm.equals(LinearExpr.MINUS_INFINITY)) {
            this.truthConst = switch (operator) {
                case GT, GE -> true;  //, NE
                case LT, LE, EQ -> false;
            };
        }
        else if (leftTerm.equals(LinearExpr.MINUS_INFINITY)
                && rightTerm.equals(LinearExpr.INFINITY)) {
            this.truthConst = switch (operator) {
                case LT, LE -> true;   //, NE
                case GT, GE, EQ -> false;
            };
        }
        /////////////////
        else if ((operator == Operator.LT || operator == Operator.LE)
                && rightTerm.equals(LinearExpr.INFINITY)) {
            this.truthConst = true;
        }

        else if ((operator == Operator.GT || operator == Operator.GE
                || operator == Operator.EQ)
                && rightTerm.equals(LinearExpr.INFINITY)) {
            this.truthConst = false;
        }

        else if ((operator == Operator.LT || operator == Operator.LE
                || operator == Operator.EQ)
                && rightTerm.equals(LinearExpr.MINUS_INFINITY)) {
            this.truthConst = false;
        }
        else if ((operator == Operator.GT || operator == Operator.GE)
                && rightTerm.equals(LinearExpr.MINUS_INFINITY)) {
            this.truthConst = true;
        }

        else if ((operator == Operator.LT || operator == Operator.LE
                || operator == Operator.EQ)
                && leftTerm.equals(LinearExpr.INFINITY)) {
            this.truthConst = false;
        }
        else if ((operator == Operator.GT || operator == Operator.GE)
                && leftTerm.equals(LinearExpr.INFINITY)) {
            this.truthConst = true;
        }

        else if ((operator == Operator.LT || operator == Operator.LE)
                && leftTerm.equals(LinearExpr.MINUS_INFINITY)) {
            this.truthConst = true;
        }
        else if ((operator == Operator.GT || operator == Operator.GE
                || operator == Operator.EQ)
                && leftTerm.equals(LinearExpr.MINUS_INFINITY)) {
            this.truthConst = false;
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


    public Constraint negate() {
        Boolean negatedTruthConst =
                (truthConst == null) ? null : !truthConst;

        return new Constraint(
                leftTerm,
                operator.getInverse(),
                rightTerm,
                negatedTruthConst);
    }

	public String toIMITATOR(){
	    if (truthConst != null) {
            return truthConst ? "True" : "False";
        }

        return leftTerm.toIMITATOR() + " " + operator.toIMITATOR() + " " + rightTerm.toIMITATOR();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Constraint other)) {
            return false;
        }

        return Objects.equals(leftTerm, other.leftTerm)
                && operator == other.operator
                && Objects.equals(rightTerm, other.rightTerm)
                && Objects.equals(truthConst, other.truthConst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                leftTerm,
                operator,
                rightTerm,
                truthConst
        );
    }
}
