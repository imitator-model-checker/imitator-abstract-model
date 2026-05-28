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

        /*
    For our implementation, because there is no mechanism to deal with bracket and order of operation, we need to keep the invariant that 
    AND will never follow after an OR. This makes the translation to imitator model later easier. 
    */


    public static boolean hasAndAfterOr(
            List<LogicalOperator> operators) {

        boolean foundOr = false;

        for (LogicalOperator operator : operators) {

            if (operator == LogicalOperator.OR) {
                foundOr = true;
            }

            if (foundOr
                    && operator == LogicalOperator.AND) {
                return true;
            }
        }

        return false;
    }

    public ComplexConstraint() {

    }

    public ComplexConstraint(ComplexConstraint complexConstraint) {
        this.constraints = new ArrayList<>(complexConstraint.getConstraints());
        this.operators = new ArrayList<>(complexConstraint.getOperators());
    }

    public ComplexConstraint(Constraint constraint) {
        this.constraints.add(constraint);
    }

    public ComplexConstraint(List<Constraint> constraints, List<LogicalOperator> operators) {
        if (constraints.size() != operators.size() + 1) {
            throw new IllegalArgumentException("The number of operators must be one less than the number of constraints.");
        }
        this.constraints.addAll(constraints);
        this.operators.addAll(operators);

        if (hasAndAfterOr(this.operators) == true){
           System.err.println("Invalid ComplexConstraint: AND cannot follow after OR");
        }
    }

    public ComplexConstraint(List<Constraint> constraints) {
        if (constraints.size() <= 1) {
            this.constraints.addAll(constraints);
        }
        else{        // number of constraints > 1
            this.constraints.addAll(constraints);
            this.operators.addAll(
                Collections.nCopies(constraints.size() - 1, LogicalOperator.AND)
            );
        }

    }

    public void addConstraint(Constraint newConstraint, LogicalOperator operator){

        if (this.constraints.isEmpty()) {
            this.constraints.add(newConstraint);  // dont need to add the and/or in this case
        }
        else {
            this.constraints.add(newConstraint);  
            this.operators.add(operator);
        }

        if (hasAndAfterOr(this.operators) == true){
           System.err.println("Invalid ComplexConstraint: AND cannot follow after OR");
        }
    }

    public void addConstraint(Constraint newConstraint){

        if (this.constraints.isEmpty()) {
            this.constraints.add(newConstraint);   // dont need to add the and/or in this case
  
        }
        else {
            this.constraints.add(newConstraint); 
            this.operators.add(LogicalOperator.AND); 
        }

        if (hasAndAfterOr(this.operators) == true){
           System.err.println("Invalid ComplexConstraint: AND cannot follow after OR");
        }
    }

    public void addConstraints(List<Constraint> constraints, List<LogicalOperator> operators){
        if (this.constraints.isEmpty()) {
            if (constraints.size() != operators.size() + 1) {
                throw new IllegalArgumentException("The number of operators must be one less than the number of constraints.");
            }
            this.constraints.addAll(constraints);
            this.operators.addAll(operators);
        }
         else {
            if (constraints.size() != operators.size()) {
                throw new IllegalArgumentException("The number of operators must be equal to the number of constraints when adding to an existing ComplexConstraint.");
            }
            this.constraints.addAll(constraints);
            this.operators.addAll(operators);
        }

        if (hasAndAfterOr(this.operators) == true){
           System.err.println("Invalid ComplexConstraint: AND cannot follow after OR");
        }
    }

    public void addConstraints(List<Constraint> constraints){
        if (this.constraints.isEmpty()) {
            this.constraints.addAll(constraints);
            this.operators.addAll(Collections.nCopies(constraints.size(), LogicalOperator.AND));
        }
         else {
            this.constraints.addAll(constraints);
            this.operators.addAll(Collections.nCopies(constraints.size(), LogicalOperator.AND));
        }

        if (hasAndAfterOr(this.operators) == true){
           System.err.println("Invalid ComplexConstraint: AND cannot follow after OR");
        }
    }

    public void addConstraints(ComplexConstraint complexConstraint, LogicalOperator operator){
        if (this.constraints.isEmpty()) { // in fact the operator is not needed here
            this.constraints = complexConstraint.getConstraints();
            this.operators = complexConstraint.getOperators();
        }
         else {
            this.constraints.addAll(complexConstraint.getConstraints());
            this.operators.add(operator);
            this.operators.addAll(complexConstraint.getOperators());
        }

        if (hasAndAfterOr(this.operators) == true){
           System.err.println("Invalid ComplexConstraint: AND cannot follow after OR");
        }
    }

    public void addConstraints(ComplexConstraint complexConstraint){
        if (this.constraints.isEmpty()) { // in fact the operator is not needed here
            this.constraints = complexConstraint.getConstraints();
            this.operators = complexConstraint.getOperators();
        }
         else {
            this.constraints.addAll(complexConstraint.getConstraints());
            this.operators.add(LogicalOperator.AND);
            this.operators.addAll(complexConstraint.getOperators());
        }

        if (hasAndAfterOr(this.operators) == true){
           System.err.println("Invalid ComplexConstraint: AND cannot follow after OR");
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
