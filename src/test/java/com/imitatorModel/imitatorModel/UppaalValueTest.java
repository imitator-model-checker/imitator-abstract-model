package com.imitatorModel.imitatorModel;

import static com.imitatorModel.imitatorModel.Imi.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.imitatorModel.bigFraction.BigFraction;
import com.imitatorModel.imitatorModel.UppaalResult.Value;
import com.imitatorModel.imitatorModel.UppaalResult.Verdict;

/**
 * Reading values back from UPPAAL ({@link Property#value}, {@link UppaalResult}). Expected values derived by hand;
 * verifyta ($VERIFYTA_BIN or PATH) and IMITATOR ($IMITATOR_BIN or PATH) are used when available.
 */
class UppaalValueTest {

    /**
     * A CherLog-like log: events {@code e} at 1, 5/2 and 7/2, then {@code logEnd} at 6; every event sets
     * {@code sampleTime}. The monitor counts the events, copies {@code sampleTime} into {@code lastTime}
     * (the log updates first, so it sees the new value), and goes to {@code l_computed} on {@code logEnd}.
     */
    static final class CountModel {
        final ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        final Clock t = model.addClock("t");
        final Clock y = model.addClock("y");
        final Clock z = model.addClock("z");
        final Rational sampleTime = model.addRational("sampleTime");
        final Rational lastTime = model.addRational("lastTime");
        final Int count = model.addInt("count");
        final ImiBoolean seen = model.addImiBoolean("seen");
        final PTA log = model.automaton("log");
        final PTA monitor = model.automaton("monitor");
        final Location m0, lComputed, never;

        CountModel(boolean nondeterministicMonitor) {
            Action e = action("e"), logEnd = action("logEnd");
            BigFraction[] times = {q(1), q(5, 2), q(7, 2)};
            Location prev = log.loc("s0").invariant(t.le(c(times[0])));
            log.initial(prev);
            for (int i = 0; i < times.length; i++) {
                BigFraction next = i + 1 < times.length ? times[i + 1] : q(6);
                Location l = log.loc("s" + (i + 1)).invariant(t.le(c(next)));
                prev.on(e).when(t.eq(c(times[i]))).doing(sampleTime.assign(times[i])).goTo(l);
                prev = l;
            }
            prev.on(logEnd).when(t.eq(6)).goTo(log.loc("s_last"));

            m0 = monitor.loc("m0");
            lComputed = monitor.loc("l_computed").stop(y);
            never = monitor.loc("never");
            monitor.initial(m0);
            m0.on(e).doing(count.assign(count.plus(1)), lastTime.assign(sampleTime), seen.assign(true)).goTo(m0);
            if (nondeterministicMonitor) {
                m0.on(e).doing(count.assign(count.plus(2)), lastTime.assign(sampleTime), seen.assign(true)).goTo(m0);
            }
            m0.on(logEnd).goTo(lComputed);
            monitor.actions(e);   // "never" is a dead end: only declared, never entered
        }
    }

    @Test
    void valuePropertyPrintsInBothSyntaxes() {
        CountModel m = new CountModel(false);
        Property v = Property.value(StatePredicate.loc(m.monitor, m.lComputed), m.count, m.lastTime);
        assertEquals("property := #synth EF(loc[monitor] = l_computed);\n", v.toIMITATOR());
        assertEquals(List.of(m.count, m.lastTime), v.getObserved());

        Property safety = Property.AGnot(StatePredicate.loc(m.monitor, m.never));
        UppaalSpec spec = m.model.toUppaalSpec(UppaalOptions.defaults(), safety, v);
        assertEquals(2, spec.timeScale());
        assertEquals("""
                A[] !(monitor.never)
                E<> monitor.l_computed
                sup{monitor.l_computed}: count, lastTime
                inf{monitor.l_computed}: count, lastTime
                """, spec.queries());
        assertEquals(List.of(0), spec.queryIndices(safety));
        assertEquals(List.of(1, 2, 3), spec.queryIndices(v));
        assertEquals(List.of(), spec.warnings());
    }

