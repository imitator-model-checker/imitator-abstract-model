package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;

public class LinearExpr {
    private List<Pair<Variable, BigFraction>> terms;
    private BigFraction constant;

    public LinearExpr(BigFraction constant) {
        this.terms = new ArrayList<>();
        this.constant = constant;
    }

    public LinearExpr(Variable v) {
        this.terms = new ArrayList<>();
        this.constant = BigFraction.ZERO;
        addTerm(v, BigFraction.ONE);
    }


    public LinearExpr(Variable v, BigFraction c) {
        this.terms = new ArrayList<>();
        this.constant = c;
        addTerm(v, c);
    }


    public void addTerm(Variable variable, BigFraction coefficient) {
        terms.add(new Pair<>(variable, coefficient));
    }

    public List<Pair<Variable, BigFraction>> getTerms() {
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
            Pair<Variable, BigFraction> term = terms.get(i);
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
