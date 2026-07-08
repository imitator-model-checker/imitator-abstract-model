package com.imitatorModel.imitatorModel;

import java.util.List;

public class OrNode extends ComplexConstraint {
    private final List<ComplexConstraint> children;

    public OrNode(List<ComplexConstraint> children) {
        // this.children = List.copyOf(children);
        this.children = children;
    }

    public static OrNode fromConstraints(List<Constraint> constraints) {
        return new OrNode(
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
        return children.stream()
                       .flatMap(child -> child.splitDisjunction().stream())
                       .toList();
    }
}
