# `com.imitatorModel` API: improvements

This document covers the Java API in this folder (`imitatorModel/*`, `bigFraction/*`, `Main.java`) and how its main client, `monitoring-patterns-imitator` (CherLog), uses it.

- **Part A** records the changes already made: steps 1–3 of the migration plan (A.1–A.4), then the migration of every CherLog pattern (A.5) and of the log automaton (A.6) to the fluent API, all done on 2026-10-14.
- **Parts 0–6** are the proposals still open. Completed proposals have been removed.

Items tagged **[verified]** were reproduced, either with `jshell` against `target/classes` or by running IMITATOR 3.4 `-mode checksyntax` on emitted text.

Usage numbers from the client codebase, measured before the migration, show where verbosity costs the most:

| Construct in client code | Occurrences |
|---|---|
| `new Constraint(` | 471 |
| `new Transition(` | 336 |
| `new ConstraintNode(` | 243 (173 of them are `new ConstraintNode(new Constraint(…))`) |
| `new LinearExpr(` | 101 |
| `new Update(` / `new ListUpdates(` | 71 / 68 |
| `new Action(` | 69 |
| `.imiprop` text | built by hand as `String` in every `generateProperty()` |

---

## A. Changes made

### A.1 Step 1: bug fixes (§1 of the original review)

| Bug | Fix | Files |
|---|---|---|
| Bool constant printed `b = true` (rejected by IMITATOR) | New `VariableType.valueToIMITATOR()`, overridden in `ImiBoolean` (`True`/`False`) and `Rational` (rejects infinity). The `var` block uses it. | `VariableType`, `ImiBoolean`, `Rational`, `ImitatorModel` |
| `Constraint.TRUE.negate()` threw; `ConstraintNode(TRUE).negate()` gave `0 < 0 \| 0 > 0` | Both check the truth constant first and return `FALSE`/`TRUE`. Negating a single `EQ` atom with `Constraint.negate()` now throws an explicit message pointing to `ConstraintNode.negate()`. | `Constraint`, `ConstraintNode` |
| Zero coefficients printed ` + y` or an empty string | `LinearExpr` drops zero terms. Printing joins the non-zero parts. | `LinearExpr` |
| Like terms never merged (`x + x` ≠ `2 * x`) | `LinearExpr` stores a normalised, unmodifiable `LinkedHashMap<VariableType, BigFraction>` (merge plus drop zeros). `equals`/`hashCode` compare that map, so equal atoms now share one LogicNG variable in `toDNF()`. | `LinearExpr` |
| `x <= y + inf` reached the output as `inf` | A side whose constant is ±∞ counts as ±∞ whatever its variables (`compareWithInfinity`, which replaces ~70 lines of `if/else` with the same truth table). `LinearExpr.toIMITATOR()` throws if an infinite constant survives (e.g. in an update). | `Constraint`, `LinearExpr`, `BigFraction.isInfinite()` |
| Two `new Location("l1")` both added | `PTA` stores locations in a `LinkedHashMap<String, Location>` keyed by name. `addLocation` throws on a duplicate name, **except** that a waiting placeholder (online mode) is replaced by the new location of the same name. `Location` and `PTA` now override `equals(Object)`/`hashCode` (by name). | `PTA`, `Location` |
| `Location.addTransitions` ignored its argument | Fixed. | `Location` |
| Same-name variables silently dropped | `addVariable` returns the existing variable when name and type match, and throws when the name exists with another type. New `replaceVariable` overwrites unconditionally. `addClock/addParameter/addRational/addInt/addImiBoolean` return the declared variable. | `ImitatorModel` |
| Clock in `if … then` update condition accepted | `Update` rejects clocks **and parameters** in conditions. IMITATOR rejects both **[verified]**. | `Update` |
| `Update(LinearExpr, LinearExpr)` used the first variable silently | First deprecated and checked, then **removed** (with its two conditional variants) once no CherLog code used it (A.5). | `Update` |
| Other `Update` checks | One canonical constructor `Update(ComplexConstraint condition, VariableType target, UpdateTerm term)`; every other overload delegates to it. It rejects a parameter target and a boolean/non-boolean mismatch. Fields are `final`. | `Update` |
| False guards detected by identity (`== Constraint.FALSE`) | `Constraint.isFalse()` (also `isTrue()`). | `Transition`, `Constraint` |
| `PTA.addLocation` added twice | Fixed as part of the map rewrite. | `PTA` |
| NPE when a PTA has no initial location | `IllegalStateException("Automaton X has no initial location")`. | `ImitatorModel` |
| `BigFraction.random(1)` threw | Returns `ONE`. | `BigFraction` |
| `BigFraction.getIMITATORType()` leftover | Removed. | `BigFraction` |
| `Main.java` dead code run at every `compile` | `exec-maven-plugin` binding removed. `Main` now builds the train/gate example with the fluent API (`Main.trainGate()`). | `Main`, `pom.xml` |

