package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

public class ConjunctionOfConstraints {
    private List<Constraint> constraints;

    public ConjunctionOfConstraints() {
        this.constraints = new ArrayList<Constraint>();
    }

    public ConjunctionOfConstraints(Constraint constraint) {

        this.constraints =  new ArrayList<Constraint>();
        this.constraints.add(constraint);
    }

    public ConjunctionOfConstraints(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

	public String toIMITATOR(){
        if (constraints.isEmpty()){
            return "True";
        }else{
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < constraints.size(); i++) {
                sb.append(constraints.get(i).toIMITATOR());

                // Append " & " if it's not the last constraint
                if (i < constraints.size() - 1) {
                    sb.append(" & ");
                }
            }

            return sb.toString();
        }
	}

}
