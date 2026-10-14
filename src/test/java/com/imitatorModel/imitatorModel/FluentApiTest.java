package com.imitatorModel.imitatorModel;

import static com.imitatorModel.imitatorModel.Imi.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.imitatorModel.Main;
import com.imitatorModel.bigFraction.BigFraction;

/** §2: fluent API. */
class FluentApiTest {

    @Test
    void termArithmeticAndComparisons() {
        Clock t = new Clock("t");
        Parameter p = new Parameter("p");
        assertEquals("t <= 2 * p + 5", t.le(p.times(2).plus(5)).toIMITATOR());
        assertEquals("t - p", t.minus(p).toString().replace("+ -1 * p", "- p"));
        assertEquals("t + -1 * p < 1/2", t.minus(p).lt(c(1, 2)).toIMITATOR());
        assertEquals("-1 * t", t.neg().toIMITATOR());
        assertEquals("t >= 3", t.cmp(Operator.GE, 3).toIMITATOR());
        assertEquals(new LinearExpr(t, BigFraction.of(3, 2)), t.times(q(3, 2)));
    }

    @Test
    void combinators() {
        Clock t = new Clock("t");
        ImiBoolean b = new ImiBoolean("b");
        assertEquals("t >= 1 & not (b)", and(t.ge(1), b.isFalse()).toIMITATOR());
        assertEquals("t < 1 | b", or(t.lt(1), b.isTrue()).toIMITATOR());
        assertEquals("t < 1", not(t.ge(1)).toIMITATOR());
        assertEquals("t >= 1 & t <= 2", t.ge(1).and(t.le(2)).toIMITATOR());
        assertSame(TRUE, and(TRUE));
    }

    @Test
    void fluentUpdates() {
        Clock x = new Clock("x");
        Rational cur = new Rational("cur");
        ImiBoolean flag = new ImiBoolean("flag");
        assertEquals("x <- 0", x.reset().toIMITATOR());
        assertEquals("cur <- cur + 1", cur.assign(cur.plus(1)).toIMITATOR());
        assertEquals("flag <- True", flag.assign(true).toIMITATOR());
        assertEquals("if flag then cur <- 2 end ", cur.assign(2).when(flag.isTrue()).toIMITATOR());
        assertEquals(" do {x <- 0; flag <- False}", updates(x.reset(), flag.assign(false)).toIMITATOR());
    }

    @Test
    void builderMatchesClassicConstruction() {
        Clock t = new Clock("t");
        Rational m = new Rational("m");
        ImiBoolean lock = new ImiBoolean("lock");
        Action a = new Action("a");

        Location classic = new Location("l1");
        Location target = new Location("l2");
        ComplexConstraint guard = AndNode.fromConstraints(List.of(
                new Constraint(m.expr(), Operator.LE, t.expr()),
                new Constraint(t.expr(), Operator.LT, new LinearExpr(BigFraction.of(5)))));
        guard = guard.addConstraint(new Constraint(lock));
        classic.addTransition(new Transition(guard, a,
                new ListUpdates(List.of(new Update(lock, Boolean.FALSE), new Update(t, LinearExpr.ZERO))), target));

        Location fluent = new Location("l1");
        fluent.on(a).when(m.le(t), t.lt(5), lock.isTrue()).doing(lock.assign(false), t.reset()).goTo(target);

        assertEquals(classic.toIMITATOR(), fluent.toIMITATOR());
    }

    @Test
    void builderDeclaresActions() {
        PTA pta = new PTA("monitor");
        Location l1 = pta.loc("l1");
        l1.on(new Action("a")).goTo(l1);
        assertTrue(pta.getActions().contains(new Action("a")));

        // location not yet in a PTA: declared when added
        Location l2 = new Location("l2");
        l2.on(new Action("b"), new Action("c")).goTo(l1);
        assertFalse(pta.getActions().contains(new Action("b")));
        pta.addLocation(l2);
        assertTrue(pta.getActions().containsAll(List.of(new Action("b"), new Action("c"))));

        // silent transitions declare nothing
        l1.silent().when(TRUE).goTo(l2);
        assertEquals(3, pta.getActions().size());
        assertTrue(l1.toIMITATOR().contains("when True goto l2;"));
    }

    @Test
    void autoDeclarationCanBeDisabled() {
        PTA monitor = new PTA("monitor").setAutoDeclareActions(false);
        monitor.actions(new Action("a"));
        Location l1 = monitor.loc("l1");
        l1.on(new Action("a")).goTo(l1);
        l1.on(new Action("z")).goTo(l1);           // not declared: stays undeclared
        assertEquals(Set.of(new Action("a")), monitor.getActions());

        // also for a location added after its transitions were built
        Location l2 = new Location("l2");
        l2.on(new Action("y")).goTo(l1);
        monitor.addLocation(l2);
        assertEquals(Set.of(new Action("a")), monitor.getActions());
    }

    @Test
    void onCollectionOfActions() {
        PTA pta = new PTA("monitor");
        Location l1 = pta.loc("l1");
        l1.on(List.of(new Action("b"), new Action("a"))).when(TRUE).goTo(l1);
        assertTrue(l1.toIMITATOR().contains("when True sync a goto l1;\n\twhen True sync b goto l1;"), l1.toIMITATOR());
        // an empty collection gives an unsynchronized transition, like new Transition(...) without action
        Location l2 = pta.loc("l2");
        l2.on(Set.of()).goTo(l1);
        assertEquals("loc l2: invariant True\n\twhen True goto l1;", l2.toIMITATOR());
    }

    @Test
    void locationAndPtaBuilders() {
        ImitatorModel model = new ImitatorModel();
        Clock x = model.addClock("x");
        Clock y = model.addClock("y");
        PTA pta = model.automaton("a");
        Location l = pta.loc("l").invariant(x.le(3)).urgent().accepting().stop(y).flow(x, c(2));
        assertSame(l, pta.loc("l"));
        assertEquals("urgent accepting loc l: invariant x <= 3 stop{y} flow{x' = 2}", l.toIMITATOR());
        assertSame(pta, model.getPTA("a"));
        assertThrows(IllegalArgumentException.class, () -> model.automaton("a"));
    }

    @Test
    void trainGateExampleIsValidImitator() throws Exception {
        String imi = Main.trainGate().toIMITATOR();
        assertTrue(imi.contains("actions: approach, enter, leave;"), imi);
        ImitatorCheck.assertSyntaxOk(imi);
    }
}