**Regression tests** (new, JUnit 5, `src/test/java/com/imitatorModel/imitatorModel/`):
- `BugFixTest`: one test per fix above, plus the historical `mergeModel` policy.
- `DeterminismTest`: covers step 2.
- `FluentApiTest`: covers step 3. It also asserts that the fluent construction prints exactly what the classic construction prints.
- `ImitatorCheck`: runs `imitator -mode checksyntax` on emitted models when `$IMITATOR_BIN` is set or `imitator` is on `PATH`. Otherwise those 2 tests are skipped.

Run with `IMITATOR_BIN=…/external/imitator mvn test` (25 tests, all pass). `mvn install` now runs the tests.

### A.2 Step 2: deterministic output (§3.3)

- Variables are declared in **insertion order** (`LinkedHashMap` keyed by name).
- Actions are printed **sorted by name**, in the `actions:` line and when a multi-action or multi-target `Transition` is expanded. The reason: clients pass `Set.of(…)`, whose iteration order changes from one JVM run to the next.
- Locations keep insertion order. Transition order inside a location is unchanged.
- `ImitatorModel.setHeaderTimestamp(false)` removes the date from the header. `mergeModel` propagates the flag. The default stays `true`, so CherLog's output is unchanged.
- A correction to the original review: `HashSet<VariableType>`/`HashSet<Action>` iteration was arbitrary but stable across runs, because `String.hashCode` is deterministic. The real run-to-run variation came from the timestamp and from `Set.of`.

### A.3 Step 3: fluent API (§2) and the `DetectOccurrence` pilot

New types: `Term`, `NumericVariable`, `Imi`, `TransitionBuilder`. Everything is additive, and the old constructors still work.

| Feature | API |
|---|---|
| Variables are expressions | `Clock`, `Parameter`, `Rational` and `Int` now extend `NumericVariable implements Term`; `LinearExpr implements Term`. |
| Arithmetic | `plus/minus(Term \| long \| BigFraction)`, `times(long \| BigFraction)`, `neg()` |
| Comparisons → `ComplexConstraint` | `lt/le/eq/ge/gt(Term \| long)`, `cmp(Operator, Term \| long)`; `atom(Operator, Term)` returns a raw `Constraint` |
| Boolean atoms | `b.isTrue()`, `b.isFalse()` |
| Combinators | `c.and(…)`, `c.or(…)`; static `Imi.and/or/not`, `Imi.TRUE/FALSE`, `Imi.of(Constraint)` |
| Numbers | `Imi.q(n)`, `Imi.q(n, d)` (`BigFraction`); `Imi.c(n)`, `Imi.c(n, d)` (`LinearExpr`); `BigFraction.of(long[, long])` |
| Updates | `x.reset()`, `v.assign(Term \| long \| BigFraction)`, `b.assign(boolean \| ImiBoolean \| BooleanExpr)`, `u.when(cond)` (AND-ed with an existing condition), `Imi.updates(…)` |
| Transitions | `loc.on(a…).when(g…).doing(u…).goTo(target)`; `loc.silent()…`. `goTo` adds the transition and **declares its actions on the owning PTA**. That is always safe: IMITATOR rejects a sync on an undeclared action **[verified]**. |
| Locations | `invariant(…)`, `urgent()`, `accepting()`, `stop(Clock…)`, `flow(Clock, Term)`; all return `this` |
| PTA / model | `model.automaton(name)`, `pta.loc(name)` (get or create), `pta.initial(l)`, `pta.actions(a…)`, `pta.hasLocation(name)` |
| Introspection | `ComplexConstraint.getVariables()`, `LinearExpr.getCoefficients()/isConstant()/isPlusInfinity()/isMinusInfinity()`, `Update.getCondition()`, `BooleanExpr.getVariable()`, `Location.getOwner()` |

