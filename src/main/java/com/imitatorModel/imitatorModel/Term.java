package com.imitatorModel.imitatorModel;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * Anything usable as a linear expression: {@link LinearExpr} itself and the numeric variables
 * ({@link Clock}, {@link Parameter}, {@link Rational}, {@link Int}).
 * Arithmetic returns a new {@link LinearExpr}; comparisons return a {@link ComplexConstraint}
 * ready to be used as a guard or invariant.
 */
public interface Term {
    LinearExpr expr();

    // Arithmetic
    default LinearExpr plus(Term o)         { return expr().add(o.expr()); }
    default LinearExpr plus(long k)         { return expr().add(BigFraction.of(k)); }
    default LinearExpr plus(BigFraction k)  { return expr().add(k); }
    default LinearExpr minus(Term o)        { return expr().minus(o.expr()); }
    default LinearExpr minus(long k)        { return expr().add(BigFraction.of(k).negate()); }
    default LinearExpr minus(BigFraction k) { return expr().add(k.negate()); }
    default LinearExpr times(long k)        { return expr().times(BigFraction.of(k)); }
    default LinearExpr times(BigFraction k) { return expr().times(k); }
    default LinearExpr neg()                { return expr().times(BigFraction.ONE.negate()); }

    // Comparisons
    default Constraint atom(Operator op, Term o) { return new Constraint(expr(), op, o.expr()); }
    default ComplexConstraint cmp(Operator op, Term o) { return new ConstraintNode(atom(op, o)); }
    default ComplexConstraint cmp(Operator op, long k) { return cmp(op, new LinearExpr(BigFraction.of(k))); }

    default ComplexConstraint lt(Term o) { return cmp(Operator.LT, o); }
    default ComplexConstraint le(Term o) { return cmp(Operator.LE, o); }
    default ComplexConstraint eq(Term o) { return cmp(Operator.EQ, o); }
    default ComplexConstraint ge(Term o) { return cmp(Operator.GE, o); }
    default ComplexConstraint gt(Term o) { return cmp(Operator.GT, o); }

    default ComplexConstraint lt(long k) { return cmp(Operator.LT, k); }
    default ComplexConstraint le(long k) { return cmp(Operator.LE, k); }
    default ComplexConstraint eq(long k) { return cmp(Operator.EQ, k); }
    default ComplexConstraint ge(long k) { return cmp(Operator.GE, k); }
    default ComplexConstraint gt(long k) { return cmp(Operator.GT, k); }
}
