package com.imitatorModel.imitatorModel;

import java.util.List;

public class ConstraintNode extends ComplexConstraint {
    private final Constraint constraint;

    public ConstraintNode(Constraint constraint) {
        this.constraint = constraint;
    }

    @Override
    public ComplexConstraint negate() {
        if (constraint.getOperator() == Operator.EQ){
            return OrNode.fromConstraints(List.of(
                    new Constraint(constraint.getLeftTerm(), Operator.LT, constraint.getRightTerm()),
                    new Constraint(constraint.getLeftTerm(), Operator.GT, constraint.getRightTerm())));
        }
        return new ConstraintNode(constraint.negate());
    }

    public Constraint getConstraint() {
        return constraint;
    }

        @Override
    public List<ComplexConstraint> splitDisjunction() {
        return List.of(this);
    }
}