**Pilot:** CherLog's `DetectOccurrence.toImitatorModel` was rewritten with the fluent API (`monitoring-patterns-imitator`, not committed). The method went from 373 to 133 lines. Verification:
- The 7 suite tests and 6 extra cases (event with `(…]`, `ABSENCE` with parameters, `||` and `==` in signals, parametric `LASTING`, no interval) were run before and after the rewrite. All 13 `.imi` and 13 `.imiprop` files are **byte-identical** apart from the timestamp, and the IMITATOR results are identical.
- Full CherLog suite: **88/88 pass**, as before the changes. All 108 generated `.imi` files are equivalent to the pre-change baseline up to declaration order.
- Found while migrating: guards 21/22 were meant to contain `x < lasting`, but `guard21.addConstraint(…)` discarded its result, so the conjunct was never emitted. The pilot keeps the emitted model unchanged and marks this with a `NOTE` comment; whether to add the conjunct is a semantics decision. This is exactly the pitfall described in §4.4.

### A.4 Changes visible to existing client code

- `addVariable` returns `VariableType` and **throws** on a name/type conflict. `addVariables` accepts any `Collection`.
- `mergeModel` deliberately keeps its historical lenient policy (see §3.6 for a configurable one).
- `PTA.addLocation` **throws** on a duplicate name (except for a waiting placeholder).
- `getLocations()` / `getVariables()` return read-only snapshots.
- `LinearExpr.getTerms()` returns merged terms without zeros. `x + x` now prints `2 * x`.
- `Update` throws for invalid targets and conditions. `LinearExpr.toIMITATOR()` throws on infinity.
- Declaration order changed: variables in insertion order, actions sorted. The semantics are unchanged.

### A.5 All CherLog patterns migrated to the fluent API

Every pattern in `monitoring-patterns-imitator/src/main/java/com/patterns2imi/pattern/patternModel` now builds its monitor with the fluent API (not committed).

| | Before | After |
|---|---|---|
| Lines in the 13 pattern classes | 8,026 | 2,431 (−70 %) |
| `new Transition(` / `new ConstraintNode(new Constraint(` in pattern code | 336 / 173 | 0 / 0 |

What else changed in CherLog:
- Event and signal branches were merged where they emit the same transitions (`Count`, `LongestSequence`).
- Guards that were repeated are now named once (`inInterval`, `beforeLogEnd`, …).
- Transition comments are renumbered `location.k` in source order and describe the actual guards and targets.
- `generateProperty()` of the `Always*` patterns goes through two helpers on `AlwaysPattern`.
- A new helper `PatternUtils.logEvents(actions)` gives the log's events.
- Debug `println`s and commented-out code were removed.
- The known crash (`DETECT/ABSENCE` event `THEN NoRepeatTrigger` event, without `NoEarlyResponse`) used to be a `NullPointerException`. It now throws an explicit `UnsupportedOperationException`.

**API changes made for the migration:**

| Change | Why |
|---|---|
| `PTA.setAutoDeclareActions(boolean)` / `isAutoDeclareActions()` (default `true`) | Some monitors (`LongestSequence`, `LongestTimeframe` with an event) declare exactly the events of the log. If the pattern's event is absent from the log, auto-declaring it would make it an action the monitor can take **on its own** (no other automaton syncs on it). The emitted model would then be silently wrong instead of being rejected by IMITATOR. With the flag off, `goTo` declares nothing. |
| `Location.on(Collection<Action>)` | Transitions on computed action sets ("every other event"). An empty collection gives an unsynchronized transition, as `new Transition` did. |
| Removed `Update(LinearExpr, LinearExpr)` and its two conditional variants | No caller left. Use `target.assign(value)` / `.when(cond)`. |

