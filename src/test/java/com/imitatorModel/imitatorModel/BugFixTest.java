package com.imitatorModel.imitatorModel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.imitatorModel.bigFraction.BigFraction;

/** Regression tests for the bugs of API_IMPROVEMENTS.md §1. */
class BugFixTest {

    private static ImitatorModel singleLocationModel(Location l) {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        PTA pta = model.automaton("a");
        pta.addLocation(l);
        pta.setInitialLocation(l);
        return model;
    }

    // §1.1
    @Test
    void booleanConstantPrintsCapitalized() throws Exception {
        ImitatorModel model = singleLocationModel(new Location("l1"));
        model.addVariable(new ImiBoolean("b", true));
        model.addVariable(new ImiBoolean("c", false));
        String imi = model.toIMITATOR();
        assertTrue(imi.contains("b = True: bool;"), imi);
        assertTrue(imi.contains("c = False: bool;"), imi);
        ImitatorCheck.assertSyntaxOk(imi);
    }

    // §1.2
    @Test
    void negatingTruthConstants() {
        assertSame(Constraint.FALSE, Constraint.TRUE.negate());
        assertSame(Constraint.TRUE, Constraint.FALSE.negate());
        assertEquals("False", new ConstraintNode(Constraint.TRUE).negate().toIMITATOR());
        assertEquals("True", new ConstraintNode(Constraint.FALSE).negate().toIMITATOR());

        // a simplified atom (x < 0 is always false for a clock) negates to True
        Clock x = new Clock("x");
        assertEquals("True", new ConstraintNode(new Constraint(x.expr(), Operator.LT)).negate().toIMITATOR());

        Location l = new Location("l1").invariant(new ConstraintNode(Constraint.FALSE).negate());
        assertTrue(l.toIMITATOR().startsWith("loc l1: invariant True"));
    }

    // §1.3
    @Test
    void zeroCoefficientsAreDropped() {
        Clock x = new Clock("x");
        Clock y = new Clock("y");
        assertEquals("y", new LinearExpr(x, BigFraction.ZERO).add(y).toIMITATOR());
        assertEquals("0", new LinearExpr(x, BigFraction.ZERO).toIMITATOR());
        assertEquals("y + 3", new LinearExpr(x, BigFraction.ZERO, BigFraction.of(3)).add(y).toIMITATOR());
    }

    // §1.4
    @Test
    void likeTermsAreMerged() {
        Clock x = new Clock("x");
        LinearExpr xPlusX = new LinearExpr(x).add(x);
        assertEquals(new LinearExpr(x, BigFraction.of(2)), xPlusX);
        assertEquals(new LinearExpr(x, BigFraction.of(2)).hashCode(), xPlusX.hashCode());
        assertEquals("2 * x", xPlusX.toIMITATOR());
        assertEquals(LinearExpr.ZERO, new LinearExpr(x).minus(new LinearExpr(x)));
        // equal atoms share one LogicNG variable: (a | b) & a2 gives a | b & a, not a & a2 | b & a2
        ComplexConstraint a = new ConstraintNode(new Constraint(xPlusX, Operator.LE, LinearExpr.ONE));
        ComplexConstraint a2 = new ConstraintNode(new Constraint(new LinearExpr(x, BigFraction.of(2)), Operator.LE, LinearExpr.ONE));
        ComplexConstraint b = new ConstraintNode(new Constraint(new LinearExpr(x), Operator.GE, LinearExpr.ONE));
        assertEquals("2 * x <= 1 | x >= 1 & 2 * x <= 1", a.or(b).and(a2).toDNF().toIMITATOR());
    }

    // §1.5
    @Test
    void infinityIsSimplifiedOrRejected() {
        Clock x = new Clock("x");
        Clock y = new Clock("y");
        LinearExpr yPlusInf = new LinearExpr(y).add(BigFraction.INFINITY);
        assertEquals("True", new Constraint(x.expr(), Operator.LE, yPlusInf).toIMITATOR());
        assertEquals("False", new Constraint(x.expr(), Operator.GE, yPlusInf).toIMITATOR());
        assertEquals("False", new Constraint(x.expr(), Operator.EQ, yPlusInf).toIMITATOR());
        assertEquals("True", new Constraint(yPlusInf, Operator.GT, x.expr()).toIMITATOR());
        assertEquals("False", new Constraint(LinearExpr.INFINITY, Operator.LT, LinearExpr.INFINITY).toIMITATOR());
        assertEquals("True", new Constraint(LinearExpr.MINUS_INFINITY, Operator.LE, LinearExpr.INFINITY).toIMITATOR());
        assertThrows(IllegalStateException.class, () -> LinearExpr.INFINITY.toIMITATOR());
        assertThrows(IllegalStateException.class, () -> new Update(x, yPlusInf).toIMITATOR());
    }

