package com.imitatorModel.imitatorModel;

import java.util.List;

public class ConstraintNode extends ComplexConstraint {
    private final Constraint constraint;

    public ConstraintNode(Constraint constraint) {
        this.constraint = constraint;
    }

    public Constraint getConstraint() {
        return constraint;
    }

        @Override
    public List<ComplexConstraint> splitDisjunction() {
        return List.of(this);
    }
}