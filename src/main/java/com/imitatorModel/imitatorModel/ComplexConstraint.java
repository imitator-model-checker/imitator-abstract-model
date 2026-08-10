package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.transformations.dnf.DNFFactorization;

import com.imitatorModel.bigFraction.BigFraction;

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

        // Maps LogicNG variables back to the original constraints.
        Map<Variable, Constraint> registry = new HashMap<>();

        // Maps logically equal constraints to the same LogicNG variable.
        Map<Constraint, Variable> vars = new HashMap<>();

        Formula formula = toFormula(this, f, vars, registry);
        Formula dnf = formula.transform(new DNFFactorization());

        return fromFormula(dnf, registry);
    }

    public static Formula toFormula(
            ComplexConstraint node,
            FormulaFactory f,
            Map<Constraint, Variable> vars,
            Map<Variable, Constraint> registry) {

        if (node instanceof ConstraintNode c) {
            Constraint constraint = c.getConstraint();

            // TRUE / FALSE are represented directly as Boolean constants.
            if (constraint.getTruthConst() != null) {
                return constraint.getTruthConst()
                        ? f.verum()
                        : f.falsum();
            }

            Variable variable = vars.computeIfAbsent(
                    constraint,
                    k -> f.variable("c" + vars.size())
            );

            // Store the reverse mapping so fromFormula()
            // can recover the original Constraint.
            registry.putIfAbsent(variable, constraint);

            return variable;
        }

        if (node instanceof AndNode and) {
            return f.and(
                    and.getChildren()
                            .stream()
                            .map(child ->
                                    toFormula(child, f, vars, registry))
                            .toList()
            );
        }

        if (node instanceof OrNode or) {
            return f.or(
                    or.getChildren()
                            .stream()
                            .map(child ->
                                    toFormula(child, f, vars, registry))
                            .toList()
            );
        }

        throw new IllegalArgumentException();
    }

    private static ComplexConstraint fromFormula(
            Formula formula,
            Map<Variable, Constraint> registry) {

        switch (formula.type()) {

            case TRUE:
                return new ConstraintNode(Constraint.TRUE);

            case FALSE:
                return new ConstraintNode(Constraint.FALSE);

            case LITERAL: {

                Literal literal = (Literal) formula;

                Constraint original =
                        registry.get(literal.variable());

                if (original == null) {
                    throw new IllegalStateException(
                            "Unknown constraint variable: "
                            + literal.variable().name());
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

   
    public String toIMITATOR() {
        return print(this, 0);
    }

    private static final int OR_PRECEDENCE = 1;
    private static final int AND_PRECEDENCE = 2;
    // private static final int ATOM_PRECEDENCE = 3;

    private static String print(ComplexConstraint node, int parentPrecedence) {
        if (node instanceof ConstraintNode c) {
            return c.getConstraint().toIMITATOR();
            // var ct = c.getConstraint();

            // if (ct.getOperator() != Operator.NE || !ct.equals(Constraint.FALSE) || !ct.equals(Constraint.TRUE)) {
            //     return ct.toIMITATOR();
            // }

            // return OrNode.fromConstraints(List.of(
            //         new Constraint(ct.getLeftTerm(), Operator.LT, ct.getRightTerm()),
            //         new Constraint(ct.getLeftTerm(), Operator.GT, ct.getRightTerm())
            // )).toIMITATOR();
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

    // public static ComplexConstraint activationCondition(List<Rational> variables) {
    public ComplexConstraint activationCondition() {
        return new OrNode(
            this.getActivationVariables().stream()
                .<ComplexConstraint>map(variable ->
                    new ConstraintNode(
                        new Constraint(
                            new LinearExpr(variable),
                            Operator.EQ,
                            new LinearExpr(BigFraction.ONE)
                        )
                    ))
                .toList()
        );
    }
    
    public List<Rational> getActivationVariables() {
        Set<String> names = new LinkedHashSet<>();
        collectVariables(this, names);

        return names.stream()
                .map(v -> new Rational(v + "Activate"))
                .toList();
    }

    private static void collectVariables(
            ComplexConstraint node,
            Set<String> names) {

        if (node instanceof ConstraintNode c) {
            Constraint constraint = c.getConstraint();

            collectVariables(constraint.getLeftTerm(), names);
            collectVariables(constraint.getRightTerm(), names);
            return;
        }

        if (node instanceof AndNode and) {
            and.getChildren().forEach(child -> collectVariables(child, names));
            return;
        }

        if (node instanceof OrNode or) {
            or.getChildren().forEach(child -> collectVariables(child, names));
        }
    }

    private static void collectVariables(
            LinearExpr expr,
            Set<String> names) {

        expr.getTerms().stream()
                .map(Pair::getFirst)
                .map(VariableType::getName)
                .forEach(names::add);
    }

}