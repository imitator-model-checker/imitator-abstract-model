package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.transformations.dnf.DNFFactorization;
import org.logicng.formulas.Variable;
import org.logicng.formulas.Literal;
import org.logicng.formulas.And;
import org.logicng.formulas.Or;
public abstract class ComplexConstraint {
    public enum LogicalOperator {
        AND,
        OR
    }

    public static ComplexConstraint of(Constraint c) {
        return new ConstraintNode(c);
    }

    public abstract ComplexConstraint negate();

    // public abstract List<Constraint> getLeaves();

    public ComplexConstraint addConstraint(
            ComplexConstraint other,
            LogicalOperator operator) {

        return switch (operator) {
            case AND -> new AndNode(List.of(this, other));
            case OR  -> new OrNode(List.of(this, other));
        };
    }

    public ComplexConstraint addConstraint(
            ComplexConstraint other) {

        return this.addConstraint(other,LogicalOperator.AND);
    }

    public ComplexConstraint addConstraint(
            Constraint constraint) {

        return this.addConstraint(new ConstraintNode(constraint),LogicalOperator.AND);
    }

    public ComplexConstraint addConstraints(
            List<ComplexConstraint> others,
            LogicalOperator operator) {

        List<ComplexConstraint> allConstraints = new ArrayList<>();
        allConstraints.add(this);
        allConstraints.addAll(others);

        return switch (operator) {
            case AND -> new AndNode(allConstraints);
            case OR  -> new OrNode(allConstraints);
        };
    }
    
    public ComplexConstraint addConstraints(
            List<Constraint> others) {

        List<ComplexConstraint> allConstraints = new ArrayList<>();
        allConstraints.add(this);
        allConstraints.addAll(
            others.stream()
                .map(ConstraintNode::new)
                .toList()
        );

        return new AndNode(allConstraints);      
    }

    public ComplexConstraint toDNF() {
        FormulaFactory f = new FormulaFactory();

        Map<String, Constraint> registry = new HashMap<>();
        collectConstraints(this, registry);

        Formula formula = toFormula(this, f, new HashMap<>());
        Formula dnf = formula.transform(new DNFFactorization());

        return fromFormula(dnf, registry);
    }

    private static void collectConstraints(
            ComplexConstraint node,
            Map<String, Constraint> registry) {

        if (node instanceof ConstraintNode c) {
            Constraint constraint = c.getConstraint();

            if (constraint != Constraint.TRUE &&
                constraint != Constraint.FALSE) {

                registry.putIfAbsent(
                        String.valueOf(constraint.getId()),
                        constraint);
            }
            return;
        }

        if (node instanceof AndNode and) {
            and.getChildren()
            .forEach(child -> collectConstraints(child, registry));
            return;
        }

        if (node instanceof OrNode or) {
            or.getChildren()
            .forEach(child -> collectConstraints(child, registry));
        }
    }

    private static ComplexConstraint fromFormula(
            Formula formula,
            Map<String, Constraint> registry) {

        switch (formula.type()) {

            case TRUE:
                return new ConstraintNode(Constraint.TRUE);

            case FALSE:
                return new ConstraintNode(Constraint.FALSE);

            case LITERAL: {

                Literal literal = (Literal) formula;

                Constraint original = registry.get(literal.variable().name());

                if (original == null) {
                    throw new IllegalStateException(
                            "Unknown constraint id: " + literal.variable().name());
                }
                
                if (literal.phase()) {
                    return new ConstraintNode(original);
                } else {
                    return new ConstraintNode(original.negate());
                }
            }

            case AND: {

                And and = (And) formula;
                List<ComplexConstraint> children = new ArrayList<>();

                for (Formula child : and) {
                    children.add(fromFormula(child, registry));
                }

                return new AndNode(children);
            }

            case OR: {

                Or or = (Or) formula;
                List<ComplexConstraint> children = new ArrayList<>();

                for (Formula child : or) {
                    children.add(fromFormula(child, registry));
                }

                return new OrNode(children);
            }

            default:
                throw new UnsupportedOperationException(
                        formula.type().toString());
        }
    }

    public static   Formula toFormula(
                ComplexConstraint node,
                FormulaFactory f,
                Map<Constraint, Variable> vars) {

            if (node instanceof ConstraintNode c) {

                Constraint constraint = c.getConstraint();

                // Handle known true/false constraints
                if (constraint.getTruthConst() != null) {
                    return constraint.getTruthConst()
                            ? f.verum()
                            : f.falsum();
                }

                return vars.computeIfAbsent(
                        constraint,
                        k -> f.variable(String.valueOf(k.getId()))
                );
            }

            if (node instanceof AndNode and) {
                return f.and(
                    and.getChildren()
                    .stream()
                    .map(child -> toFormula(child, f, vars))
                    .toList()
                );
            }

            if (node instanceof OrNode or) {
                return f.or(
                    or.getChildren()
                    .stream()
                    .map(child -> toFormula(child, f, vars))
                    .toList()
                );
            }

            throw new IllegalArgumentException();
        } 
   
    public String toIMITATOR() {
        return print(this, 0);
    }

    private static final int OR_PRECEDENCE = 1;
    private static final int AND_PRECEDENCE = 2;
    // private static final int ATOM_PRECEDENCE = 3;

    private static String print(ComplexConstraint node, int parentPrecedence) {
        if (node instanceof ConstraintNode c) {
            return c.getConstraint().toIMITATOR();
        }

        if (node instanceof AndNode and) {
            String s = and.getChildren().stream()
                    .map(child -> print(child, AND_PRECEDENCE))
                    .collect(Collectors.joining(" & "));

            return parentPrecedence > AND_PRECEDENCE ? "(" + s + ")" : s;
        }

        if (node instanceof OrNode or) {
            String s = or.getChildren().stream()
                    .map(child -> print(child, OR_PRECEDENCE))
                    .collect(Collectors.joining(" | "));

            return parentPrecedence > OR_PRECEDENCE ? "(" + s + ")" : s;
        }

        throw new IllegalArgumentException("Unknown node: " + node.getClass());
    }

    public boolean haveDisjunction() {
        if (this instanceof OrNode) {
            return true;
        }

        if (this instanceof AndNode and) {
            return and.getChildren()
                      .stream()
                      .anyMatch(ComplexConstraint::haveDisjunction);
        }

        if (this instanceof ConstraintNode) {
            return false;
        }

        return false;
    }
    
    public List<ComplexConstraint> splitDisjunction() {
        return List.of(this);
    }
}