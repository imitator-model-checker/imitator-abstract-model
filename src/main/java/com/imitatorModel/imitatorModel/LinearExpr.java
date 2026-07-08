package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.imitatorModel.bigFraction.BigFraction;

public final class LinearExpr {

    private final List<Pair<VariableType, BigFraction>> terms;
    private final BigFraction constant;

    public static final LinearExpr ZERO = new LinearExpr(BigFraction.ZERO);
    public static final LinearExpr INFINITY = new LinearExpr(BigFraction.INFINITY);

    public LinearExpr(BigFraction constant) {
        this.terms = List.of();
        this.constant = constant;
    }

    public LinearExpr(VariableType v) {
        this.terms = List.of(new Pair<>(v, BigFraction.ONE));
        this.constant = BigFraction.ZERO;
    }

    public LinearExpr(VariableType v, BigFraction c) {
        this.terms = List.of(new Pair<>(v, c));
        this.constant = BigFraction.ZERO;
    }

    public LinearExpr(VariableType v, BigFraction c, BigFraction constant) {
        this.terms = List.of(new Pair<>(v, c));
        this.constant = constant;
    }

    public LinearExpr(List<? extends VariableType> variables,
                      List<BigFraction> coefficients,
                      BigFraction constant) {

        if (variables.size() != coefficients.size()) {
            throw new IllegalArgumentException(
                "variables and coefficients must have the same size");
        }

        List<Pair<VariableType, BigFraction>> temp = new ArrayList<>();

        for (int i = 0; i < variables.size(); i++) {
            temp.add(new Pair<>(variables.get(i), coefficients.get(i)));
        }

        this.terms = List.copyOf(temp);
        this.constant = constant;
    }

    public LinearExpr(List<Pair<VariableType, BigFraction>> terms,
                      BigFraction constant) {
        this.terms = List.copyOf(terms); // immutable copy
        this.constant = constant;
    }

    public List<Pair<VariableType, BigFraction>> getTerms() {
        return terms;
    }

    public BigFraction getConstant() {
        return constant;
    }

    public LinearExpr addTerm(VariableType variable, BigFraction coefficient) {
        List<Pair<VariableType, BigFraction>> newTerms = new ArrayList<>(terms);
        newTerms.add(new Pair<>(variable, coefficient));
        return new LinearExpr(newTerms, constant);
    }

    public LinearExpr add(BigFraction constant){ 
        return new LinearExpr(this.terms, this.constant.add(constant));
    }
    
    public LinearExpr add(VariableType variable){ 
        List<Pair<VariableType, BigFraction>> newTerms = new ArrayList<>(terms);
        newTerms.add(new Pair<>(variable, BigFraction.ONE));
        return new LinearExpr(newTerms, constant);
    }

     public LinearExpr add(LinearExpr exp){ 
        List<Pair<VariableType, BigFraction>> newTerms = new ArrayList<>(terms);
        newTerms.addAll(exp.terms);
        return new LinearExpr(newTerms, this.constant.add(exp.constant));
    }
   
     public LinearExpr minus(LinearExpr exp){ 
        List<Pair<VariableType, BigFraction>> newTerms = terms.stream()
            .map(p -> new Pair<VariableType, BigFraction>(
                    p.getFirst(),
                    p.getSecond().negate()))
            .collect(Collectors.toList());
        newTerms.addAll(exp.terms);
        return new LinearExpr(newTerms, this.constant.add(exp.constant.negate()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LinearExpr other)) {
            return false;
        }

        return Objects.equals(terms, other.terms)
                && Objects.equals(constant, other.constant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terms, constant);
    }



    // Method to format the linear term as specified
    public String toIMITATOR() {
        StringBuilder sb = new StringBuilder();

        // Append each term in the form coef * variable
        for (int i = 0; i < terms.size(); i++) {
            Pair<VariableType, BigFraction> term = terms.get(i);
            BigFraction coefficient = term.getSecond();
            String variableName = term.getFirst().getName();

            // Append coefficient and variable name
            if(coefficient.equals(BigFraction.ZERO)){
                // Add nothing
            }else if (coefficient.equals(BigFraction.ONE)){
                sb.append(variableName);
            }else {
                sb.append(coefficient).append(" * ").append(variableName);
            }

            // Add " + " if not the last term
            // WARNING: will create an issue if only 0-terms before
            if (i < terms.size() - 1) {
                sb.append(" + ");
            }
        }

        // Add constant term with correct formatting
        if (!constant.equals(BigFraction.ZERO) || terms.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(constant);
        }

        return sb.toString();
    }
}