    @Test
    void deterministicMonitorGivesSingleValues() throws Exception {
        String verifyta = UppaalCheck.binary();
        assumeTrue(verifyta != null, "verifyta not found");
        CountModel m = new CountModel(false);
        Property v = Property.value(StatePredicate.loc(m.monitor, m.lComputed), m.count, m.lastTime, m.seen);
        UppaalSpec spec = m.model.toUppaalSpec(UppaalOptions.defaults(), v);
        UppaalResult r = UppaalResult.run(verifyta, spec);

        assertTrue(r.isSatisfied(v));
        assertEquals(BigFraction.of(3), r.value(v, m.count).orElseThrow().single());
        assertEquals(q(7, 2), r.value(v, m.lastTime).orElseThrow().single());   // 7 in UPPAAL, time scale 2
        assertEquals(BigFraction.ONE, r.value(v, m.seen).orElseThrow().single());
        assertEquals("{count=3, lastTime=7/2, seen=1}", r.values(v).toString().replace(" (over-approximation)", ""));

        // IMITATOR agrees: the target with these values is reachable, and with another lastTime it is not
        StatePredicate target = StatePredicate.loc(m.monitor, m.lComputed);
        assertEquals("True", ImitatorCheck.result(m.model.toIMITATOR(),
                Property.EF(target.and(StatePredicate.of(and(m.count.eq(3), m.lastTime.eq(c(7, 2)))))).toIMITATOR()));
        assertEquals("False", ImitatorCheck.result(m.model.toIMITATOR(),
                Property.EF(target.and(StatePredicate.of(m.lastTime.eq(c(5, 2))))).toIMITATOR()));
    }

    @Test
    void nondeterministicMonitorGivesRange() throws Exception {
        String verifyta = UppaalCheck.binary();
        assumeTrue(verifyta != null, "verifyta not found");
        CountModel m = new CountModel(true);   // each event adds 1 or 2: count in [3, 6]
        Property v = Property.value(StatePredicate.loc(m.monitor, m.lComputed), m.count);
        UppaalResult r = UppaalResult.run(verifyta, m.model.toUppaalSpec(UppaalOptions.defaults(), v));
        Value count = r.value(v, m.count).orElseThrow();
        assertFalse(count.isSingle());
        assertEquals(BigFraction.of(3), count.min());
        assertEquals(BigFraction.of(6), count.max());
        assertThrows(IllegalStateException.class, count::single);
    }

    @Test
    void unreachableTargetHasNoValue() throws Exception {
        String verifyta = UppaalCheck.binary();
        assumeTrue(verifyta != null, "verifyta not found");
        CountModel m = new CountModel(false);
        Property v = Property.value(StatePredicate.loc(m.monitor, m.never), m.count);
        UppaalResult r = UppaalResult.run(verifyta, m.model.toUppaalSpec(UppaalOptions.defaults(), v));
        assertEquals(Verdict.NOT_SATISFIED, r.verdict(v));
        assertFalse(r.isSatisfied(v));
        assertEquals(Optional.empty(), r.value(v, m.count));
        assertEquals(Map.of(), r.values(v));
    }

    @Test
    void stoppedClockIsReadRunningClockIsUnbounded() throws Exception {
        CountModel m = new CountModel(false);
        Property v = Property.value(StatePredicate.loc(m.monitor, m.lComputed), m.y, m.z);
        UppaalSpec spec = m.model.toUppaalSpec(UppaalOptions.defaults(), v);
        assertEquals(1, spec.warnings().size(), spec.warnings().toString());
        assertTrue(spec.warnings().get(0).startsWith("clock z is not stopped"), spec.warnings().toString());

        String verifyta = UppaalCheck.binary();
        assumeTrue(verifyta != null, "verifyta not found");
        UppaalResult r = UppaalResult.run(verifyta, spec);
        Value y = r.value(v, m.y).orElseThrow();
        assertEquals(BigFraction.of(6), y.single());   // stopped on entering l_computed at t = 6
        assertFalse(y.exact());                         // stopwatch: verifyta answers MAY
        assertFalse(r.isExact(v));
        Value z = r.value(v, m.z).orElseThrow();
        assertEquals(BigFraction.of(6), z.min());
        assertTrue(z.max().isInfinite() && z.maxStrict());
    }

