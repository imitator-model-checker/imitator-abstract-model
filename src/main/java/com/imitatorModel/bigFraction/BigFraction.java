// package com.patterns2imi.pattern.patternModel.interval;
package com.imitatorModel.bigFraction;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

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
        this(n, BigInteger.ONE);    // cast int n as n/1
    }

    public BigFraction(int n) {
        this(BigInteger.valueOf(n));  // int is diffirent from BigInteger, so we need to convert it first
    }

     public BigFraction(int n, int d) {
        this(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }
   public BigFraction(BigInteger n, BigInteger d) { //reduce to cannonical form
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

    // basic fractional arithmetic operations, all return reduced results
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

    // Returns -1, 0, or 1 as this fraction is negative, zero, or positive.
    public int signum() {
        return num.signum();
    }

    //basic getters 
    public BigInteger numerator() {
        return num;
    }

    public BigInteger denominator() {
        return den;
    }

    //toString
    @Override public String toString() {
        return den.equals(BigInteger.ONE) ? num.toString()
                                         : num + "/" + den;
    }

    // equality  
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BigFraction)) return false; // o cant be reduced to BigFraction
        BigFraction that = (BigFraction) o; //o can be reduced to BigFraction, so do it
        return num.equals(that.num) && den.equals(that.den);
    }

    //hash code for use in hash-based collections
    @Override public int hashCode() {
        return Objects.hash(num, den);
    }

    //to compare two fractions
    // Returns a negative integer → this < o
    // Returns 0 → this == o
    // Returns a positive integer → this > o
    @Override public int compareTo(BigFraction o) {
        return num.multiply(o.den).compareTo(o.num.multiply(den));
    }

    // return a positive random BigFraction in [0, 1] given denominator 
    public static BigFraction random(int denominator) {
        if (denominator <= 0) {
            throw new IllegalArgumentException("Denominator must be positive");
        }

        int numerator = new Random().nextInt(denominator-1) + 1; // random integer in [1, denominator-1]

        return new BigFraction(numerator, denominator);
    }

}