Tests: `FluentApiTest.autoDeclarationCanBeDisabled`, `FluentApiTest.onCollectionOfActions`.

**Verification:**
- Corpus: **8,538 runs** of CherLog, generated from the grammar, with IMITATOR replaced by `/bin/true` so that only the `.imi`/`.imiprop` are produced. It covers:
  - every pattern form;
  - event / signal / `||` / `!=` / `LASTING` occurrences, events absent from the log, parameters;
  - all flag combinations;
  - open, closed, half-empty and degenerate (0, ∞) intervals;
  - two logs, plus the 90 suite tests.
- The reference was produced by a pristine build of CherLog's `HEAD`. Every `.imi` and `.imiprop` is **byte-identical** apart from the timestamp line. The only differences are the 36 runs of the known crash, where the error message changed.
- `python test/test.py`: 88/88 pass.

**Findings while migrating** (now open items):
- The printed DNF of a guard can depend on how its `AND`s are nested. LogicNG's `FormulaFactory` caches n-ary `AND`s by operand set. When conjuncts collapse (duplicates such as `beforeLogEnd` appearing twice, or bounds that simplify to `True` with a 0/∞ interval), an inner cached `AND` decides the conjunct order. Example: `and(and(yIn, tBeforeQ), B)` and `and(yIn, tBeforeQ, B)` can print differently. `x.and(y).and(z)` reproduces an `addConstraint` chain exactly. See §3.6.
- A transition whose guard simplifies to `False` prints nothing but still adds an empty line to its location. See §4.2.
- First kept unchanged and marked `NOTE` in the code, since fixed by the pattern author (the `NOTE` markers are gone). Each fix has regression tests in CherLog (`detectOccurrenceLasting*`, `longestsequence5-9`, `longesttimeframe5-9`, `AlwaysPrecedence11`):
  - `DetectOccurrence` 2.1/2.2 lacked `x < lasting`.
  - `LongestSequence` 2.5 did not store `cur_len` for events.
  - `LongestTimeframe` 1.12 went to `l2` when the signal constraint was violated at `t = M`.
  - `AlwaysPrecedence` 2.29 (two signals) used `NoRepeatTrigger` where the other cases use `NoRepeatPrecedent`.

---

### A.6 CherLog log automaton (`Log.java`) migrated

`Log.log2IMITATOR` (offline log automaton) and `Log.updateFromLog` (online increments) use the fluent API (1,121 → 691 lines, not committed).
- The log PTA uses `setAutoDeclareActions(false)`: it declares exactly its events, plus `sample`/`noSample` when it has signals.
- The sample updates are built by two helpers, `valuationUpdates` and `activationUpdates`.
- Debug `println`s were removed.

**Offline:** the 8,538-run corpus (A.5) was re-run. Every `.imi`/`.imiprop` is byte-identical to the reference.

