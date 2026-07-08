package com.imitatorModel.imitatorModel;

import java.util.List;

public class AndNode extends ComplexConstraint {
    private final List<ComplexConstraint> children;

    public AndNode(List<ComplexConstraint>  children) {
        // this.children = List.copyOf(children);
        this.children = children;
    }

    public static AndNode fromConstraints(List<Constraint> constraints) {
        return new AndNode(
            constraints.stream()
                    .map(c -> (ComplexConstraint) new ConstraintNode(c))
                    .toList()
        );
    }

    public List<ComplexConstraint> getChildren() {
        return children;
    }

        @Override
    public List<ComplexConstraint> splitDisjunction() {
        return List.of(this);
    }
}