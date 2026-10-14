package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.imitatorModel.bigFraction.BigFraction;

/**
 * Immutable linear expression {@code c1*v1 + ... + cn*vn + constant}.
 * Terms are normalised on construction: like terms are merged and zero coefficients dropped,
 * so {@code x + x} and {@code 2 * x} are equal.
 */
public final class LinearExpr implements UpdateTerm, Term {

    // insertion-ordered, normalised (no zero coefficient), unmodifiable
    private final Map<VariableType, BigFraction> coefficients;
    private final BigFraction constant;

    public static final LinearExpr ZERO = new LinearExpr(BigFraction.ZERO);
    public static final LinearExpr ONE = new LinearExpr(BigFraction.ONE);
    public static final LinearExpr INFINITY = new LinearExpr(BigFraction.INFINITY);
    public static final LinearExpr MINUS_INFINITY = new LinearExpr(BigFraction.MINUS_INFINITY);

    private LinearExpr(Map<VariableType, BigFraction> normalisedCoefficients, BigFraction constant, boolean alreadyNormalised) {
        this.coefficients = Collections.unmodifiableMap(normalisedCoefficients);
        this.constant = Objects.requireNonNull(constant);
    }

    public LinearExpr(BigFraction constant) {
        this(new LinkedHashMap<>(), constant, true);
    }

    public LinearExpr(VariableType v) {
        this(v, BigFraction.ONE, BigFraction.ZERO);
    }

    public LinearExpr(VariableType v, BigFraction c) {
        this(v, c, BigFraction.ZERO);
    }

    public LinearExpr(VariableType v, BigFraction c, BigFraction constant) {
        this(List.of(new Pair<>(v, c)), constant);
    }

    public LinearExpr(List<? extends VariableType> variables,
                      List<BigFraction> coefficients,
                      BigFraction constant) {
        this(zip(variables, coefficients), constant);
    }

    public LinearExpr(List<Pair<VariableType, BigFraction>> terms,
                      BigFraction constant) {
        this(normalise(terms), constant, true);
    }

    private static List<Pair<VariableType, BigFraction>> zip(List<? extends VariableType> variables,
                                                             List<BigFraction> coefficients) {
        if (variables.size() != coefficients.size()) {
            throw new IllegalArgumentException(
                "variables and coefficients must have the same size");
        }

        List<Pair<VariableType, BigFraction>> terms = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            terms.add(new Pair<>(variables.get(i), coefficients.get(i)));
        }
        return terms;
    }

    // Merge like terms (same variable name) and drop zero coefficients
    private static Map<VariableType, BigFraction> normalise(List<Pair<VariableType, BigFraction>> terms) {
        Map<VariableType, BigFraction> merged = new LinkedHashMap<>();
        for (Pair<VariableType, BigFraction> term : terms) {
            merged.merge(Objects.requireNonNull(term.getFirst()), term.getSecond(), BigFraction::add);
        }
        merged.values().removeIf(c -> c.equals(BigFraction.ZERO));
        return merged;
    }

    @Override
    public LinearExpr expr() {
        return this;
    }

    /** Terms as (variable, coefficient) pairs, in insertion order. Never contains a zero coefficient. */
    public List<Pair<VariableType, BigFraction>> getTerms() {
        return coefficients.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), e.getValue()))
                .toList();
    }

    public Map<VariableType, BigFraction> getCoefficients() {
        return coefficients;
    }

    public BigFraction getConstant() {
        return constant;
    }

    public boolean isConstant() {
        return coefficients.isEmpty();
    }

    /** True if the constant is +inf: the whole expression is +inf whatever the variables. */
    public boolean isPlusInfinity() {
        return constant.isInfinite() && constant.signum() > 0;
    }

    /** True if the constant is -inf: the whole expression is -inf whatever the variables. */
    public boolean isMinusInfinity() {
        return constant.isInfinite() && constant.signum() < 0;
    }

    public LinearExpr addTerm(VariableType variable, BigFraction coefficient) {
        List<Pair<VariableType, BigFraction>> newTerms = new ArrayList<>(getTerms());
        newTerms.add(new Pair<>(variable, coefficient));
        return new LinearExpr(newTerms, constant);
    }

    public LinearExpr add(BigFraction constant){ 
        return new LinearExpr(new LinkedHashMap<>(coefficients), this.constant.add(constant), true);
    }
    
    public LinearExpr add(VariableType variable){ 
        return addTerm(variable, BigFraction.ONE);
    }

    public LinearExpr add(LinearExpr exp){ 
        List<Pair<VariableType, BigFraction>> newTerms = new ArrayList<>(getTerms());
        newTerms.addAll(exp.getTerms());
        return new LinearExpr(newTerms, this.constant.add(exp.constant));
    }

    public LinearExpr minus(LinearExpr exp) {
        return add(exp.times(BigFraction.ONE.negate()));
    }

    @Override
    public LinearExpr times(BigFraction k) {
        List<Pair<VariableType, BigFraction>> newTerms = getTerms().stream()
                .map(p -> new Pair<>(p.getFirst(), p.getSecond().multiply(k)))
                .collect(Collectors.toList());
        return new LinearExpr(newTerms, constant.multiply(k));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof LinearExpr other)) {
            return false;
        }

        return constant.equals(other.constant)
                && coefficients.equals(other.coefficients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constant, coefficients);
    }

    // Method to format the linear term as specified
    public String toIMITATOR() {
        if (constant.isInfinite()) {
            throw new IllegalStateException(
                "Infinite expression cannot be printed in IMITATOR: " + this);
        }
        return format();
    }

    private String format() {
        List<String> parts = new ArrayList<>();

        for (Map.Entry<VariableType, BigFraction> term : coefficients.entrySet()) {
            String variableName = term.getKey().getName();
            BigFraction coefficient = term.getValue();
            parts.add(coefficient.equals(BigFraction.ONE)
                    ? variableName
                    : coefficient + " * " + variableName);
        }

        if (!constant.equals(BigFraction.ZERO) || parts.isEmpty()) {
            parts.add(constant.toString());
        }

        return String.join(" + ", parts);
    }

    @Override
    public String toString() {
        return format();
    }
}
