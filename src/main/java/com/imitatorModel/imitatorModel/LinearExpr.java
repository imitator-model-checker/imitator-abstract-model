package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

public class LinearExpr {
    private List<Pair<VariableType, BigFraction>> terms;
    private BigFraction constant;

    public LinearExpr(BigFraction constant) {
        this.terms = new ArrayList<>();
        this.constant = constant;
    }

    public LinearExpr(VariableType v) {
        this.terms = new ArrayList<>();
        this.constant = BigFraction.ZERO;
        addTerm(v, BigFraction.ONE);
    }


    public LinearExpr(VariableType v, BigFraction c) {
        this.terms = new ArrayList<>();
        addTerm(v, c);
    }

    public LinearExpr(VariableType v, BigFraction c,  BigFraction constant) {
        this.terms = new ArrayList<>();
        this.constant = constant;
        addTerm(v, c);
    }

    public LinearExpr(List<? extends VariableType> variables, List<BigFraction> coefficients, BigFraction constant) {
        this.terms = new ArrayList<>();
        this.constant = constant;
        for (int i = 0; i < variables.size(); i++) {
            addTerm(variables.get(i), coefficients.get(i));
        }
    }


    public void addTerm(VariableType variable, BigFraction coefficient) {
        terms.add(new Pair<>(variable, coefficient));
    }


    // getter methods

    public List<Pair<VariableType, BigFraction>> getTerms() {
        return terms;
    }

    public BigFraction getConstant() {
        return constant;
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
