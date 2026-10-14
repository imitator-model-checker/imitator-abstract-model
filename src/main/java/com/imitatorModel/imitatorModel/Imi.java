package com.imitatorModel.imitatorModel;

import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * Static shorthands, meant for {@code import static com.imitatorModel.imitatorModel.Imi.*;}
 * <pre>
 * ComplexConstraint g = and(t.ge(c(1, 2)), not(lock.isTrue()));
 * </pre>
 */
public final class Imi {
    private Imi() {}

    public static final ComplexConstraint TRUE  = new ConstraintNode(Constraint.TRUE);
    public static final ComplexConstraint FALSE = new ConstraintNode(Constraint.FALSE);

    /** Rational number n. */
    public static BigFraction q(long n)         { return BigFraction.of(n); }
    /** Rational number n/d. */
    public static BigFraction q(long n, long d) { return BigFraction.of(n, d); }

    /** Constant expression n. */
    public static LinearExpr c(long n)          { return new LinearExpr(q(n)); }
    /** Constant expression n/d. */
    public static LinearExpr c(long n, long d)  { return new LinearExpr(q(n, d)); }
    public static LinearExpr c(BigFraction k)   { return new LinearExpr(k); }

    public static Action action(String name)    { return new Action(name); }

    public static ComplexConstraint of(Constraint c) { return new ConstraintNode(c); }

    /** Conjunction; a single argument is returned as is. */
    public static ComplexConstraint and(ComplexConstraint... cs) {
        return cs.length == 1 ? cs[0] : new AndNode(List.of(cs));
    }

    /** Disjunction; a single argument is returned as is. */
    public static ComplexConstraint or(ComplexConstraint... cs) {
        return cs.length == 1 ? cs[0] : new OrNode(List.of(cs));
    }

    public static ComplexConstraint not(ComplexConstraint c) { return c.negate(); }

    public static ListUpdates updates(Update... us) { return new ListUpdates(List.of(us)); }
}
