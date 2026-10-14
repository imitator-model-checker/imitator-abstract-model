/**
 * Example: builds the train/gate model of the IMITATOR manual with the fluent API and prints it.
 * Expected IMITATOR result for "#synth AGnot(loc[observer] = obs_bad)": p_approach > p_lower & p_lower >= 0.
 */

package com.imitatorModel;

import static com.imitatorModel.imitatorModel.Imi.*;

import com.imitatorModel.imitatorModel.*;

public class Main {

    public static ImitatorModel trainGate() {
        ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);

        Clock xt = model.addClock("xt");
        Clock xg = model.addClock("xg");
        Rational nbCross = model.addRational("nb_cross");
        Parameter pApproach = model.addParameter("p_approach");
        Parameter pLower = model.addParameter("p_lower");
        pApproach.setConstraints(pApproach.atom(Operator.GE, c(0)));
        pLower.setConstraints(pLower.atom(Operator.GE, c(0)));

        Action approach = action("approach"), enter = action("enter"), leave = action("leave"),
               lowerDone = action("lower_done");

        PTA train = model.automaton("train");
        Location far = train.loc("far");
        Location near = train.loc("near").invariant(xt.le(pApproach.times(2)));
        Location crossing = train.loc("crossing").invariant(xt.le(pApproach.times(2).plus(5)));
        train.initial(far);
        far.on(approach).when(nbCross.lt(3)).doing(xt.reset()).goTo(near);
        near.on(enter).when(xt.ge(pApproach)).goTo(crossing);
        crossing.on(leave).doing(nbCross.assign(nbCross.plus(1))).goTo(far);

        PTA gate = model.automaton("gate");
        Location up = gate.loc("up");
        Location lowering = gate.loc("lowering").invariant(xg.le(pLower));
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

    public static void main(String[] args) {
        System.out.println(trainGate().toIMITATOR());
    }
}
