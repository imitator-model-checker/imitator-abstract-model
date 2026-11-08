package com.imitatorModel.imitatorModel;

import static com.imitatorModel.imitatorModel.Imi.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.imitatorModel.Main;
import com.imitatorModel.imitatorModel.UppaalCheck.Verdict;

/**
 * IMITATOR → UPPAAL translation ({@code toUppaal}). Expected values are derived by hand; when verifyta
 * ($VERIFYTA_BIN or PATH) and IMITATOR ($IMITATOR_BIN or PATH) are available, the verdicts of both tools are
 * also compared on the same model.
 */
class UppaalTranslationTest {

    // ------------------------------------------------------------------
    // Choice of the model checker
    // ------------------------------------------------------------------

    @Test
    void parametricModelIsForcedToImitator() {
        ImitatorModel model = Main.trainGate();
        assertTrue(model.usesParameters());
        assertEquals(List.of("p_approach", "p_lower"),
                model.getUsedParameters().stream().map(Parameter::getName).toList());
        assertEquals(Backend.IMITATOR, model.preferredBackend());

        UppaalUnsupportedException e = assertThrows(UppaalUnsupportedException.class, model::toUppaal);
        assertTrue(e.getProblems().get(0).startsWith("parameter p_approach"), e.getMessage());
        assertTrue(e.getProblems().stream().anyMatch(p -> p.startsWith("parameter p_lower")));
        assertFalse(model.canUseUppaal());
    }

    @Test
    void undeclaredParameterInGuardIsDetected() {
        ImitatorModel model = new ImitatorModel();
        Clock x = model.addClock("x");
        Parameter p = new Parameter("p");   // not declared, only used
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0");
        a.initial(l0);
        l0.silent().when(x.ge(p)).goTo(l0);
        assertTrue(model.usesParameters());
        assertEquals(Backend.IMITATOR, model.preferredBackend());
    }

    @Test
    void parametricPropertyIsForcedToImitator() {
        ImitatorModel model = simpleModel();
        PTA a = model.getPTA("a");
        Property p = Property.EF(StatePredicate.loc(a, a.getLocation("l1"))).projectResult(new Parameter("p"));
        assertEquals("property := #synth EF(loc[a] = l1);\nprojectresult(p);\n", p.toIMITATOR());
        assertEquals(Backend.IMITATOR, model.preferredBackend(p));
        assertEquals(Backend.UPPAAL, model.preferredBackend());
    }

    // ------------------------------------------------------------------
    // Agreement with IMITATOR
    // ------------------------------------------------------------------

    /** Train/gate of {@link Main} with the parameters replaced by p_approach = a/2 and p_lower = l/2. */
    static ImitatorModel trainGate(long a, long l) {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Clock xt = model.addClock("xt");
        Clock xg = model.addClock("xg");
        Rational nbCross = model.addRational("nb_cross");
        Action approach = action("approach"), enter = action("enter"), leave = action("leave"),
               lowerDone = action("lower_done");

        PTA train = model.automaton("train");
        Location far = train.loc("far");
        Location near = train.loc("near").invariant(xt.le(c(2 * a, 2)));
        Location crossing = train.loc("crossing").invariant(xt.le(c(2 * a + 10, 2)));
        train.initial(far);
        far.on(approach).when(nbCross.lt(3)).doing(xt.reset()).goTo(near);
        near.on(enter).when(xt.ge(c(a, 2))).goTo(crossing);
        crossing.on(leave).doing(nbCross.assign(nbCross.plus(1))).goTo(far);

        PTA gate = model.automaton("gate");
        Location up = gate.loc("up");
        Location lowering = gate.loc("lowering").invariant(xg.le(c(l, 2)));
        Location down = gate.loc("down");
        gate.initial(up);
        up.on(approach).doing(xg.reset()).goTo(lowering);
        lowering.on(lowerDone).goTo(down);
        down.on(leave).goTo(up);

        PTA observer = model.automaton("observer");
        Location wait = observer.loc("obs_wait");
        Location ok = observer.loc("obs_ok");
        Location bad = observer.loc("obs_bad");
        observer.initial(wait);
        wait.on(lowerDone).goTo(ok);
        wait.on(enter).goTo(bad);
        ok.on(enter).goTo(wait);
        return model;
    }

