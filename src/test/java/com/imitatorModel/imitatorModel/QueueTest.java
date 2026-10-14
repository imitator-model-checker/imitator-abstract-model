package com.imitatorModel.imitatorModel;

import static com.imitatorModel.imitatorModel.Imi.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.imitatorModel.imitatorModel.ImiQueue.ElementType;

/** Queues: printing, validation, and recording of timestamps along an IMITATOR run. */
class QueueTest {

    @Test
    void declarationAndInit() {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        ImiQueue ts = model.addQueue("ts", ElementType.RATIONAL);
        model.addQueue("ids", ElementType.INT);
        model.addImiBoolean("b");
        PTA a = model.automaton("a");
        a.initial(a.loc("l0"));
        String imi = model.toIMITATOR();
        assertTrue(imi.contains("\tts: rational queue;"), imi);
        assertTrue(imi.contains("\tids: int queue;"), imi);
        assertTrue(imi.contains("\t ts <- queue(),\n"), imi);
        assertTrue(imi.contains("\t b <- False,\n"), imi);
        assertSame(ts, model.addQueue("ts", ElementType.RATIONAL));
        assertThrows(IllegalArgumentException.class, () -> model.addQueue("ts", ElementType.INT));
        assertThrows(IllegalArgumentException.class, () -> model.addRational("ts"));
    }

    @Test
    void updatesPrinting() {
        ImiQueue ts = new ImiQueue("ts", ElementType.RATIONAL);
        ImiQueue best = new ImiQueue("best", ElementType.RATIONAL);
        ImiQueue flags = new ImiQueue("flags", ElementType.BOOL);
        Rational now = new Rational("now");
        Int n = new Int("n");
        ImiBoolean b = new ImiBoolean("b");

        assertEquals("queue_push(now, ts)", ts.push(now).toIMITATOR());
        assertEquals("queue_push(now + 1/2, ts)", ts.push(now.plus(q(1, 2))).toIMITATOR());
        assertEquals("queue_push(3, ts)", ts.push(3).toIMITATOR());
        assertEquals("ts <- queue()", ts.clear().toIMITATOR());
        assertEquals("best <- ts", best.assign(ts).toIMITATOR());
        assertEquals("queue_push(True, flags)", flags.push(true).toIMITATOR());
        assertEquals("queue_push(not (b), flags)", flags.push(b.negate()).toIMITATOR());
        assertEquals("if n > 0 then queue_push(now, ts) end ", ts.push(now).when(n.gt(0)).toIMITATOR());
        assertEquals(" do {n <- n + 1; queue_push(now, ts)}", updates(n.assign(n.plus(1)), ts.push(now)).toIMITATOR());
        assertSame(ts, ts.push(now).getVariable());
    }

