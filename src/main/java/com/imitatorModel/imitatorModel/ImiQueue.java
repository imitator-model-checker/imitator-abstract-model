package com.imitatorModel.imitatorModel;

import java.math.BigInteger;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * IMITATOR FIFO queue variable ({@code q : rational queue;}), initialised to the empty queue {@code queue()}.
 * Meant to record values (e.g. timestamps) along a run: the accepting states printed by
 * {@code -states-description-accepting} show the content in insertion order, e.g. {@code q = queue([1, 7/2])}.
 * <p>
 * Only update operations are provided: {@link #push} ({@code queue_push(e, q)}, a side effect),
 * {@link #clear} ({@code q <- queue()}) and {@link #assign(ImiQueue)} (copy by value).
 * Elements must be discrete: IMITATOR rejects clocks and parameters in {@code queue_push}, and does not convert
 * between {@code int} and {@code rational}.
 */
public final class ImiQueue extends VariableType {

    public enum ElementType {
        RATIONAL("rational"), INT("int"), BOOL("bool");

        private final String imitatorName;

        ElementType(String imitatorName) {
            this.imitatorName = imitatorName;
        }

        public String toIMITATOR() {
            return imitatorName;
        }
    }

    private final ElementType elementType;

    public ImiQueue(String name, ElementType elementType) {
        super(name);
        if (elementType == null) {
            throw new IllegalArgumentException("Queue " + name + " needs an element type");
        }
        this.elementType = elementType;
    }

    public ElementType getElementType() {
        return elementType;
    }

    @Override
    public String getIMITATORType() {
        return elementType.toIMITATOR() + " queue";
    }

    @Override
    public boolean is_discrete_initially_0() {
        return true;
    }

    @Override
    public String initialValueToIMITATOR() {
        return QueueExpr.EMPTY;
    }

    /** {@code queue_push(value, this)} (rational or int queue). */
    public Update push(Term value) {
        LinearExpr element = value.expr();
        checkNumericElement(element);
        return new Update(this, QueueExpr.push(element, this));
    }

    public Update push(long value) {
        return push(new LinearExpr(BigFraction.of(value)));
    }

    public Update push(BigFraction value) {
        return push(new LinearExpr(value));
    }

    /** {@code queue_push(value, this)} (bool queue). */
    public Update push(boolean value) {
        return push(BooleanExpr.of(value));
    }

    public Update push(ImiBoolean value) {
        return push(BooleanExpr.of(value));
    }

    public Update push(BooleanExpr value) {
        if (elementType != ElementType.BOOL) {
            throw new IllegalArgumentException("Cannot push a boolean into " + getIMITATORType() + " " + getName());
        }
        return new Update(this, QueueExpr.push(value, this));
    }

    /** {@code this <- queue()} */
    public Update clear() {
        return new Update(this, QueueExpr.empty());
    }

    /** {@code this <- other}: copy by value, later pushes to one queue do not affect the other. */
    public Update assign(ImiQueue other) {
        if (other.elementType != elementType) {
            throw new IllegalArgumentException(
                "Cannot assign " + other.getIMITATORType() + " " + other.getName()
                + " to " + getIMITATORType() + " " + getName());
        }
        return new Update(this, QueueExpr.copyOf(other));
    }

    private void checkNumericElement(LinearExpr element) {
        if (elementType == ElementType.BOOL) {
            throw new IllegalArgumentException("Cannot push a number into " + getIMITATORType() + " " + getName());
        }
        if (element.getConstant().isInfinite()) {
            throw new IllegalArgumentException("Cannot push an infinite value into " + getName());
        }
        for (VariableType v : element.getCoefficients().keySet()) {
            if (v instanceof Clock || v instanceof Parameter) {
                throw new IllegalArgumentException(
                    "Queue element may only use discrete variables, found " + v.getIMITATORType() + " " + v.getName()
                    + " (record the time through a discrete variable instead)");
            }
            if (elementType == ElementType.RATIONAL && v instanceof Int) {
                throw new IllegalArgumentException("Cannot push int " + v.getName() + " into rational queue " + getName());
            }
            if (elementType == ElementType.INT && !(v instanceof Int)) {
                throw new IllegalArgumentException(
                    "Cannot push " + v.getIMITATORType() + " " + v.getName() + " into int queue " + getName());
            }
        }
        if (elementType == ElementType.INT) {
            boolean integral = element.getConstant().denominator().equals(BigInteger.ONE)
                    && element.getCoefficients().values().stream()
                              .allMatch(k -> k.denominator().equals(BigInteger.ONE));
            if (!integral) {
                throw new IllegalArgumentException("Cannot push non-integer " + element + " into int queue " + getName());
            }
        }
    }
}