    // IMITATOR synthesises p_approach > p_lower for the parametric version: safe iff a > l
    @ParameterizedTest
    @CsvSource({"8,6,true", "4,3,true", "3,4,false", "5,5,false", "7,1,true"})
    void trainGateSafetyAgreesWithImitator(long a, long l, boolean safe) throws Exception {
        ImitatorModel model = trainGate(a, l);
        PTA observer = model.getPTA("observer");
        Property p = Property.AGnot(StatePredicate.loc(observer, observer.getLocation("obs_bad")));
        assertEquals(Backend.UPPAAL, model.preferredBackend(p));

        UppaalSpec spec = model.toUppaalSpec(UppaalOptions.defaults(), p);
        assertEquals((a % 2 == 0 && l % 2 == 0) ? 1 : 2, spec.timeScale());
        assertEquals(List.of(), spec.warnings());
        assertEquals("A[] !(observer.obs_bad)\n", spec.queries());

        assertEquals(List.of(safe ? Verdict.SATISFIED : Verdict.NOT_SATISFIED), UppaalCheck.verify(spec.xml()));
        assertEquals(safe ? "True" : "False", ImitatorCheck.result(model.toIMITATOR(), p.toIMITATOR()));
    }

    // Synchronised updates: IMITATOR runs the top-most automaton first (r := 2r, then r := r + 1)
    @Test
    void synchronisedUpdatesKeepImitatorOrder() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Rational r = model.addRational("r");
        Action a = action("a");
        PTA first = model.automaton("first");
        Location l0 = first.loc("l0"), l1 = first.loc("l1"), l2 = first.loc("l2");
        first.initial(l0);
        l0.silent().doing(r.assign(1)).goTo(l1);
        l1.on(a).doing(r.assign(r.times(2))).goTo(l2);
        PTA second = model.automaton("second");
        Location m0 = second.loc("m0"), m1 = second.loc("m1");
        second.initial(m0);
        m0.on(a).doing(r.assign(r.plus(1))).goTo(m1);

        Property three = Property.EF(StatePredicate.loc(second, m1).and(StatePredicate.of(r.eq(3))));
        Property four = Property.EF(StatePredicate.loc(second, m1).and(StatePredicate.of(r.eq(4))));
        String xml = model.toUppaal(UppaalOptions.defaults().withLayout(false), three, four);
        assertTrue(xml.contains("<label kind=\"synchronisation\">a!</label>"), xml);
        assertTrue(xml.contains("<label kind=\"synchronisation\">a?</label>"), xml);

