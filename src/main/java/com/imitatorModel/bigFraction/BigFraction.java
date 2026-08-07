// package com.patterns2imi.pattern.patternModel.interval;
package com.imitatorModel.bigFraction;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

// import com.imitatorModel.imitatorModel.VariableType;

/**
 * Immutable rational number (numerator / denominator) with automatic reduction.
 * Only the operations needed by the visitor are provided.
 */
public final class BigFraction  implements Comparable<BigFraction> {

    private final BigInteger num;   // always reduced
    private final BigInteger den;   // always positive
    private final boolean infinite;

    public  String getIMITATORType(){
        return "BigFraction";
    }

    public static final BigFraction ZERO = new BigFraction(BigInteger.ZERO);
    public static final BigFraction ONE = new BigFraction(BigInteger.ONE);
    public static final BigFraction INFINITY = new BigFraction(true);
    public static final BigFraction MINUS_INFINITY = new BigFraction(false);

    private BigFraction(boolean positivity) {
        this.num = positivity ? BigInteger.ONE : BigInteger.ONE.negate();
        this.den = BigInteger.ONE;
        this.infinite = true;
    }

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
        this.infinite = false;
    }

    // basic fractional arithmetic operations, all return reduced results
    public BigFraction add(BigFraction o) {
        if (this.infinite && o.infinite) {
            if (this.num.signum() != o.num.signum()) {
                throw new ArithmeticException("Infinity + -Infinity is undefined");
            }
            return this;
        }

        if (this.infinite) {
            return this;
        }

        if (o.infinite) {
            return o;
        }

        return new BigFraction(
                num.multiply(o.den).add(o.num.multiply(den)),
                den.multiply(o.den)
        );
    }

    public BigFraction subtract(BigFraction o) {
        if (this.infinite && o.infinite) {
            if (this.num.signum() != o.num.signum()) {
                return this; // +∞ - (-∞) = +∞, -∞ - (+∞) = -∞
            }
            throw new ArithmeticException("Infinity - Infinity is undefined");
        }

        if (this.infinite) {
            return this;
        }

        if (o.infinite) {
            return o.num.signum() > 0
                    ? BigFraction.MINUS_INFINITY
                    : BigFraction.INFINITY;
        }

        return new BigFraction(
                num.multiply(o.den).subtract(o.num.multiply(den)),
                den.multiply(o.den)
        );
    }

    public BigFraction multiply(BigFraction o) {
        if (this.infinite && o.infinite) {
            return this.num.signum() == o.num.signum()
                    ? BigFraction.INFINITY
                    : BigFraction.MINUS_INFINITY;
        }

        if (this.infinite) {
            if (o.num.signum() == 0) {
                throw new ArithmeticException("0 * Infinity is undefined");
            }
            return this.num.signum() == o.num.signum()
                    ? BigFraction.INFINITY
                    : BigFraction.MINUS_INFINITY;
        }

        if (o.infinite) {
            if (this.num.signum() == 0) {
                throw new ArithmeticException("0 * Infinity is undefined");
            }
            return this.num.signum() == o.num.signum()
                    ? BigFraction.INFINITY
                    : BigFraction.MINUS_INFINITY;
        }

        return new BigFraction(
                num.multiply(o.num),
                den.multiply(o.den)
        );
    }

    public BigFraction divide(BigFraction o) {
        if (o.num.equals(BigInteger.ZERO) && !o.infinite) {
            throw new ArithmeticException("Division by zero");
        }

        if (this.infinite && o.infinite) {
            throw new ArithmeticException("Infinity / Infinity is undefined");
        }

        if (this.infinite) {
            return this.num.signum() == o.num.signum()
                    ? BigFraction.INFINITY
                    : BigFraction.MINUS_INFINITY;
        }

        if (o.infinite) {
            return BigFraction.ZERO;
        }

        return new BigFraction(
                num.multiply(o.den),
                den.multiply(o.num)
        );
    }

    public BigFraction negate() {
        if (this.infinite) {
            return this.num.signum() > 0
                    ? BigFraction.MINUS_INFINITY
                    : BigFraction.INFINITY;
        }

        return new BigFraction(num.negate(), den);
    }

    /** Exact conversion to int – throws if the fraction is not an integer or is infinite. */
    public int intValueExact() {
        if (infinite) {
            throw new ArithmeticException("Cannot convert infinity to int");
        }

        if (!den.equals(BigInteger.ONE)) {
            throw new ArithmeticException("Fraction is not an integer");
        }

        return num.intValueExact();
    }

    // Returns -1, 0, or 1 as this fraction is negative, zero, or positive.
    public int signum() {
        return num.signum();
    }

    //basic getters 
    public BigInteger numerator() {
        if (infinite) {
            throw new ArithmeticException("Infinity has no numerator");
        }
        return num;
    }

    public BigInteger denominator() {
        if (infinite) {
            throw new ArithmeticException("Infinity has no denominator");
        }
        return den;
    }

    @Override
    public String toString() {
        if (infinite) {
            return num.signum() > 0 ? "inf" : "-inf";
        }

        return den.equals(BigInteger.ONE)
                ? num.toString()
                : num + "/" + den;
    }

    // equality  
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BigFraction)) return false; // check type BigFraction of o
        BigFraction that = (BigFraction) o; //o can be reduced to BigFraction, so do it
        return num.equals(that.num) && den.equals(that.den) && infinite == that.infinite ;
    }

    //hash code for use in hash-based collections
    @Override
    public int hashCode() {
        return Objects.hash(num, den, infinite);
    }

    //to compare two fractions
    // Returns a negative integer → this < o
    // Returns 0 → this == o
    // Returns a positive integer → this > o
    @Override
    public int compareTo(BigFraction o) {
        if (this.infinite && o.infinite) {
            return Integer.compare(this.num.signum(), o.num.signum());
        }

        if (this.infinite) {
            return this.num.signum();
        }

        if (o.infinite) {
            return -o.num.signum();
        }

        return num.multiply(o.den)
                .compareTo(o.num.multiply(den));
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
