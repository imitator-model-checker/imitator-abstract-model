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

    public ComplexConstraint() {

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
    }

    public void addConstraint(Constraint newConstraint){

        if (this.constraints.isEmpty()) {
            this.constraints.add(newConstraint);   // dont need to add the and/or in this case
  
        }
        else {
            this.constraints.add(newConstraint); 
            this.operators.add(LogicalOperator.AND); 
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