    @Test
    void parsesCannedVerifytaOutput() {
        CountModel m = new CountModel(false);
        Property v = Property.value(StatePredicate.loc(m.monitor, m.lComputed), m.count, m.lastTime, m.z);
        Property w = Property.value(StatePredicate.loc(m.monitor, m.never), m.count);
        UppaalSpec spec = m.model.toUppaalSpec(UppaalOptions.defaults(), v, w);
        String out = """

                Verifying formula 1 at /nta/queries/query[1]/formula
                 -- Formula: E<> monitor.l_computed
                 -- Formula is satisfied.

                Verifying formula 2 at /nta/queries/query[2]/formula
                 -- Formula: sup{monitor.l_computed}: count, lastTime, z
                 -- Formula is satisfied.
                sup: 3, 7, ∞:
                count <= 3
                lastTime <= 7
                z < ∞

                Verifying formula 3 at /nta/queries/query[3]/formula
                 -- Formula: inf{monitor.l_computed}: count, lastTime, z
                 -- Formula is satisfied.
                inf: 3, 5, 12:
                count >= 3
                lastTime >= 5
                z > 12

                Verifying formula 4 at /nta/queries/query[4]/formula
                 -- Formula: E<> monitor.never
                 -- Formula MAY be satisfied.

                Verifying formula 5 at /nta/queries/query[5]/formula
                 -- Formula: sup{monitor.never}: count
                 -- Formula MAY be satisfied.
                sup: -100000000:
                count <= -100000000

                Verifying formula 6 at /nta/queries/query[6]/formula
                 -- Formula: inf{monitor.never}: count
                 -- Formula MAY be satisfied.
                inf: 100000000:
                count >= 100000000
                """;
        UppaalResult r = UppaalResult.parse(spec, out);
        assertEquals("3", r.value(v, m.count).orElseThrow().toString());
        Value last = r.value(v, m.lastTime).orElseThrow();
        assertEquals(q(5, 2), last.min());
        assertEquals(q(7, 2), last.max());
        assertEquals("[5/2, 7/2]", last.toString());
        Value z = r.value(v, m.z).orElseThrow();
        assertEquals(BigFraction.of(6), z.min());
        assertTrue(z.minStrict() && z.max().isInfinite());
        // reachable only in the over-approximation, and the reversed range says the set is empty
        assertTrue(r.isSatisfied(w));
        assertFalse(r.isExact(w));
        assertEquals(Optional.empty(), r.value(w, m.count));

        assertThrows(IllegalStateException.class, () -> UppaalResult.parse(spec, "Verifying formula 1 at x\n -- Formula is satisfied.\n"));
        assertThrows(IllegalArgumentException.class, () -> r.value(w, m.lastTime));
    }

    @Test
    void namesAndWitnessTrace() throws Exception {
        CountModel m = new CountModel(false);
        Property v = Property.value(StatePredicate.loc(m.monitor, m.lComputed), m.count);
        UppaalSpec spec = m.model.toUppaalSpec(UppaalOptions.defaults(), v);
        assertEquals("log_1", spec.names().automata().get("log"));          // "log" is reserved in UPPAAL
        assertEquals("monitor", spec.names().automata().get("monitor"));
        assertEquals("l_computed", spec.names().locations().get("monitor").get("l_computed"));
        assertEquals("sampleTime", spec.names().variables().get("sampleTime"));

        String verifyta = UppaalCheck.binary();
        assumeTrue(verifyta != null, "verifyta not found");
        java.nio.file.Path xml = java.nio.file.Files.createTempDirectory("trace").resolve("m.xml");
        UppaalResult.run(verifyta, spec, xml);
        String trace = UppaalResult.trace(verifyta, spec, v, xml);
        assertTrue(trace.contains("( log_1.s_last monitor.l_computed )"), trace);
        assertTrue(trace.contains("lastTime=7 count=3 seen=1"), trace);   // UPPAAL units: lastTime = 7/2 * 2
    }

