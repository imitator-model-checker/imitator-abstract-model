package com.imitatorModel.imitatorModel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** §3.3: printed output does not depend on time or on Set iteration order. */
class DeterminismTest {

    private static ImitatorModel build(boolean reverse) {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
        Clock t = model.addClock("t");
        model.addRational("r");
        model.addImiBoolean("b");
        PTA pta = model.automaton("monitor");
        Action a = new Action("a"), b = new Action("b"), c = new Action("c");
        if (reverse) {
            pta.actions(c, b, a);
        } else {
            pta.actions(a, b, c);
        }
        Location l1 = pta.loc("l1");
        Location l2 = pta.loc("l2");
        pta.initial(l1);
        l1.addTransition(new Transition(t.le(1), Set.of(a, b, c), null, Set.of(l1, l2)));
        return model;
    }

    @Test
    void sameModelPrintsIdentically() {
        assertEquals(build(false).toIMITATOR(), build(false).toIMITATOR());
        assertEquals(build(false).toIMITATOR(), build(true).toIMITATOR());
    }

    @Test
    void timestampCanBeDisabled() {
        assertFalse(build(false).toIMITATOR().contains("generated ("));
        assertTrue(new ImitatorModel().toIMITATOR().contains("generated ("));
    }

    @Test
    void actionsSortedVariablesInInsertionOrder() {
        String imi = build(true).toIMITATOR();
        assertTrue(imi.contains("actions: a, b, c;"), imi);
        assertTrue(imi.indexOf("t: clock;") < imi.indexOf("r: rational;"));
        assertTrue(imi.indexOf("r: rational;") < imi.indexOf("b: bool;"));
        String[] order = {"sync a goto l1", "sync a goto l2", "sync b goto l1", "sync b goto l2", "sync c goto l1", "sync c goto l2"};
        int last = -1;
        for (String s : order) {
            int i = imi.indexOf(s);
            assertTrue(i > last, s + " out of order in\n" + imi);
            last = i;
        }
    }
}
