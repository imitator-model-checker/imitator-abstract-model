package com.imitatorModel.bigFraction;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Immutable rational number (numerator / denominator) with automatic reduction.
 * Only the operations needed by the visitor are provided.
 */
public final class BigFraction implements Comparable<BigFraction> {

    private final BigInteger num;   // always reduced
    private final BigInteger den;   // always positive

    public static final BigFraction ZERO = new BigFraction(BigInteger.ZERO);
    public static final BigFraction ONE = new BigFraction(BigInteger.ONE);

    public BigFraction(BigInteger n) {
        this(n, BigInteger.ONE);
    }

    public BigFraction(int n) {
        this(BigInteger.valueOf(n));
    }

    public BigFraction(int n, int d) {
        this(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }

    public BigFraction(BigInteger n, BigInteger d) {
        if (d.equals(BigInteger.ZERO))
            throw new ArithmeticException("Denominator cannot be zero");
        if (d.signum() < 0) {          // keep denominator positive
            n = n.negate();
            d = d.negate();
        }
        BigInteger g = n.gcd(d);
        this.num = n.divide(g);
        this.den = d.divide(g);
    }

    public BigFraction add(BigFraction o) {
        return new BigFraction(num.multiply(o.den).add(o.num.multiply(den)),
                              den.multiply(o.den));
    }

    public BigFraction subtract(BigFraction o) {
        return new BigFraction(num.multiply(o.den).subtract(o.num.multiply(den)),
                              den.multiply(o.den));
    }

    public BigFraction multiply(BigFraction o) {
        return new BigFraction(num.multiply(o.num), den.multiply(o.den));
    }

    public BigFraction divide(BigFraction o) {
        if (o.num.equals(BigInteger.ZERO))
            throw new ArithmeticException("Division by zero");
        return new BigFraction(num.multiply(o.den), den.multiply(o.num));
    }

    public BigFraction negate() {
        return new BigFraction(num.negate(), den);
    }

    /** Exact conversion to int – throws if the fraction is not an integer. */
    public int intValueExact() {
        if (!den.equals(BigInteger.ONE))
            throw new ArithmeticException("Fraction is not an integer");
        return num.intValueExact();
    }

    public int signum() {
        return num.signum();
    }

    public BigInteger numerator() {
        return num;
    }

    public BigInteger denominator() {
        return den;
    }

    @Override public String toString() {
        return den.equals(BigInteger.ONE) ? num.toString()
                                         : num + "/" + den;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BigFraction)) return false;
        BigFraction that = (BigFraction) o;
        return num.equals(that.num) && den.equals(that.den);
    }

    @Override public int hashCode() {
        return Objects.hash(num, den);
    }

    @Override public int compareTo(BigFraction o) {
        return num.multiply(o.den).compareTo(o.num.multiply(den));
    }
}