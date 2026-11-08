# IMITATOR abstract model

A Java library to build (parametric) timed automata in memory and print them for a model checker. One abstract model, two outputs:

- **IMITATOR**: `.imi` (model) and `.imiprop` (property), with parameters and parameter synthesis;
- **UPPAAL**: `.xml` project with `<queries>` (and a `.q` file), for models **without parameters**.

It is the back end of [monitoring-patterns-imitator](../monitoring-patterns-imitator) (CherLog / HPTRE): log and monitor automata are built with this API, then sent to IMITATOR, or to UPPAAL when no parameter is involved.

Only a reduced subset of the IMITATOR syntax is covered (clocks, rationals, ints, bools, queues, parameters; invariants, guards, updates, `if` updates, stopwatches; EF / AG / AGnot / AF / DeadlockFree properties, `#witness`, `projectresult`). Online mode (`toOnlineIMITATOR`) exists for IMITATOR only.

## Build

Requires Java 21 and Maven.

```bash
mvn install            # compiles, runs the tests, installs javaAbstractImi:javaAbstractImi:1.0-SNAPSHOT into ~/.m2
mvn install -DskipTests
```


## Example

```java
import static com.imitatorModel.imitatorModel.Imi.*;
import com.imitatorModel.imitatorModel.*;

ImitatorModel model = new ImitatorModel().setHeaderTimestamp(false);
Clock x = model.addClock("x");
Action ping = action("ping");

PTA a = model.automaton("a");
Location idle = a.loc("idle").invariant(x.le(3));
Location done = a.loc("done");
a.initial(idle);
idle.on(ping).when(x.ge(1)).doing(x.reset()).goTo(done);

Property reach = Property.EF(StatePredicate.loc(a, done));

model.toIMITATOR();                    // the .imi text
reach.toIMITATOR();                    // "property := #synth EF(loc[a] = done);"
model.preferredBackend(reach);         // Backend.UPPAAL: no parameter
model.toUppaal(reach);                 // UPPAAL .xml, query "E<> a.done" embedded
model.toUppaalQueries(reach);          // the same queries as a .q file
```

[Main.java](src/main/java/com/imitatorModel/Main.java) builds the train/gate model of the IMITATOR manual with parameters, and prints it.

Main classes: `ImitatorModel` (variables, automata, printing), `PTA` / `Location` / `Transition` (fluent `loc.on(action).when(...).doing(...).goTo(...)`), `Imi` (static helpers: `c(...)`, `action(...)`, `and(...)`), `Constraint` / `LinearExpr` / `Update`, `Property` / `StatePredicate`, `BigFraction` (exact rationals).

## Choosing a backend

`model.preferredBackend(properties...)` returns IMITATOR when a parameter is used anywhere (declared or only used in a guard, update or property), or when UPPAAL cannot express the model; UPPAAL otherwise. `model.uppaalProblems(properties...)` lists every reason UPPAAL refuses a model (parameters, `x + y ~ c`, an update that reads a clock, a lower-bound invariant on one clock, online placeholders...), and `toUppaal` throws `UppaalUnsupportedException` with the same list.

## The UPPAAL translation

- **Names**: IMITATOR names that clash with UPPAAL keywords or globals are renamed (the automaton `log` becomes `log_1`); `UppaalSpec.names()` gives the map and `names().renamed()` lists the changes.
- **Time**: UPPAAL compares clocks with integers, so every clock and rational is multiplied by one time scale (`UppaalSpec.timeScale()`); results are divided back.
- **Synchronisation**: an action of two automata becomes a binary channel (exact); of three or more, a broadcast, with a warning when a receiver can block.
- **Queues**: bounded arrays (`UppaalOptions.queueCapacity`); a push on a full queue aborts the verification.
- **Results** (`UppaalResult`): verdicts, `sup`/`inf` of scalar variables (`Property.value(...)`), a `-t1` witness trace, and `UppaalTrace.queues(...)` for the recorded queues.


## Tests

```bash
mvn test
IMITATOR_BIN=/path/to/imitator VERIFYTA_BIN=/path/to/verifyta mvn test
```

Tests that compare the output with a real tool are skipped when the tool is not found (`IMITATOR_BIN`, or `imitator` on the `PATH`; `VERIFYTA_BIN`, or `verifyta` on the `PATH`). Tools used: IMITATOR 3.4 and UPPAAL 5.0.0. `devDoc/uppaal-e2e/` has two drivers that run the whole CherLog test suite through both tools and compare the verdicts and the values (see section 5 of the translation log).

## License

See [LICENSE](LICENSE).
