package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComplexConstraint {

    public enum LogicalOperator {
        AND,
        OR
    }

    private List<Constraint> constraints = new ArrayList<Constraint>();
    private  List<LogicalOperator> operators = new ArrayList<LogicalOperator>(); 
    // private Boolean isFalse; 

        /*
    For our implementation, because there is no mechanism to deal with bracket and order of operation, we need to keep the invariant that 
    AND will never follow after an OR. This makes the translation to imitator model later easier. 

    (.. and .. and .. and ..) and (.. or .. or .. or .. )
    */

    public static boolean hasAndAfterOr(List<LogicalOperator> operators) {
        boolean foundOr = false;

        for (LogicalOperator operator : operators) {
            if (operator == LogicalOperator.OR) {
                foundOr = true;
            } else if (foundOr && operator == LogicalOperator.AND) {
                return true;
            }
        }

        return false;
    }

    public ComplexConstraint() {
    }

    public ComplexConstraint(ComplexConstraint other) {
        this.constraints = new ArrayList<>(other.getConstraints());
        this.operators = new ArrayList<>(other.getOperators());
    }

    public ComplexConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    public ComplexConstraint(List<Constraint> constraints, List<LogicalOperator> operators) {
        validateInitialCounts(constraints, operators);

        this.constraints.addAll(constraints);
        this.operators.addAll(operators);

        validateOperatorOrder();
    }

    public ComplexConstraint(List<Constraint> constraints) {
        this.constraints.addAll(constraints);

        if (constraints.size() > 1) {
            operators.addAll(Collections.nCopies(
                    constraints.size() - 1,
                    LogicalOperator.AND));
        }
    }

    public void addConstraint(Constraint constraint) {
        addConstraint(constraint, LogicalOperator.AND);
    }

    public void addConstraint(Constraint constraint, LogicalOperator operator) {
        if (!constraints.isEmpty()) {
            operators.add(operator);
        }

        constraints.add(constraint);

        validateOperatorOrder();
    }

    public void addConstraints(List<Constraint> constraints) {
        addConstraints(
                constraints,
                Collections.nCopies(constraints.size(), LogicalOperator.AND));
    }

    public void addConstraints(
            List<Constraint> constraints,
            List<LogicalOperator> operators) {

        validateAdditionalCounts(constraints, operators);

        this.constraints.addAll(constraints);
        this.operators.addAll(operators);

        validateOperatorOrder();
    }

    public void addConstraints(ComplexConstraint complexConstraint) {
        addConstraints(complexConstraint, LogicalOperator.AND);
    }

    public void addConstraints(
            ComplexConstraint complexConstraint,
            LogicalOperator operator) {

        if (constraints.isEmpty()) {
            constraints.addAll(complexConstraint.getConstraints());
            operators.addAll(complexConstraint.getOperators());
        } else {
            constraints.addAll(complexConstraint.getConstraints());
            operators.add(operator);
            operators.addAll(complexConstraint.getOperators());
        }

        validateOperatorOrder();
    }

    private void validateInitialCounts(
            List<Constraint> constraints,
            List<LogicalOperator> operators) {

        if (constraints.size() != operators.size() + 1) {
            throw new IllegalArgumentException(
                    "The number of operators must be one less than the number of constraints.");
        }
    }

    private void validateAdditionalCounts(
            List<Constraint> constraints,
            List<LogicalOperator> operators) {

        boolean valid = this.constraints.isEmpty()
                ? constraints.size() == operators.size() + 1
                : constraints.size() == operators.size();

        if (!valid) {
            throw new IllegalArgumentException(
                    this.constraints.isEmpty()
                            ? "The number of operators must be one less than the number of constraints."
                            : "The number of operators must equal the number of constraints when appending.");
        }
    }

    private void validateOperatorOrder() {
        if (hasAndAfterOr(operators)) {
            System.err.println(
                    "Invalid ComplexConstraint: AND cannot follow OR.");
        }
    }


    public boolean haveDisjunction(){
        return operators.contains(LogicalOperator.OR);
    }


    public List<Constraint> getConstraints() {
        return this.constraints;
    }

    public List<LogicalOperator> getOperators() {
        return this.operators ;
    }

    public Boolean isFalse(){
        return this.isFalse ;
    }


	public String toIMITATOR(){          // only support & because imitator guard doesnt support OR
        if (constraints.isEmpty()){
            return "True";
        }else{
            StringBuilder sb = new StringBuilder();
            sb.append(constraints.get(0).toIMITATOR());

            for (int i = 0; i < operators.size(); i++) {
                sb.append(operators.get(i) == LogicalOperator.AND ? " & " : " | ")
                  .append(constraints.get(i + 1).toIMITATOR());
            }

            return sb.toString();
        }
	}

}
