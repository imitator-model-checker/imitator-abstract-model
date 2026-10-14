package com.imitatorModel.imitatorModel;

import java.util.Objects;

import com.imitatorModel.bigFraction.BigFraction;

public final class Constraint {
    private final LinearExpr leftTerm;
    private final Operator operator;
    private final LinearExpr rightTerm;
    private final Boolean truthConst ;
    private final BooleanExpr booleanTerm;


    public static final Constraint TRUE = new Constraint(LinearExpr.ZERO, Operator.EQ, LinearExpr.ZERO, true);
    public static final Constraint FALSE = new Constraint(LinearExpr.ZERO, Operator.GT, LinearExpr.ZERO, false);

    public Constraint(LinearExpr leftTerm, Operator operator, LinearExpr rightTerm, Boolean truthConst) {
        this.booleanTerm = null;
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
        // A side whose constant is +-inf is +-inf whatever its variables
        else if (leftTerm.isPlusInfinity() || leftTerm.isMinusInfinity()
                || rightTerm.isPlusInfinity() || rightTerm.isMinusInfinity()) {
            this.truthConst = compareWithInfinity(leftTerm, operator, rightTerm);
        }

        else {
            this.truthConst = truthConst;
        }
    }

    // Truth value of "left op right" when at least one side is infinite
    private static boolean compareWithInfinity(LinearExpr left, Operator operator, LinearExpr right) {
        int l = infinitySign(left);
        int r = infinitySign(right);

        // Both sides infinite: compare the signs. One side infinite: the finite side counts as 0.
        int cmp = Integer.compare(l, r);
        if (l != 0 && l == r) {
            cmp = 0;
        }

        return switch (operator) {
            case LT -> cmp < 0;
            case LE -> cmp <= 0;
            case EQ -> cmp == 0 && l != 0;
            case GE -> cmp >= 0;
            case GT -> cmp > 0;
        };
    }

    private static int infinitySign(LinearExpr e) {
        if (e.isPlusInfinity()) return 1;
        if (e.isMinusInfinity()) return -1;
        return 0;
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

    public Constraint(ImiBoolean variable) {
        this(BooleanExpr.of(variable));
    }

    public Constraint(BooleanExpr booleanTerm) {
        this.leftTerm = null;
        this.operator = null;
        this.rightTerm = null;
        this.truthConst = null;
        this.booleanTerm = Objects.requireNonNull(booleanTerm);
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
        if (booleanTerm != null) {
            return new Constraint(booleanTerm.negate());
        }

        if (truthConst != null) {
            return truthConst ? FALSE : TRUE;
        }

        if (operator == Operator.EQ) {
            throw new IllegalStateException(
                "The negation of " + toIMITATOR() + " is a disjunction; use ConstraintNode.negate()");
        }

        return new Constraint(
                leftTerm,
                operator.getInverse(),
                rightTerm);
    }

    public BooleanExpr getBooleanTerm() {
        return booleanTerm;
    }

    public boolean isTrue() {
        return Boolean.TRUE.equals(truthConst);
    }

    public boolean isFalse() {
        return Boolean.FALSE.equals(truthConst);
    }

	public String toIMITATOR(){
	    if (booleanTerm != null) {
	        return booleanTerm.toIMITATOR();
	    }
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
                && Objects.equals(truthConst, other.truthConst)
                && Objects.equals(booleanTerm, other.booleanTerm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                leftTerm,
                operator,
                rightTerm,
                truthConst,
                booleanTerm
        );
    }
}