    // §1.6
    @Test
    void duplicateLocationNamesAreRejected() {
        PTA pta = new PTA("a");
        pta.addLocation(new Location("l1"));
        assertThrows(IllegalArgumentException.class, () -> pta.addLocation(new Location("l1")));
        assertEquals(1, pta.getLocations().size());
        assertEquals(new Location("l1"), pta.getLocation("l1"));
        assertEquals(new PTA("a"), pta);
        assertEquals(new PTA("a").hashCode(), pta.hashCode());
    }

    @Test
    void waitingPlaceholderIsReplacedBySameNameLocation() {
        PTA pta = new PTA("log");
        Location placeholder = new Location("l3");
        placeholder.setIsWaiting(true);
        pta.addLocation(placeholder);
        Location real = new Location("l3");
        pta.addLocation(real);
        assertSame(real, pta.getLocation("l3"));
        assertEquals(1, pta.getLocations().size());
    }

    // §1.7
    @Test
    void addTransitionsAddsItsArgument() {
        Location l1 = new Location("l1");
        Location l2 = new Location("l2");
        l1.addTransitions(List.of(new Transition(null, new Action("a"), null, l2),
                                  new Transition(null, new Action("b"), null, l2)));
        assertEquals(2, l1.getTransitions().size());
    }

    // §1.8
    @Test
    void addVariableKeepsSameTypeAndRejectsOtherType() {
        ImitatorModel model = new ImitatorModel();
        Rational r = model.addRational("r");
        assertSame(r, model.addRational("r"));
        assertSame(r, model.addVariable(new Rational("r", BigFraction.ONE)));
        assertThrows(IllegalArgumentException.class, () -> model.addClock("r"));
        assertThrows(IllegalArgumentException.class, () -> model.addVariable(new ImiBoolean("r")));

        Clock c = new Clock("r");
        assertSame(c, model.replaceVariable(c));
        assertEquals(1, model.getVariables().size());
        assertTrue(model.getVariables().iterator().next() instanceof Clock);
    }

    @Test
    void mergeKeepsHistoricalPolicy() {
        ImitatorModel log = new ImitatorModel();
        log.addVariable(new Rational("logEndTime", BigFraction.of(15)));
        log.addClock("t");
        log.addImiBoolean("x");
        ImitatorModel pattern = new ImitatorModel();
        pattern.addRational("logEndTime");   // same type: kept from log (with its value)
        pattern.addClock("t");
        pattern.addRational("x");             // rational of the right model replaces another type
        pattern.addParameter("p");

        ImitatorModel merged = log.mergeModel(pattern);
        String vars = merged.generateVarDeclaration();
        assertTrue(vars.contains("logEndTime = 15: rational;"), vars);
        assertTrue(vars.contains("x: rational;"), vars);
        assertTrue(vars.contains("p: parameter;"), vars);
    }

    // §1.9
    @Test
    void conditionalUpdateRejectsClocksAndParameters() {
        Clock x = new Clock("x");
        Parameter p = new Parameter("p");
        Rational r = new Rational("r");
        ImiBoolean b = new ImiBoolean("b");

        assertThrows(IllegalArgumentException.class, () -> new Update(x.atom(Operator.LE, LinearExpr.ONE), r, LinearExpr.ONE));
        assertThrows(IllegalArgumentException.class, () -> r.assign(1).when(r.le(p)));
        assertEquals("if r <= 1 & b then r <- 0 end ", r.assign(0).when(r.le(1)).when(b.isTrue()).toIMITATOR());
        assertThrows(IllegalArgumentException.class, () -> p.assign(1));
    }

    // §1.10
    @Test
    void falseGuardsAreDropped() {
        Clock x = new Clock("x");
        Location l2 = new Location("l2");
        Transition t = new Transition(x.lt(0), new Action("a"), null, l2);
        assertEquals("", t.toIMITATOR());
        Transition t2 = new Transition(x.lt(0).or(x.ge(1)), new Action("a"), null, l2);
        assertEquals("\n\twhen x >= 1 sync a goto l2;", t2.toIMITATOR());
    }

    @Test
    void missingInitialLocationHasClearError() {
        ImitatorModel model = new ImitatorModel();
        model.automaton("monitor").loc("l1");
        IllegalStateException e = assertThrows(IllegalStateException.class, model::toIMITATOR);
        assertTrue(e.getMessage().contains("monitor"));
    }

    @Test
    void randomWithDenominatorOne() {
        assertEquals(BigFraction.ONE, BigFraction.random(1));
    }
}
