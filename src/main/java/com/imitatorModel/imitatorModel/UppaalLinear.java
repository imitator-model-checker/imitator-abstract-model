package com.imitatorModel.imitatorModel;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * Printing of linear expressions and constraints in UPPAAL syntax.
 * <p>
 * UPPAAL clock constraints are {@code x ~ e} or {@code x - y ~ e} with {@code e} an integer expression over
 * integer variables, whereas IMITATOR accepts any linear constraint over clocks and rationals. A constraint is
 * written {@code clocks + discrete + constant ~ 0} and divided by the clock coefficient; the remaining coefficients
 * must be integers, the constants are scaled by the time scale.
 */
final class UppaalLinear {
    private UppaalLinear() {}

    /** {@code sum + constant} where every coefficient is an integer; time-like constants scaled when {@code sort} is RATIONAL. */
    static String format(UppaalContext ctx, Map<VariableType, BigFraction> terms, BigFraction constant,
                         UppaalContext.Sort sort, String where) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<VariableType, BigFraction> term : terms.entrySet()) {
            BigFraction k = term.getValue();
            String name = ctx.variableName(term.getKey());
            if (!k.denominator().equals(BigInteger.ONE)) {
                ctx.problem("coefficient " + k + " of " + term.getKey().getName() + " in " + where
                        + " is not an integer (UPPAAL has integer arithmetic only)");
            }
            BigInteger n = k.numerator();
            appendSigned(sb, n.signum() < 0, n.abs().equals(BigInteger.ONE) ? name : n.abs() + "*" + name);
        }
        BigInteger c = sort == UppaalContext.Sort.INT ? ctx.integer(constant, where) : ctx.scaled(constant);
        if (c.signum() != 0 || sb.length() == 0) {
            appendSigned(sb, c.signum() < 0, c.abs().toString());
        }
        return sb.toString();
    }

    private static void appendSigned(StringBuilder sb, boolean negative, String magnitude) {
        if (sb.length() == 0) {
            sb.append(negative ? "-" : "").append(magnitude);
        } else {
            sb.append(negative ? " - " : " + ").append(magnitude);
        }
    }

    static String operator(Operator op) {
        return op == Operator.EQ ? "==" : op.getSymbol();
    }

    /** {@code a op b} ⇔ {@code b mirror(op) a} */
    static Operator mirror(Operator op) {
        return switch (op) {
            case LT -> Operator.GT;
            case LE -> Operator.GE;
            case EQ -> Operator.EQ;
            case GE -> Operator.LE;
            case GT -> Operator.LT;
        };
    }

    private static boolean holds(BigFraction value, Operator op) {
        int s = value.signum();
        return switch (op) {
            case LT -> s < 0;
            case LE -> s <= 0;
            case EQ -> s == 0;
            case GE -> s >= 0;
            case GT -> s > 0;
        };
    }

    /** Linear constraint {@code left op right} (no boolean atom, not a truth constant). */
    static String constraint(UppaalContext ctx, LinearExpr left, Operator op, LinearExpr right, boolean invariant) {
        String where = "`" + left + " " + op.getSymbol() + " " + right + "`";
        LinearExpr d = left.minus(right);   // d op 0

        Map<VariableType, BigFraction> clocks = new LinkedHashMap<>();
        Map<VariableType, BigFraction> discrete = new LinkedHashMap<>();
        for (Map.Entry<VariableType, BigFraction> e : d.getCoefficients().entrySet()) {
            (e.getKey() instanceof Clock ? clocks : discrete).put(e.getKey(), e.getValue());
        }
        BigFraction constant = d.getConstant();

        if (clocks.isEmpty()) {
            return discreteConstraint(ctx, discrete, constant, op, where);
        }
        return clockConstraint(ctx, clocks, discrete, constant, op, invariant, where);
    }

    // discrete op 0, printed "discrete op -constant" with integer coefficients
    private static String discreteConstraint(UppaalContext ctx, Map<VariableType, BigFraction> discrete,
                                             BigFraction constant, Operator op, String where) {
        UppaalContext.Sort sort = ctx.sortOf(discrete.keySet(), where);
        if (sort == UppaalContext.Sort.CONST) {
            return holds(constant, op) ? "true" : "false";
        }
        // multiply by the lcm of the coefficient denominators (and of the constant for an int constraint)
        BigInteger m = BigInteger.ONE;
        for (BigFraction k : discrete.values()) {
            m = lcm(m, k.denominator());
        }
        if (sort == UppaalContext.Sort.INT) {
            m = lcm(m, constant.denominator());
        }
        BigFraction factor = new BigFraction(m);
        Map<VariableType, BigFraction> terms = new LinkedHashMap<>();
        discrete.forEach((v, k) -> terms.put(v, k.multiply(factor)));
        String lhs = format(ctx, terms, BigFraction.ZERO, sort, where);
        String rhs = format(ctx, Map.of(), constant.multiply(factor).negate(), sort, where);
        return lhs + " " + operator(op) + " " + rhs;
    }

    private static String clockConstraint(UppaalContext ctx, Map<VariableType, BigFraction> clocks,
                                          Map<VariableType, BigFraction> discrete, BigFraction constant,
                                          Operator op, boolean invariant, String where) {
        var entries = clocks.entrySet().stream().toList();
        String lhs;
        BigFraction divisor;
        if (entries.size() == 1) {
            divisor = entries.get(0).getValue();                       // a*x + R op 0  ->  x + R/a op' 0
            lhs = ctx.variableName(entries.get(0).getKey());
        } else if (entries.size() == 2 && entries.get(0).getValue().equals(entries.get(1).getValue().negate())) {
            var pos = entries.get(0).getValue().signum() > 0 ? entries.get(0) : entries.get(1);
            var neg = pos == entries.get(0) ? entries.get(1) : entries.get(0);
            divisor = pos.getValue();                                  // a*x - a*y + R op 0  ->  x - y + R/a op 0
            lhs = ctx.variableName(pos.getKey()) + " - " + ctx.variableName(neg.getKey());
        } else {
            ctx.problem("clock constraint " + where + " is not of the form x ~ e or x - y ~ e (UPPAAL accepts only those)");
            return "true";
        }
        Operator o = divisor.signum() < 0 ? mirror(op) : op;

        // lhs o E, with E = -(R / divisor)
        BigFraction inv = BigFraction.ONE.divide(divisor).negate();
        Map<VariableType, BigFraction> rhsTerms = new LinkedHashMap<>();
        for (Map.Entry<VariableType, BigFraction> e : discrete.entrySet()) {
            rhsTerms.put(e.getKey(), e.getValue().multiply(inv));
        }
        BigFraction rhsConstant = constant.multiply(inv);

        UppaalContext.Sort sort = ctx.sortOf(rhsTerms.keySet(), where);
        if (sort == UppaalContext.Sort.INT) {
            ctx.problem("clock constraint " + where + " compares a clock with an int (IMITATOR rejects it too)");
        }

        if (invariant && (o == Operator.GE || o == Operator.GT || o == Operator.EQ)) {
            if (entries.size() == 2 && o != Operator.EQ) {
                // x - y >= E  <=>  y - x <= -E : an upper bound on the reversed difference
                String[] parts = lhs.split(" - ");
                lhs = parts[1] + " - " + parts[0];
                o = mirror(o);
                rhsTerms.replaceAll((v, k) -> k.negate());
                rhsConstant = rhsConstant.negate();
            } else {
                ctx.problem("invariant " + where + " bounds a clock from below: UPPAAL invariants accept upper bounds only");
                return "true";
            }
        }

        String rhs = format(ctx, rhsTerms, rhsConstant, UppaalContext.Sort.RATIONAL, where);
        if (rhsTerms.isEmpty() && !rhsConstant.isInfinite()) {
            ctx.checkClockBound(ctx.scaled(rhsConstant), where);
        }
        return lhs + " " + operator(o) + " " + rhs;
    }

    static BigInteger lcm(BigInteger a, BigInteger b) {
        return a.divide(a.gcd(b)).multiply(b);
    }
}