    @Test
    void queuesAreReadFromTheWitnessTrace() throws Exception {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Clock x = model.addClock("x");
        ImiQueue ts = model.addQueue("ts", ImiQueue.ElementType.RATIONAL);
        ImiQueue ns = model.addQueue("ns", ImiQueue.ElementType.INT);
        ImiQueue flags = model.addQueue("flags", ImiQueue.ElementType.BOOL);
        ImiQueue empty = model.addQueue("empty", ImiQueue.ElementType.RATIONAL);
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0").invariant(x.le(c(1, 2)));
        Location l1 = a.loc("l1");
        a.initial(l0);
        l0.silent().when(x.eq(c(1, 2))).doing(ts.push(c(1, 2)), ts.push(c(7, 2)), ns.push(3), flags.push(true),
                flags.push(false)).goTo(l1);
        Property reach = Property.EF(StatePredicate.loc(a, l1));
        UppaalSpec spec = model.toUppaalSpec(UppaalOptions.defaults().withQueueCapacity(4).withLayout(false), reach);
        assertEquals(2, spec.timeScale());

        // a trace as verifyta prints it: UPPAAL units (rationals times 2), array padded with 0 after len elements
        String trace = "State:\n( a.l0 )\nts.len=0 x=0\n\nTransition:\n  a.l0->a.l1 { x == 1, 1, ... }\n\n"
                + "State:\n( a.l1 )\nts.data[0]=1 ts.data[1]=7 ts.data[2]=0 ts.data[3]=0 ts.len=2 ns.data[0]=3 ns.len=1 "
                + "flags.data[0]=1 flags.data[1]=0 flags.len=2 empty.len=0 x=1\n";
        Map<String, List<BigFraction>> q = UppaalTrace.queues(model, spec, trace);
        assertEquals(List.of(BigFraction.of(1, 2), BigFraction.of(7, 2)), q.get("ts"));       // divided by the time scale
        assertEquals(List.of(BigFraction.of(3)), q.get("ns"));                                // ints are not scaled
        assertEquals(List.of(BigFraction.of(1), BigFraction.of(0)), q.get("flags"));          // bool: 1 / 0
        assertEquals(List.of(), q.get("empty"));
        assertEquals(Map.of(), UppaalTrace.queues(model, spec, "Property is not satisfied.\n"));

        // the same against a real verifyta
        String verifyta = UppaalCheck.binary();
        assumeTrue(verifyta != null, "verifyta not found");
        java.nio.file.Path xml = java.nio.file.Files.createTempDirectory("queues").resolve("m.xml");
        UppaalResult.run(verifyta, spec, xml);
        Map<String, List<BigFraction>> real =
                UppaalTrace.queues(model, spec, UppaalResult.trace(verifyta, spec, reach, xml));
        assertEquals(q, real);
    }

    @Test
    void valuesThatUppaalCannotGive() {
        CountModel m = new CountModel(false);
        StatePredicate target = StatePredicate.loc(m.monitor, m.lComputed);
        assertThrows(IllegalArgumentException.class,
                () -> Property.value(target, new ImiQueue("q", ImiQueue.ElementType.RATIONAL)));
        assertThrows(IllegalArgumentException.class, () -> Property.value(target));

        Property p = Property.value(target, new Parameter("p"));
        assertTrue(p.usesParameters());
        assertEquals(Backend.IMITATOR, m.model.preferredBackend(p));
        assertEquals(Backend.UPPAAL, m.model.preferredBackend(Property.value(target, m.count)));
    }
}
