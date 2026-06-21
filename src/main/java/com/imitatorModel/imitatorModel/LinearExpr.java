package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

public final class LinearExpr {

    private final List<Pair<VariableType, BigFraction>> terms;
    private final BigFraction constant;

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