        assertEquals(List.of(Verdict.SATISFIED, Verdict.NOT_SATISFIED), UppaalCheck.verify(xml));
        assertEquals("True", ImitatorCheck.result(model.toIMITATOR(), three.toIMITATOR()));
        assertEquals("False", ImitatorCheck.result(model.toIMITATOR(), four.toIMITATOR()));
    }

    // ------------------------------------------------------------------
    // Constraints and scaling
    // ------------------------------------------------------------------

    @Test
    void rationalConstantsAreScaledToIntegers() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false).setHeaderTimestamp(false);
        Clock x = model.addClock("x");
        Rational r = model.addRational("r");
        model.addVariable(new Rational("HALF", q(1, 2)));
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0").invariant(x.le(c(5, 3)));
        Location l1 = a.loc("l1");
        a.initial(l0);
        l0.silent().when(x.ge(c(3, 2))).doing(r.assign(x.minus(x).plus(c(1, 4)))).goTo(l1);

        UppaalSpec spec = model.toUppaalSpec(UppaalOptions.defaults().withLayout(false),
                Property.EF(StatePredicate.loc(a, l1).and(StatePredicate.of(r.eq(c(1, 4))))));
        assertEquals(12, spec.timeScale());                    // lcm(3, 2, 2, 4)
        assertTrue(spec.xml().contains("x &lt;= 20"), spec.xml());  // 5/3 * 12
        assertTrue(spec.xml().contains("x &gt;= 18"), spec.xml());  // 3/2 * 12
        assertTrue(spec.xml().contains("r = 3"), spec.xml());       // 1/4 * 12
        assertTrue(spec.xml().contains("const int HALF = 6;"), spec.xml());
        assertEquals("E<> (a.l1) && (r == 3)\n", spec.queries());
        assertEquals(List.of(Verdict.SATISFIED), UppaalCheck.verify(spec.xml()));
    }

    @Test
    void forcedTimeScaleMustMakeConstantsIntegers() {
        ImitatorModel model = new ImitatorModel();
        Clock x = model.addClock("x");
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0");
        a.initial(l0);
        l0.silent().when(x.ge(c(1, 3))).goTo(l0);
        assertEquals(30, model.toUppaalSpec(UppaalOptions.defaults().withTimeScale(30)).timeScale());
        UppaalUnsupportedException e = assertThrows(UppaalUnsupportedException.class,
                () -> model.toUppaalSpec(UppaalOptions.defaults().withTimeScale(2)));
        assertTrue(e.getProblems().get(0).contains("not an integer once scaled"), e.getMessage());
    }

    private static String guardOf(ImitatorModel model, ComplexConstraint guard) {
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0");
        a.initial(l0);
        l0.silent().when(guard).goTo(l0);
        return model.toUppaal(UppaalOptions.defaults().withLayout(false));
    }

    @Test
    void clockConstraintsAreNormalised() {
        ImitatorModel m1 = new ImitatorModel();
        Clock x = m1.addClock("x"), y = m1.addClock("y");
        Rational r = m1.addRational("r");
        Int i = m1.addInt("i");
        String xml = guardOf(m1, and(x.minus(y).gt(2), x.neg().le(-1), x.times(2).le(6), x.ge(r.plus(1)),
                                     y.minus(x).ge(c(1)), i.times(2).lt(5), r.times(q(1, 2)).le(1)));
        assertTrue(xml.contains("x - y &gt; 2"), xml);
        assertTrue(xml.contains("x &gt;= 1"), xml);          // -x <= -1
        assertTrue(xml.contains("x &lt;= 3"), xml);          // 2x <= 6
        assertTrue(xml.contains("x &gt;= r + 1"), xml);
        assertTrue(xml.contains("y - x &gt;= 1"), xml);
        assertTrue(xml.contains("2*i &lt; 5"), xml);         // int: not scaled
        assertTrue(xml.contains("r &lt;= 2"), xml);          // r/2 <= 1
    }

    @Test
    void unsupportedClockConstraintsAreReported() {
        ImitatorModel m1 = new ImitatorModel();
        Clock x = m1.addClock("x"), y = m1.addClock("y");
        PTA a = m1.automaton("a");
        Location l0 = a.loc("l0");
        a.initial(l0);
        l0.silent().when(x.plus(y).le(3)).goTo(l0);
        assertTrue(m1.uppaalProblems().get(0).contains("not of the form x ~ e"), m1.uppaalProblems().toString());

        ImitatorModel m2 = new ImitatorModel();
        Clock z = m2.addClock("z");
        Int i = m2.addInt("i");
        PTA b = m2.automaton("b");
        Location k0 = b.loc("k0");
        b.initial(k0);
        k0.silent().when(z.le(i)).goTo(k0);
        assertTrue(m2.uppaalProblems().stream().anyMatch(p -> p.contains("compares a clock with an int")), m2.uppaalProblems().toString());
        assertEquals(Backend.IMITATOR, m2.preferredBackend());
    }

    @Test
    void invariantsAreUpperBoundsOnly() throws Exception {
        ImitatorModel m1 = new ImitatorModel().setHeaderTimestamp(false);
        Clock x = m1.addClock("x"), y = m1.addClock("y");
        PTA a = m1.automaton("a");
        Location l0 = a.loc("l0").invariant(and(x.minus(y).ge(-3), x.le(4))).stop(y);
        a.initial(l0);
        l0.silent().when(x.ge(4)).doing(x.reset()).goTo(l0);
        String xml = m1.toUppaal(UppaalOptions.defaults().withLayout(false), Property.EF(StatePredicate.loc(a, l0)));
        assertTrue(xml.contains("y - x &lt;= 3 &amp;&amp; x &lt;= 4 &amp;&amp; y' == 0"), xml);
        assertEquals(1, UppaalCheck.verify(xml).size());

        ImitatorModel m2 = new ImitatorModel();
        Clock z = m2.addClock("z");
        PTA b = m2.automaton("b");
        b.initial(b.loc("k0").invariant(z.ge(2)));
        assertTrue(m2.uppaalProblems().get(0).contains("bounds a clock from below"), m2.uppaalProblems().toString());
    }

    @Test
    void ratesOtherThanZeroOrOneAreReported() {
        ImitatorModel model = new ImitatorModel();
        Clock x = model.addClock("x");
        PTA a = model.automaton("a");
        a.initial(a.loc("l0").flow(x, c(2)));
        assertTrue(model.uppaalProblems().get(0).contains("rates 0 and 1 only"), model.uppaalProblems().toString());
    }

    @Test
    void disjunctiveGuardsOverClocksAreSplit() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Clock x = model.addClock("x");
        Int i = model.addInt("i");
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0").invariant(x.le(5));
        Location l1 = a.loc("l1");
        Location l2 = a.loc("l2");
        a.initial(l0);
        l0.silent().when(or(x.lt(1), x.gt(2))).goTo(l1);   // clocks: split
        l0.silent().when(or(i.eq(0), i.eq(2))).goTo(l2);   // discrete: kept
        String xml = model.toUppaal(UppaalOptions.defaults().withLayout(false),
                Property.EF(StatePredicate.loc(a, l1)), Property.EF(StatePredicate.loc(a, l2)));
        assertTrue(xml.contains("<label kind=\"guard\">x &lt; 1</label>"), xml);
        assertTrue(xml.contains("<label kind=\"guard\">x &gt; 2</label>"), xml);
        assertTrue(xml.contains("<label kind=\"guard\">i == 0 || i == 2</label>"), xml);
        assertEquals(List.of(Verdict.SATISFIED, Verdict.SATISFIED), UppaalCheck.verify(xml));
    }

    // ------------------------------------------------------------------
    // Synchronisation
    // ------------------------------------------------------------------

    @Test
    void actionsAreRoutedLikeImitatorStrongBroadcast() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Action solo = action("solo"), pair = action("pair"), dead = action("dead");
        PTA p = model.automaton("p");
        Location p0 = p.loc("p0"), p1 = p.loc("p1"), p2 = p.loc("p2");
        p.initial(p0);
        p0.on(solo).goTo(p1);                     // only p declares solo: internal edge
        p1.on(pair).goTo(p2);                     // p and q: binary channel
        p2.on(dead).goTo(p0);                     // q declares dead but never uses it: IMITATOR removes it
        PTA q = model.automaton("q");
        Location q0 = q.loc("q0"), q1 = q.loc("q1");
        q.initial(q0);
        q0.on(pair).goTo(q1);
        q.actions(dead);

        String xml = model.toUppaal(UppaalOptions.defaults().withLayout(false),
                Property.EF(StatePredicate.loc(q, q1)), Property.EF(StatePredicate.loc(p, p2).and(StatePredicate.loc(q, q0))));
        assertTrue(xml.contains("chan pair;"), xml);
        assertFalse(xml.contains("chan solo") || xml.contains("chan dead"), xml);
        assertTrue(xml.contains("Note: action dead is declared but unused by [q]"), xml);
        assertFalse(xml.contains("<source ref=\"id2\"/>"), "the dead edge from p2 must be dropped:\n" + xml);
        assertEquals(List.of(Verdict.SATISFIED, Verdict.NOT_SATISFIED), UppaalCheck.verify(xml));
    }

    @Test
    void undeclaredActionIsReported() {
        ImitatorModel model = new ImitatorModel();
        PTA p = model.automaton("p").setAutoDeclareActions(false);
        Location p0 = p.loc("p0");
        p.initial(p0);
        p0.on(action("e")).goTo(p0);
        assertTrue(model.uppaalProblems().get(0).contains("without declaring it"), model.uppaalProblems().toString());
    }

    @Test
    void threeWaySynchronisationWarnsUnlessReceiversAlwaysListen() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Clock x = model.addClock("x");
        Action a = action("a");
        PTA s = model.automaton("s");
        Location s0 = s.loc("s0"), s1 = s.loc("s1");
        s.initial(s0);
        s0.on(a).goTo(s1);
        PTA r1 = model.automaton("r1");             // always listens
        Location r10 = r1.loc("r10");
        r1.initial(r10);
        r10.on(a).goTo(r10);
        PTA r2 = model.automaton("r2");             // listens only while x <= 1
        Location r20 = r2.loc("r20");
        r2.initial(r20);
        r20.on(a).when(x.le(1)).goTo(r20);

        UppaalSpec spec = model.toUppaalSpec(UppaalOptions.defaults(), Property.EF(StatePredicate.loc(s, s1)));
        assertTrue(spec.xml().contains("broadcast chan a;"), spec.xml());
        assertEquals(1, spec.warnings().size(), spec.warnings().toString());
        assertTrue(spec.warnings().get(0).contains("r2 cannot"), spec.warnings().toString());
        assertEquals(List.of(Verdict.SATISFIED), UppaalCheck.verify(spec.xml()));
    }

    // ------------------------------------------------------------------
    // Names, queues, conditional updates, properties
    // ------------------------------------------------------------------

    @Test
    void clashingAndReservedNamesAreRenamed() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Rational r = model.addRational("r");
        Rational sel = model.addRational("select");          // UPPAAL keyword
        Action act = action("r");                             // same name as a variable
        PTA sys = model.automaton("system");                  // UPPAAL keyword
        Location lr = sys.loc("r");                           // a location named r would shadow the variable r
        Location l1 = sys.loc("l1");
        sys.initial(lr);
        lr.on(act).when(r.eq(0)).doing(sel.assign(1)).goTo(l1);
        PTA other = model.automaton("log");                   // built-in function (CherLog's log automaton)
        Location o0 = other.loc("o0"), o1 = other.loc("o1");
        other.initial(o0);
        o0.on(act).goTo(o1);

        Property p = Property.EF(StatePredicate.loc(other, o1).and(StatePredicate.loc(sys, l1)));
        String xml = model.toUppaal(UppaalOptions.defaults().withLayout(false), p);
        assertTrue(xml.contains("select_1 = 1"), xml);
        assertTrue(xml.contains("<name>system_1</name>"), xml);
        assertTrue(xml.contains("<name>log_1</name>"), xml);
        assertTrue(xml.contains("chan r_1;"), xml);
        assertTrue(xml.contains(">r_2</name>"), xml);           // the location
        assertTrue(xml.contains("E&lt;&gt; (log_1.o1) &amp;&amp; (system_1.l1)"), xml);
        assertEquals(List.of(Verdict.SATISFIED), UppaalCheck.verify(xml));
        List<String> renamed = model.toUppaalSpec(UppaalOptions.defaults().withLayout(false), p).names().renamed();
        assertTrue(renamed.contains("automaton log -> log_1"), renamed.toString());
        assertTrue(renamed.contains("variable select -> select_1"), renamed.toString());
        assertTrue(renamed.contains("location system.r -> r_2"), renamed.toString());
    }

    @Test
    void queuesAndConditionalUpdates() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Clock x = model.addClock("x");
        Rational t = model.addRational("t");
        Int n = model.addInt("n");
        ImiBoolean seen = model.addImiBoolean("seen");
        ImiQueue ts = model.addQueue("ts", ImiQueue.ElementType.RATIONAL);
        ImiQueue best = model.addQueue("best", ImiQueue.ElementType.RATIONAL);
        ImiQueue flags = model.addQueue("flags", ImiQueue.ElementType.BOOL);
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0").invariant(x.le(c(1, 2)));
        Location l1 = a.loc("l1");
        Location l2 = a.loc("l2");
        a.initial(l0);
        l0.silent().when(x.eq(c(1, 2))).doing(t.assign(c(1, 2)), ts.push(t), ts.push(c(7, 2)),
                flags.push(true), best.assign(ts), ts.clear(), n.assign(n.plus(1)).when(seen.isFalse()),
                seen.assign(true)).goTo(l1);
        l1.silent().when(and(n.eq(1), seen.isTrue())).goTo(l2);

        UppaalSpec spec = model.toUppaalSpec(UppaalOptions.defaults().withQueueCapacity(4).withLayout(false),
                Property.EF(StatePredicate.loc(a, l2).and(StatePredicate.of(t.eq(c(1, 2))))));
        String xml = spec.xml();
        assertEquals(2, spec.timeScale());
        assertTrue(xml.contains("const int IMI__QUEUE_CAP = 4;"), xml);
        assertTrue(xml.contains("imi__rqueue_t ts;"), xml);
        assertTrue(xml.contains("imi__bqueue_t flags;"), xml);
        assertTrue(xml.contains("imi__rqueue_push(ts, t), imi__rqueue_push(ts, 7), imi__bqueue_push(flags, true), "
                + "best = ts, imi__rqueue_clear(ts), imi__cond_0(), seen = true"), xml);
        assertTrue(xml.contains("void imi__cond_0() {\n    if (!seen) {\n        n = n + 1;"), xml);
        assertEquals(List.of(Verdict.SATISFIED), UppaalCheck.verify(xml));
    }

    @Test
    void propertiesInBothSyntaxes() {
        ImitatorModel model = simpleModel();
        PTA a = model.getPTA("a");
        Location l1 = a.getLocation("l1");
        a.getLocation("l1").accepting();
        Rational r = (Rational) model.getVariables().stream().filter(v -> v.getName().equals("r")).findFirst().orElseThrow();
        StatePredicate inL1 = StatePredicate.loc(a, l1);

        assertEquals("property := #synth EF(loc[a] = l1);\n", Property.EF(inL1).toIMITATOR());
        assertEquals("property := #witness AGnot(loc[a] = l1);\n", Property.AGnot(inL1).witness().toIMITATOR());
        assertEquals("property := #synth DeadlockFree;\n", Property.deadlockFree().toIMITATOR());
        assertEquals("property := #synth AF((accepting) | (not (r >= 3/2)));\n",
                Property.AF(StatePredicate.accepting().or(StatePredicate.of(r.ge(c(3, 2))).not())).toIMITATOR());

        String q = model.toUppaalQueries(Property.EF(inL1), Property.AG(inL1.not()), Property.AGnot(inL1),
                Property.AF(StatePredicate.accepting().or(StatePredicate.of(r.ge(c(3, 2))).not())), Property.deadlockFree());
        assertEquals("E<> a.l1\nA[] !(a.l1)\nA[] !(a.l1)\nA<> ((a.l1)) || (!(r >= 3))\nA[] !deadlock\n", q);

        assertThrows(IllegalArgumentException.class, () -> StatePredicate.of(new Clock("x").ge(1)));
        assertThrows(IllegalArgumentException.class, () -> StatePredicate.loc(new PTA("b"), l1));
    }

    @Test
    void imitatorOutputIsUnchangedByQueueRefactoring() {
        ImiQueue q = new ImiQueue("q", ImiQueue.ElementType.RATIONAL);
        ImiQueue o = new ImiQueue("o", ImiQueue.ElementType.RATIONAL);
        Rational t = new Rational("t");
        assertEquals("queue_push(t, q)", q.push(t).toIMITATOR());
        assertEquals("q <- queue()", q.clear().toIMITATOR());
        assertEquals("q <- o", q.assign(o).toIMITATOR());
    }

    // a -> l0 --(r := 3/2)--> l1
    private static ImitatorModel simpleModel() {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Rational r = model.addRational("r");
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0"), l1 = a.loc("l1");
        a.initial(l0);
        l0.silent().doing(r.assign(c(3, 2))).goTo(l1);
        return model;
    }
}