**Online — a regression from A.1, now fixed.**
- **The problem.** `updateFromLog` re-adds the location the log was waiting in, under the same name. Before A.1 this silently produced an `.imi` with **two `loc lN`**, which IMITATOR rejects (`Several locations have name lN` **[verified]**). Since A.1 (location names unique), it threw `Location lN already exists`.
- **The fix.** `log2IMITATOR` now marks that last location as a waiting placeholder (the commented-out `setIsWaiting(true)` shows this was the intent). It is not printed, and `updateFromLog` replaces it.
- **Verification.** A harness drives `Log` like `LiveLogReader` + `CherLog` (initial model, three update batches, suffix) for a signals-only, a mixed and an events-only log. Compared with the original client and the original API, the only difference in the log automaton is the removed duplicate placeholder (besides A.2's declaration order).

**Online mode is still not usable.** Pre-existing issues, all **[verified]** with IMITATOR 3.4 on the harness output, kept unchanged and marked `NOTE` in `Log.java`:
1. The main file references the log automaton with `#include <file>`, which IMITATOR 3.4 does not parse.
2. Updates guard on `lock = 1` (a type error on a `bool`); the offline translation uses `lock`.
3. Updates always use `noSample` and the lock, so a log without signals syncs on an undeclared `noSample`.
4. Updates assign `xActivate` flags, but the initial model is built without them (`log2IMITATOR()` passes `needVariableActivationFlag = false`): `fActivate is assigned but never defined`.

### A.7 Queues (recording values along a run)

For CherLog's timestamp recording (`monitoring-patterns-imitator/TimestampRecording.md`): `ImiQueue` (`name : <rational|int|bool> queue;`, initialised `queue()`), `model.addQueue(name, ElementType)`, updates `q.push(e)` (`queue_push(e, q)`), `q.clear()` (`q <- queue()`), `q.assign(other)` (copy). A push is an `Update` whose term is a side effect (`UpdateTerm.isSideEffect()`), so it composes with `when`, `doing`, `ListUpdates`. Init values now come from `VariableType.initialValueToIMITATOR()`; `addVariable` also throws on a same-class variable with another IMITATOR type (`rational queue` vs `int queue`). CherLog `.imi` output unchanged (183/183 tests).

Only queues: they cover the needs (append in order, reset, snapshot) and print in insertion order in the accepting states. Lists (prepend, so reversed) and stacks add nothing for now; `queue_pop`/`queue_top`/`queue_length` in guards are not wrapped (not needed, and `queue_top` on an empty queue aborts IMITATOR).

**[verified]** with IMITATOR 3.4 (`QueueTest` + probes): a clock or a parameter in `queue_push` is an error (*Unable to use clock in this context*), so a time must be pushed through a discrete variable set by the log; `int` is not converted to `rational`; a side effect in a guard is an error; `queue_push` works inside `if … then … end` and with `<-`; `best <- ts` copies by value; updates are sequential; with parameters each accepting state (one per parameter region) carries its own queue, printed `ts = queue([1, 7/2])`; `queue_length(q)` is accepted in a property; `[]` is not an empty list (`list([])` is).

**Bug fixed: no `;` after a conditional update.** `ListUpdates` printed `if c then x <- 1 end ; y <- 2`, which IMITATOR rejects (Menhir parse error) **[verified]**; `if c then x <- 1 end y <- 2` is valid. Never hit before because a conditional update was always the last one; hit by CherLog's timestamp recording (`extremumTime` before `curComputed` in `Extrema`). `ListUpdates.toIMITATOR` now omits the separator after a conditional update (`QueueTest.conditionalUpdateFollowedByAnotherUpdate`).

## 0. Priorities at a glance (remaining)

| # | Change | Effort | Payoff |
|---|---|---|---|
| 1 | `Property` builder for `.imiprop` (§3.1) | small | ends string concatenation for properties |
| 2 | `validate()` before printing (§3.2) | medium | catches undeclared variables, actions, keyword names before IMITATOR does |
| 3 | Real initial values, parameter constraints (§3.4) | small | removes "value means constant" trap |
| 4 | Canonical guards before DNF (§3.6) | small (output changes once) | printed guards independent of how they were built |
| 5 | Sealed hierarchies and records; printer split out of the model (§4) | medium–large | simpler code, exhaustive `switch` |
| 6 | Move online mode out of `PTA`/`Location` (§4.4) | medium | core model has no I/O side effects |
| 7 | Make CherLog's online mode produce valid models (the 4 issues of A.6), then move it out of the core API (§4.4) | medium | online monitoring works |

---

## 1. Remaining correctness issues

- `Int`/`Rational`/`ImiBoolean` "value" means **constant definition** (`N = 3 : int`), not initial value. A discrete variable therefore cannot start at a non-default value. See §3.4.
- `Clock.is_continuous_initially_0()` always returns `true`: every clock is forced to `= 0` in `init`, with no opt-out.
- `Parameter` holds a **single** `Constraint` (`setConstraints` is singular despite its name; `geConstraint` is a typo for `getConstraint`). Parameters are not constrained to `>= 0` by default.
- `ImitatorModel.mergeModel` shares `PTA` objects between the source and merged models (mutations leak) and does not detect two PTAs with the same name.
- `ImitatorModel.addPTA` does not reject a duplicate PTA name (`automaton(name)` does).

---

## 2. Less verbose invocation (remaining)

- `ListUpdates` can disappear from public signatures: `Transition` could take `List<Update>` directly. The builder already hides it.
- `Location` and `PTA` flags still use `Boolean` (nullable) instead of `boolean`.
- Overloads that take a `Constraint` (e.g. `addConstraint(Constraint)`) could be dropped: CherLog no longer uses them, except `Log.updateFromLog` to reproduce the `lock = 1` guard (A.6).

---

## 3. Useful functions to add

### 3.1 `.imiprop` builder: `Property`

Today every pattern hand-writes `"property := #synth EF(loc[monitor] = l_detected);"`, with no link to the actual PTA and location names. Proposal:

```java
public sealed interface StatePredicate permits LocIs, DiscreteCmp, SpAnd, SpOr, SpNot, SpConst { String toIMITATOR(); }

StatePredicate sp = Sp.loc(monitor, lDetected);          // loc[monitor] = l_detected
Property p = Property.synth().EF(sp);                     // also witness(), exemplify()
Property q = Property.synth().AGnot(Sp.loc(monitor, lUnsat))
                     .project(p1, p2);                    // projectresult(p1, p2);
String text = p.toIMITATOR();
```
- Supported forms: `EF`, `AGnot`, `AG`, `AF`, `EU`, `AU`, `EFpmin/pmax`, `EFtmin`, `CycleThrough`, `IM`, `BCcover`, `Valid`, `DeadlockFree`.
- Because it takes `PTA`/`Location` objects, a renamed location breaks at compile time instead of yielding an unreachable-location result.
- State predicates may only use discrete variables. `Sp.cmp(Term…)` should reject clocks and parameters, as IMITATOR does. `ComplexConstraint.getVariables()` already provides the check.
- Timed variants (`EF_[a,b]`) should check that a clock named `global_time` exists.

### 3.2 `ImitatorModel.validate()` → `List<Diagnostic>`

Run it before `toIMITATOR()` (or automatically, with a `strict` flag). Checks:
- every PTA has an initial location, and it belongs to that PTA (today: only a clear error at print time);
- every transition target belongs to the same PTA (`Location.getOwner()` now makes this easy);
- every `sync a` action is declared in the PTA's `actions`. The fluent builder now guarantees this (unless `setAutoDeclareActions(false)`); transitions built with `new Transition` still need the check. Also the reverse: a declared but unused action blocks the whole network under broadcast. That is sometimes intended (e.g. `logEnd` in CherLog monitors), so it should be a warning;
- an action declared by **only one** automaton can be taken by it alone at any time. In a monitor this usually means the log does not contain that event (see A.5), so it should be a warning;
- every variable used in guards, invariants, updates and rates is declared in the model, with a type consistent with its use (no `int` in a clock constraint);
- names do not collide with IMITATOR keywords (`loc`, `init`, `do`, `done`, `A`, `E`, `EF`, `U`, `is`, `in`, …). **[verified]** A bool named `done` makes IMITATOR fail with a bare Menhir parse error. See the `imitator-modeling` skill pitfalls;
- no disjunction in invariants (already checked, but only at print time and as an exception);
- variables declared but never read. IMITATOR removes them with a warning, as seen with `lock`/`logEndTime` in generated test models.

### 3.3 Configurable output
The timestamp is optional now (A.2). A `PrintOptions` record could also cover:
- comment banners on/off;
- `:=` vs `<-` syntax;
- indentation;
- grouping same-type variables on one line (`x, y : clock;`).

### 3.4 Real initial values and init constraints
- Separate **constants** (`N = 3 : int;`) from **initial values** (`i := 3` in `init.discrete`). For example: `model.addInt("i").init(3)` vs `model.constInt("N", 3)`.
- `Clock.init(...)`: default `= 0`, with the option to leave a clock unconstrained or to set `x = p`.
- `model.initConstraint(ComplexConstraint)`: arbitrary linear constraints in `init.continuous` (`p1 + p2 <= 10`), instead of one `Constraint` per parameter.
- `Parameter`: `nonNegative()` by default (opt-out), `fixed(value)` for `p = 2 : parameter`, and `addConstraint` (plural).

### 3.5 Writing and running
```java
model.writeTo(Path imi);                         // Files.writeString(...)
property.writeTo(Path imiprop);
ImitatorResult r = Imitator.run(imitatorBin, imi, imiprop, "-mode", "statespace");  // ProcessBuilder wrapper
```
CherLog already has this logic (temp dirs, `.res` parsing). The test helper `ImitatorCheck` is a minimal start.

### 3.6 Model manipulation helpers
- `mergeModel` should **copy** the PTAs and offer a conflict policy (`KEEP_LEFT`, `KEEP_RIGHT`, `FAIL`) instead of the hard-coded "a rational from the right replaces another type; anything else is kept from the left".
- `PTA.rename(String)` / `Location.rename(...)` / `prefixLocations("mon_")`, to avoid name clashes when composing.
- `model.findVariable(name)` / `pta.findAction(name)` returning `Optional`.
- `ImitatorModel.toString()` = `toIMITATOR()` for easier debugging. `PTA.toDot()` to render an automaton in Graphviz.
- `ComplexConstraint.simplify()`: dropping `True` conjuncts, collapsing `False` disjuncts. Today the DNF pass does part of this only when printing transitions, not invariants.
- **Canonical guards before DNF.** Today the printed DNF can depend on the nesting of `AND`s, because LogicNG caches n-ary `AND`s by operand set (A.5). Flattening nested `AND`/`OR` and removing duplicate conjuncts before calling LogicNG would make the output depend only on the guard's content, not on how it was built. This changes existing outputs once (conjunct order only), so it needs its own regression run with the CherLog corpus.

---

## 4. Refactoring / rewriting suggestions

### 4.1 Sealed hierarchies and records (Java 21)
```java
public sealed interface Guard permits Atom, BoolAtom, And, Or, Const {}
public record Atom(LinearExpr left, Operator op, LinearExpr right) implements Guard {}
public record BoolAtom(ImiBoolean var, boolean negated)            implements Guard {}
public record And(List<Guard> children)                            implements Guard {}
public record Or(List<Guard> children)                             implements Guard {}
public enum   Const implements Guard { TRUE, FALSE }
```
- This replaces `ComplexConstraint` + `ConstraintNode` + `AndNode` + `OrNode` + the `truthConst` / `booleanTerm` fields of `Constraint`. Today `Constraint` is in practice a union of three things: a linear atom, a boolean atom, and a truth constant.
- `instanceof` chains in `toFormula`, `print`, `haveDisjunction`, `collectVariables`, `getVariables` become exhaustive `switch` expressions.
- The same treatment applies to `VariableType` → `sealed interface Variable permits Clock, Parameter, IntVar, RationalVar, BoolVar`. (`NumericVariable` already groups the four numeric kinds.)
- `Pair<T,U>` → `record`, or remove it: `LinearExpr` already uses a map internally, and `Location.rate` could be a `Map<Clock, LinearExpr>`.
- `Constraint` still simplifies in its constructor (positive clocks vs 0, infinity). A static `Guard.atom(l, op, r)` factory returning `Const.TRUE/FALSE` would keep the record trivial.
- `LinearExpr` could become `record LinearExpr(Map<VariableType, BigFraction> coeffs, BigFraction constant)`, since normalisation is done.

### 4.2 Separate printing from the model
Every class has a `toIMITATOR()`, and formatting decisions (banners, `<-`, newlines) are spread across 10 files. Move them into one `ImiPrinter` (with `PrintOptions`, §3.3) that pattern-matches over the sealed types. Benefits:
- one place to switch `<-` → `:=` or `init =` → `init :=` (IMITATOR 3.4's preferred syntax; the current one is still accepted, as verified with 3.4);
- output for other targets (e.g. UPPAAL XML, Graphviz) can be added without touching the model classes;
- a transition whose guard simplifies to `False` would print nothing at all. Today it leaves an empty line in its location (A.5).

### 4.3 Consistent mutability
The model mixes immutable value types (`LinearExpr`, `ComplexConstraint`, `Update`, `ListUpdates.addUpdate` returning a copy) with mutable structure (`Location`, `PTA`, `Transition.guard`). `ComplexConstraint.addConstraint` returns a new node, and forgetting to reassign is a silent bug: it happened in `DetectOccurrence` (A.3). The fluent API follows the intended convention:
- **expressions and constraints immutable**, with combinators named as values (`and`, `plus`);
- **structure mutable** through builders (`PTA`, `Location`), with methods returning `this`.

Remaining work: deprecate `addConstraint`/`addConstraints` (the names suggest mutation) in favour of `and`/`or`.

### 4.4 Move online mode into its own class
`PTA.separateFileName`, `pendingOnlineLocations`, `addOnelineLocation` (typo), `drainPendingLocations`, `appendToSeparateFile`, `addSuffixToSeparateFile`, `Location.isWaiting`, and `ImitatorModel.toOnlineIMITATOR` are all specific to the CherLog online log. Problems they cause:
- `PTA.toIMITATOR()` **writes a file** as a side effect when `separateFileName` is set;
- `Location.toIMITATOR()` returns `""` for "waiting" locations, and `PTA.addLocation` must special-case them (A.1).

Proposal: an `IncrementalPtaWriter` (or `OnlinePtaSink`) wrapping a `PTA` and a `Path`, with `writeHeader()`, `append(Location)`, `close()` (suffix). `PTA` becomes a pure data object; the include directive is emitted by the printer when the model is configured with `printer.includeExternally(pta, path)`.

### 4.5 Naming and packaging
- The package `com.imitatorModel.imitatorModel` repeats itself. Suggested: `com.imitatormodel` (lowercase, per Java convention) with subpackages `expr`, `model`, `print`, `property`, `online`.
- Java-style names: `isContinuousInitiallyZero` instead of `is_continuous_initially_0`; `ImiBoolean` → `BoolVar`; `Int` → `IntVar`; `getAction()` returning a `Set` → `getActions()`; `getTo()` → `getTargets()`; `setStop(Clock)` (adds) → `addStop(Clock)` (the fluent `stop(Clock…)` exists now).
- `Transition` holding `Set<Action>` and `Set<Location>` destinations is a non-IMITATOR shortcut (it expands into the cross product when printed). The builder already offers `on(a, b)`. Storing one action and one target per `Transition` would make the model match the language.

### 4.6 `BigFraction`
- `valueOf(String)` ("3/2", "0.25") would help CherLog's numeric normalisation.
- Infinity can no longer reach printed output (A.1), but a separate `ExtendedRational` type would make that guarantee static.

---

## 5. Tests and tooling for the library itself
- Unit tests exist now (A.1). Possible additions: golden-file tests comparing a whole `.imi` against an expected file, and running IMITATOR on a property (not only `checksyntax`) when the binary is available.
- Update `README.md`: it still says printing "was not much tested". Point it to the fluent API (`Main.trainGate()` is a complete example) and to the Claude skill `imitator-java-api`.

---

## 6. Migration order
1. ~~§1 bug fixes plus regression tests~~ (done, A.1).
2. ~~§3.3 deterministic output~~ (done, A.2).
3. ~~§2 fluent additions; migrate `DetectOccurrence` as a pilot~~ (done, A.3), ~~then all CherLog patterns~~ (done, A.5), ~~then the log automaton~~ (done, A.6).
4. §3.1 `Property` builder; replace the `generateProperty()` strings.
5. §3.2 `validate()`; enable it in CherLog's test run.
6. §4 internal rewrite (sealed types, printer, online writer) behind the now-stable fluent façade, then remove the deprecated constructors.
