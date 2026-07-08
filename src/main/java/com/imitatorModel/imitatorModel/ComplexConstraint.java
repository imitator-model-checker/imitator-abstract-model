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
    // protected ComplexConstraint() {
    // }

    // protected ComplexConstraint(List<Constraint> constraints) {

    // }


    // protected ComplexConstraint(Constraint constraint) {

    // }
    public static ComplexConstraint of(Constraint c) {
        return new ConstraintNode(c);
    }

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

    // @Override
    public String toIMITATOR() {
        return print(this);
    }

    private static String print(ComplexConstraint node) {

        if (node instanceof ConstraintNode c) {
            return c.getConstraint().toIMITATOR();
        }

        if (node instanceof AndNode and) {
            return "(" +
                    and.getChildren().stream()
                            .map(ComplexConstraint::print)
                            .collect(Collectors.joining(" & "))
                    + ")";
        }

        if (node instanceof OrNode or) {
            return "(" +
                    or.getChildren().stream()
                            .map(ComplexConstraint::print)
                            .collect(Collectors.joining(" | "))
                    + ")";
        }

        throw new IllegalArgumentException();
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

    // public enum LogicalOperator {
    //     AND,
    //     OR
    // }

    // private List<Constraint> constraints = new ArrayList<Constraint>();
    // private  List<LogicalOperator> operators = new ArrayList<LogicalOperator>(); 
    // // private Boolean isFalse; 

    //     /*
    // For our implementation, because there is no mechanism to deal with bracket and order of operation, we need to keep the invariant that 
    // AND will never follow after an OR. This makes the translation to imitator model later easier. 

    // (.. and .. and .. and ..) and (.. or .. or .. or .. )
    // */

    // public static boolean hasAndAfterOr(List<LogicalOperator> operators) {
    //     boolean foundOr = false;

    //     for (LogicalOperator operator : operators) {
    //         if (operator == LogicalOperator.OR) {
    //             foundOr = true;
    //         } else if (foundOr && operator == LogicalOperator.AND) {
    //             return true;
    //         }
    //     }

    //     return false;
    // }

    // public ComplexConstraint() {
    // }

    // public ComplexConstraint(ComplexConstraint other) {
    //     this.constraints = new ArrayList<>(other.getConstraints());
    //     this.operators = new ArrayList<>(other.getOperators());
    // }

    // public ComplexConstraint(Constraint constraint) {
    //     constraints.add(constraint);
    // }

    // public ComplexConstraint(List<Constraint> constraints, List<LogicalOperator> operators) {
    //     validateInitialCounts(constraints, operators);

    //     this.constraints.addAll(constraints);
    //     this.operators.addAll(operators);

    //     validateOperatorOrder();
    // }

    // public ComplexConstraint(List<Constraint> constraints) {
    //     this.constraints.addAll(constraints);

    //     if (constraints.size() > 1) {
    //         operators.addAll(Collections.nCopies(
    //                 constraints.size() - 1,
    //                 LogicalOperator.AND));
    //     }
    // }

    // public void addConstraint(Constraint constraint) {
    //     addConstraint(constraint, LogicalOperator.AND);
    // }

    // public void addConstraint(Constraint constraint, LogicalOperator operator) {
    //     if (!constraints.isEmpty()) {
    //         operators.add(operator);
    //     }

    //     constraints.add(constraint);

    //     validateOperatorOrder();
    // }

    // public void addConstraints(List<Constraint> constraints) {
    //     addConstraints(
    //             constraints,
    //             Collections.nCopies(constraints.size(), LogicalOperator.AND));
    // }

    // public void addConstraints(
    //         List<Constraint> constraints,
    //         List<LogicalOperator> operators) {

    //     validateAdditionalCounts(constraints, operators);

    //     this.constraints.addAll(constraints);
    //     this.operators.addAll(operators);

    //     validateOperatorOrder();
    // }

    // public void addConstraints(ComplexConstraint complexConstraint) {
    //     addConstraints(complexConstraint, LogicalOperator.AND);
    // }

    // public void addConstraints(
    //         ComplexConstraint complexConstraint,
    //         LogicalOperator operator) {

    //     if (constraints.isEmpty()) {
    //         constraints.addAll(complexConstraint.getConstraints());
    //         operators.addAll(complexConstraint.getOperators());
    //     } else {
    //         constraints.addAll(complexConstraint.getConstraints());
    //         operators.add(operator);
    //         operators.addAll(complexConstraint.getOperators());
    //     }

    //     validateOperatorOrder();
    // }

    // private void validateInitialCounts(
    //         List<Constraint> constraints,
    //         List<LogicalOperator> operators) {

    //     if (constraints.size() != operators.size() + 1) {
    //         throw new IllegalArgumentException(
    //                 "The number of operators must be one less than the number of constraints.");
    //     }
    // }

    // private void validateAdditionalCounts(
    //         List<Constraint> constraints,
    //         List<LogicalOperator> operators) {

    //     boolean valid = this.constraints.isEmpty()
    //             ? constraints.size() == operators.size() + 1
    //             : constraints.size() == operators.size();

    //     if (!valid) {
    //         throw new IllegalArgumentException(
    //                 this.constraints.isEmpty()
    //                         ? "The number of operators must be one less than the number of constraints."
    //                         : "The number of operators must equal the number of constraints when appending.");
    //     }
    // }

    // private void validateOperatorOrder() {
    //     if (hasAndAfterOr(operators)) {
    //         System.err.println(
    //                 "Invalid ComplexConstraint: AND cannot follow OR.");
    //     }
    // }


    // public boolean haveDisjunction(){
    //     return operators.contains(LogicalOperator.OR);
    // }


    // public List<Constraint> getConstraints() {
    //     return this.constraints;
    // }

    // public List<LogicalOperator> getOperators() {
    //     return this.operators ;
    // }

    // public Boolean isFalse(){
    //     return this.isFalse ;
    // }


	// public String toIMITATOR(){          // only support & because imitator guard doesnt support OR
    //     if (constraints.isEmpty()){
    //         return "True";
    //     }else{
    //         StringBuilder sb = new StringBuilder();
    //         sb.append(constraints.get(0).toIMITATOR());

    //         for (int i = 0; i < operators.size(); i++) {
    //             sb.append(operators.get(i) == LogicalOperator.AND ? " & " : " | ")
    //               .append(constraints.get(i + 1).toIMITATOR());
    //         }

    //         return sb.toString();
    //     }
	// }

}