    @Test
    void invalidElementsAreRejected() {
        ImiQueue ts = new ImiQueue("ts", ElementType.RATIONAL);
        ImiQueue ids = new ImiQueue("ids", ElementType.INT);
        ImiQueue flags = new ImiQueue("flags", ElementType.BOOL);
        Clock t = new Clock("t");
        Parameter p = new Parameter("p");
        Int n = new Int("n");
        Rational r = new Rational("r");

        assertThrows(IllegalArgumentException.class, () -> ts.push(t));            // clock: IMITATOR error
        assertThrows(IllegalArgumentException.class, () -> ts.push(p));            // parameter: IMITATOR error
        assertThrows(IllegalArgumentException.class, () -> ts.push(n));            // no int -> rational conversion
        assertThrows(IllegalArgumentException.class, () -> ids.push(r));
        assertThrows(IllegalArgumentException.class, () -> ids.push(q(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> flags.push(r));
        assertThrows(IllegalArgumentException.class, () -> ts.push(true));
        assertThrows(IllegalArgumentException.class, () -> ts.assign(ids));
        assertThrows(IllegalArgumentException.class, () -> r.assign(c(0)).when(t.gt(0)));
        assertThrows(IllegalArgumentException.class, () -> new Update(ts, c(0)));  // queue <- number
        assertDoesNotThrow(() -> ids.push(n.plus(1)));
    }

    @Test
    void conditionalUpdateFollowedByAnotherUpdate() throws Exception {
        Rational now = new Rational("now");
        Int f = new Int("f");
        Int best = new Int("best");
        Rational bestTime = new Rational("bestTime");
        // no ";" after "end": IMITATOR rejects it
        assertEquals(" do {if f > best then bestTime <- now end if f > best then best <- f end }",
                updates(bestTime.assign(now).when(f.gt(best)), best.assign(f).when(f.gt(best))).toIMITATOR());
        assertEquals(" do {if f > best then best <- f end best <- 0}",
                updates(best.assign(f).when(f.gt(best)), best.assign(0)).toIMITATOR());

        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        model.addVariables(java.util.List.of(now, f, best, bestTime));
        PTA a = model.automaton("a");
        Location l0 = a.loc("l0");
        a.initial(l0);
        l0.silent().doing(bestTime.assign(now).when(f.gt(best)), best.assign(f).when(f.gt(best)), now.assign(1)).goTo(l0);
        ImitatorCheck.assertSyntaxOk(model.toIMITATOR());
    }

    /**
     * The log stores the time of each sample in a discrete variable {@code now} (clocks cannot be pushed);
     * the monitor pushes it for every sample with f > V. One accepting state per parameter region, each with
     * its own queue.
     */
    private static ImitatorModel recordingModel() {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Clock t = model.addClock("t");
        Rational f = model.addRational("f");
        Rational now = model.addRational("now");
        ImiQueue ts = model.addQueue("ts", ElementType.RATIONAL);
        ImiQueue best = model.addQueue("best", ElementType.RATIONAL);
        Parameter v = model.addParameter("V");
        v.setConstraints(v.atom(Operator.GE, c(0)));
        Action sample = action("sample"), logEnd = action("logEnd");

        PTA log = model.automaton("log");
        Location l0 = log.loc("l0").invariant(t.le(1));
        Location l0p = log.loc("l0p").urgent();
        Location l1 = log.loc("l1").invariant(t.le(c(7, 2)));
        Location l1p = log.loc("l1p").urgent();
        Location l2 = log.loc("l2").invariant(t.le(20));
        Location l2p = log.loc("l2p").urgent();
        Location l3 = log.loc("l3");
        log.initial(l0);
        l0.silent().when(t.eq(1)).doing(f.assign(5), now.assign(1)).goTo(l0p);
        l0p.on(sample).goTo(l1);
        l1.silent().when(t.eq(c(7, 2))).doing(f.assign(7), now.assign(q(7, 2))).goTo(l1p);
        l1p.on(sample).goTo(l2);
        l2.silent().when(t.eq(20)).goTo(l2p);
        l2p.on(logEnd).goTo(l3);

        PTA monitor = model.automaton("monitor");
        Location m1 = monitor.loc("m1");
        Location done = monitor.loc("l_end").accepting();
        monitor.initial(m1);
        m1.on(sample).when(f.gt(v)).doing(ts.push(now), best.assign(ts).when(f.gt(6))).goTo(m1);
        m1.on(sample).when(f.le(v)).goTo(m1);
        m1.on(logEnd).goTo(done);
        return model;
    }

    @Test
    void recordingModelIsAcceptedByImitator() throws Exception {
        ImitatorCheck.assertSyntaxOk(recordingModel().toIMITATOR());
    }

    @Test
    void acceptingStatesShowTheRecordedQueues() throws Exception {
        String bin = ImitatorCheck.binary();
        assumeTrue(bin != null, "IMITATOR binary not found (set IMITATOR_BIN)");
        Path dir = Files.createTempDirectory("imi-queue");
        Files.writeString(dir.resolve("m.imi"), recordingModel().toIMITATOR());
        Files.writeString(dir.resolve("m.imiprop"), "property := #synth EF(loc[monitor] = l_end);");
        Process p = new ProcessBuilder(bin, "m.imi", "m.imiprop", "-states-description-accepting", "-no-var-autoremove")
                .directory(dir.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        assertFalse(out.contains("ERROR"), out);
        String states = Files.readString(dir.resolve("m-statespace.states"));
        // V < 5: both samples recorded, in insertion order; best copied at 7/2 (after the push)
        assertTrue(states.contains("best = queue([1, 7/2]), ts = queue([1, 7/2])"), states);
        // 5 <= V < 7: only the sample at 7/2
        assertTrue(states.contains("best = queue([7/2]), ts = queue([7/2])"), states);
        // V >= 7: nothing recorded
        assertTrue(states.contains("best = queue(), ts = queue()"), states);
    }
}
