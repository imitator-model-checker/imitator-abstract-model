# `com.imitatorModel` API: suggested improvements

Review of the Java API in this folder (`imitatorModel/*`, `bigFraction/*`, `Main.java`) and how it is used by its main client, `monitoring-patterns-imitator` (CherLog). **No code was changed.** Everything below is a proposal.

Items tagged **[verified]** were reproduced, either with `jshell` against `target/classes` or by running IMITATOR 3.4 `-mode checksyntax` on emitted text. Untagged items come from reading the code.

Usage numbers from the client codebase give a sense of where verbosity costs the most:

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

## 0. Priorities at a glance

| # | Change | Effort | Payoff |
|---|---|---|---|
| 1 | Fix the correctness bugs in §1 | small | avoids silently wrong `.imi` files |
| 2 | Static factories plus fluent methods on `LinearExpr` and variables (§2.1–2.3) | small, additive | removes most of the boilerplate |
| 3 | `Location.on(...)` transition builder (§2.4) | small, additive | `new Transition(g, a, null, l)` becomes one readable line |
| 4 | `Property` builder for `.imiprop` (§3.1) | small | ends string concatenation for properties |
| 5 | `validate()` before printing (§3.2) | medium | catches undeclared variables, actions and missing initial locations before IMITATOR does |
| 6 | Deterministic output (§3.3) | trivial | output that can be diffed, cached and regression-tested |
| 7 | Sealed hierarchies and records; printer split out of the model (§4) | medium–large | simpler code, exhaustive `switch` |
| 8 | Move online mode out of `PTA`/`Location` (§4.5) | medium | core model has no I/O side effects |

Items 2–6 are **additive**: the existing constructors can stay (mark them `@Deprecated` later), so CherLog keeps compiling while it migrates.

---

## 1. Correctness bugs and pitfalls (fix first)

### 1.1 `ImiBoolean` constant prints lowercase `true`/`false` **[verified]**
[`ImitatorModel.generateVarDeclaration`](imitatorModel/ImitatorModel.java#L166) appends `variable.getValue()`, which for `ImiBoolean("b", true)` is `Boolean.toString()`, so the output is `b = true: bool;`. IMITATOR rejects it with `Expression "true" uses undeclared variable or constant`.
**Fix:** give each variable class its own `valueToIMITATOR()` (`True`/`False` for bool, `BigFraction` formatting for rational).

### 1.2 Negating `TRUE`/`FALSE` is broken **[verified]**
- `Constraint.TRUE.negate()` throws `IllegalStateException: Unexpected operator: EQ`. The reason: `TRUE`/`FALSE` are stored as `0 = 0` / `0 > 0`, and `Operator.getInverse()` has no `EQ` case.
- `new ConstraintNode(Constraint.TRUE).negate()` returns `0 < 0 | 0 > 0`. That is a disjunction, so used as an **invariant** it throws "Invariant contains disjunction"; used as a guard it produces two dead transitions.

**Fix:** in `Constraint.negate()` and `ConstraintNode.negate()`, check `truthConst` **first** and return `FALSE`/`TRUE`. Better still, make TRUE/FALSE their own node type (§4.1) instead of a `Constraint` that happens to carry a flag.

### 1.3 `LinearExpr.toIMITATOR` with zero coefficients **[verified]**
`new LinearExpr(x, ZERO).add(y)` prints ` + y`. An expression whose only term has coefficient 0 prints the empty string, which gives `invariant  <= 3`. A comment on [line 169](imitatorModel/LinearExpr.java#L169) already flags this.
**Fix:** filter out zero terms before joining (`String.join(" + ", nonZeroTerms)`).

### 1.4 `LinearExpr` never merges like terms **[verified]**
`x + x` prints `x + x`, and `new LinearExpr(x).add(x).equals(new LinearExpr(x, 2))` is `false`. As a result, `equals`/`hashCode`, which `toDNF()` relies on to share LogicNG variables, treat equal expressions as different atoms.
**Fix:** store terms as a `LinkedHashMap<VariableType, BigFraction>` and normalise in the constructor (merge, drop zeros). §4.2 has a sketch.

### 1.5 Infinity can reach the output **[verified]**
`BigFraction.INFINITY.toString()` is `inf`, which IMITATOR does not parse (`x <= inf` gives a Menhir parse error). The `Constraint` constructor simplifies only when an entire side *equals* `LinearExpr.INFINITY`; `x <= y + inf` is printed as is.
**Fix:** in the `Constraint` constructor, treat any side with an infinite constant as ±∞ (true/false by operator). Also make `LinearExpr.toIMITATOR` throw if an infinite constant survives.

### 1.6 `Location.equals(Location)` / `PTA.equals(PTA)` overload instead of override **[verified]**
`equals(Location o)` does not override `equals(Object)`, so `LinkedHashSet<Location>` falls back to identity. `addLocation(new Location("l1"))` called twice adds **two** `l1` locations (the "Location already exists" check never fires), and IMITATOR then fails on the duplicate. `PTA` has the same problem and no `hashCode` either.
**Fix:** `@Override public boolean equals(Object o)` plus `hashCode` on `name`. Better, key locations by name in a `LinkedHashMap<String, Location>`.

### 1.7 `Location.addTransitions` ignores its argument
[`Location.java:84-87`](imitatorModel/Location.java#L84-L87): `transitions.addAll(transitions)` (should be `transition`). The method duplicates the existing list instead of adding the new transitions.

### 1.8 Same-name variables silently dropped **[verified]**
`VariableType.equals` compares only `name`. `model.addVariable(new ImiBoolean("b", true)); model.addVariable(new Clock("b"));` keeps only the boolean, with no warning. `addVariableOverwrite` replaces only when the **type differs**: a same-type variable with a different value keeps the *old* value.
**Fix:** have `addVariable` return the existing variable when name and type match, and throw when the name matches but the type differs. Expose an explicit `replaceVariable` for the overwrite case.

### 1.9 Conditional updates may reference clocks **[verified]**
`Update(condition, …)` accepts any `ComplexConstraint`. IMITATOR rejects `if x <= 1 then … end` when `x` is a clock (`Clock x found in if …`).
**Fix:** check in the constructor that the condition contains only discrete variables (bool/int/rational).

### 1.10 Other smaller issues
- `Update(LinearExpr term1, LinearExpr term2)` silently uses the **first variable** of `term1` as the target and throws `IndexOutOfBounds` if `term1` is constant. Remove it; the target of an update is a variable, not an expression.
- `Transition.toIMITATOR` detects false guards with `==` identity (`c.getConstraint() == Constraint.FALSE`). Use `Boolean.FALSE.equals(c.getConstraint().getTruthConst())`.
- `PTA.addLocation` calls `locations.add(location)` twice.
- `generateInitialization` throws NPE when a PTA has no initial location. It should raise a clear error, or `validate()` should catch it (§3.2).
- `Int`/`Rational`/`ImiBoolean` "value" means **constant definition** (`N = 3 : int`), not initial value. A discrete variable therefore cannot start at a non-default value. See §3.4.
- `Clock.is_continuous_initially_0()` always returns `true`: every clock is forced to `= 0` in `init`, with no opt-out.
- `Parameter` holds a **single** `Constraint` (`setConstraints` is singular despite its name; `geConstraint` is a typo for `getConstraint`). Parameters are not constrained to `>= 0` by default, which IMITATOR guidance recommends.
- `BigFraction.random(1)` calls `nextInt(0)`, which throws `IllegalArgumentException`.
- `BigFraction.getIMITATORType()` returns `"BigFraction"`, a leftover with no meaning.
- `ImitatorModel.mergeModel` shares `PTA` objects between the source and merged models (mutations leak) and does not detect two PTAs with the same name.
- `Main.java` contains only commented-out code that no longer compiles (`ConjunctionOfConstraints` no longer exists), but `pom.xml` runs it during `compile` through `exec-maven-plugin`. Replace it with a real smoke test (§5) and remove the exec binding.

---

## 2. Less verbose invocation

The goal is to make Java read like the `.imi` it produces. All of the following can be **added** next to the current API.

### 2.1 Static factory entry point: `Imi`

```java
public final class Imi {
    public static BigFraction q(long n)          { return new BigFraction((int) n); }
    public static BigFraction q(long n, long d)  { return new BigFraction((int) n, (int) d); }
    public static LinearExpr  c(long n)          { return new LinearExpr(q(n)); }
    public static LinearExpr  c(long n, long d)  { return new LinearExpr(q(n, d)); }
    public static Action      action(String n)   { return new Action(n); }

    public static ComplexConstraint and(ComplexConstraint... cs) { return new AndNode(List.of(cs)); }
    public static ComplexConstraint or (ComplexConstraint... cs) { return new OrNode(List.of(cs)); }
    public static ComplexConstraint not(ComplexConstraint c)     { return c.negate(); }
    public static final ComplexConstraint TRUE  = new ConstraintNode(Constraint.TRUE);
    public static final ComplexConstraint FALSE = new ConstraintNode(Constraint.FALSE);
}
```
Used through `import static com.imitatorModel.imitatorModel.Imi.*;`.

### 2.2 Variables behave as expressions

Today every variable is wrapped by hand: `LinearExpr t = new LinearExpr(model.addClock("t"));`. Instead, have `VariableType` (or a `Term` interface shared with `LinearExpr`) expose arithmetic and comparisons:

```java
public interface Term {                    // implemented by LinearExpr and by numeric VariableTypes
    LinearExpr expr();                     // variable -> 1·v + 0
    default LinearExpr plus(Term o)         { return expr().add(o.expr()); }
    default LinearExpr plus(long k)         { return expr().add(q(k)); }
    default LinearExpr minus(Term o)        { return expr().minus(o.expr()); }
    default LinearExpr times(BigFraction k) { … }

    default ComplexConstraint lt(Term o) { return atom(this, Operator.LT, o); }
    default ComplexConstraint le(Term o) { … }
    default ComplexConstraint eq(Term o) { … }
    default ComplexConstraint ge(Term o) { … }
    default ComplexConstraint gt(Term o) { … }
    default ComplexConstraint le(long k) { return le(c(k)); }   // and so on for each operator
    // returns ComplexConstraint (already wrapped in a ConstraintNode)
}
```

Combining constraints also gets shorter: `ComplexConstraint.and(other)` / `.or(other)` instead of `addConstraint(other, LogicalOperator.AND)`.

### 2.3 Updates read as assignments

```java
Update u1 = x.reset();                    // x := 0
Update u2 = i.assign(i.plus(1));          // i := i + 1
Update u3 = b.assign(true);               // b := True
Update u4 = cur.assign(t).when(flag);     // if flag then cur := t end
```
These replace the 18 overloaded `Update` constructors, which differ only in the combination of `(Constraint | ComplexConstraint | none) × (LinearExpr | Boolean | ImiBoolean | BooleanExpr | lists)`. Keep a single canonical constructor `Update(ComplexConstraint condition, VariableType target, UpdateTerm term)` and validate the types there.

`ListUpdates` can disappear from the public API: a transition can take `Update...` or `List<Update>` directly.

### 2.4 Transition builder on `Location`

```java
l1.on(a).when(t.lt(M), beforeLogEnd).goTo(l1);
l1.on(a).when(M.le(t), t.le(N), beforeLogEnd).doing(cur.reset()).goTo(lDetected);
l2.silent().when(t.ge(N)).goTo(lUnsat);                      // ε-transition, no sync
```
`on(...)` returns a small mutable `TransitionBuilder`; `goTo(...)` creates the `Transition` and calls `addTransition`. Varargs in `when(...)` are combined with AND.

### 2.5 Location / PTA / model builders

```java
PTA monitor = model.automaton("monitor");          // creates and registers the PTA
Location l1 = monitor.loc("l1").invariant(t.le(N)); // created, added, returned
Location ok = monitor.loc("l_detected").urgent();
monitor.initial(l1);
monitor.actions(logEnd, a);                         // varargs
```
- `PTA.loc(name)` returns the existing location or creates it, which removes the `new Location` + `addLocation` pair.
- `urgent()`, `accepting()`, `stop(Clock...)`, `flow(Clock, rate)` return `this` for chaining. Use plain `boolean` instead of `Boolean` for the flags.
- `Location.goTo` / `on` could **auto-declare** the action on the owning PTA (each `Location` keeps a back-reference to its `PTA`). Declaring actions is easy to forget, and IMITATOR's broadcast semantics make a missing declaration a silent bug. It should be opt-out, because declaring an action in a PTA *changes* synchronization semantics.

### 2.6 Before / after on real client code

From `DetectOccurrence.toImitatorModel` (CherLog), the "guard 12" transition:

```java
// before (13 lines)
ComplexConstraint guard12 = AndNode.fromConstraints( List.of(
        new Constraint(M, isLeftClosed ? Operator.LE : Operator.LT, t),
        new Constraint(t, isRightClosed ? Operator.LE : Operator.LT, N)
));
guard12 = guard12.addConstraint(beforeLogEnd);
Transition tr12 = new Transition(guard12, action, null, l_detected);
l1.addTransition(tr12);
```
```java
// after (3 lines)
l1.on(action)
  .when(M.cmp(isLeftClosed ? LE : LT, t), t.cmp(isRightClosed ? LE : LT, N), beforeLogEnd)
  .goTo(lDetected);
```
(`cmp(Operator, Term)` covers interval endpoints that are open or closed depending on a flag, a case that comes up often in CherLog.)

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
- State predicates may only use discrete variables. `Sp.cmp(Term…)` should reject clocks and parameters, as IMITATOR does.
- Timed variants (`EF_[a,b]`) should check that a clock named `global_time` exists.

### 3.2 `ImitatorModel.validate()` → `List<Diagnostic>`

Run it before `toIMITATOR()` (or automatically, with a `strict` flag). Checks:
- every PTA has an initial location, and it belongs to that PTA;
- every transition target belongs to the same PTA;
- every `sync a` action is declared in the PTA's `actions` (and the reverse: a declared but unused action blocks the whole network under broadcast);
- every variable used in guards, invariants, updates and rates is declared in the model, with a type consistent with its use (no `int` in a clock constraint, no clock in a conditional update, no parameter update);
- names do not collide with IMITATOR keywords (`loc`, `init`, `do`, `done`, `A`, `E`, `EF`, `U`, `is`, `in`, …). **[verified]** A bool named `done` makes IMITATOR fail with a bare Menhir parse error. See the `imitator-modeling` skill pitfalls;
- no disjunction in invariants (already checked, but only at print time and as an exception);
- variables declared but never read. IMITATOR removes them with a warning, as seen with `lock`/`logEndTime` in generated test models.

### 3.3 Deterministic, configurable output
- `variables` is a `HashSet`, and `PTA.actions` and `Transition.actions` are `HashSet`/`Set.of`, so declaration and action order change between JVM runs (for example `actions: a, logEnd, b, ;`). Use `LinkedHashSet` / `LinkedHashMap`, or sort by name when printing.
- The header embeds `ZonedDateTime.now()`, so two identical models never compare equal byte for byte. Make the timestamp optional (`PrintOptions.timestamp(false)`).
- A `PrintOptions` record could also cover: comment banners on/off, `:=` vs `<-` syntax, indentation, grouping same-type variables on one line (`x, y : clock;`).

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
CherLog already has this logic (temp dirs, `.res` parsing); moving a minimal, generic version into this library would let other clients reuse it.

### 3.6 Model manipulation helpers
- `mergeModel` should **copy** the PTAs, offer a conflict policy (`KEEP_LEFT`, `KEEP_RIGHT`, `FAIL`) instead of the hard-coded "rational from the right overwrites", and detect duplicate PTA names.
- `PTA.rename(String)` / `Location.rename(...)` / `prefixLocations("mon_")`, to avoid name clashes when composing.
- `model.findVariable(name)` / `pta.findAction(name)` returning `Optional`.
- `ImitatorModel.toString()` = `toIMITATOR()` for easier debugging. `PTA.toDot()` to render an automaton in Graphviz, handy when debugging pattern translations.
- `LinearExpr.simplify()` / `ComplexConstraint.simplify()`: constant folding, dropping `True` conjuncts, collapsing `False` disjuncts. Today the DNF pass does part of this only when printing transitions, not invariants.

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
- `instanceof` chains in `toFormula`, `print`, `haveDisjunction`, `collectVariables` become exhaustive `switch` expressions, so the compiler flags a missing case.
- The same treatment applies to `VariableType` → `sealed interface Variable permits Clock, Parameter, IntVar, RationalVar, BoolVar`, each a `record(String name)`.
- `Pair<T,U>` → `record Pair<T,U>(T first, U second)`, or better, remove it: `LinearExpr` uses a `Map`, and `Location.rate` a `Map<Clock, LinearExpr>`.
- Fold the infinity simplifications of the `Constraint` constructor (~100 lines of `if/else`) into a static `Guard.atom(l, op, r)` factory that returns `Const.TRUE/FALSE` when decidable. The constructor stays trivial.

### 4.2 Normalised `LinearExpr`
```java
public record LinearExpr(Map<Variable, BigFraction> coeffs, BigFraction constant) {
    public LinearExpr { coeffs = normalise(coeffs); }   // merge, drop zeros, LinkedHashMap → unmodifiable
}
```
`equals` and `hashCode` are then correct automatically, which fixes §1.3 and §1.4 together and makes `toDNF` sharing reliable.

### 4.3 Separate printing from the model
Every class has a `toIMITATOR()`, and formatting decisions (banners, `<-`, newlines) are spread across 10 files. Move them into one `ImiPrinter` (with `PrintOptions`, §3.3) that pattern-matches over the sealed types. Benefits:
- one place to switch `<-` → `:=` or `init =` → `init :=` (IMITATOR 3.4's preferred syntax; the current one is still accepted, as verified with 3.4);
- output for other targets (e.g. UPPAAL XML, Graphviz) can be added without touching the model classes.

### 4.4 Consistent mutability
The model mixes immutable value types (`LinearExpr`, `ListUpdates.addUpdate` returning a copy) with mutable ones (`Location`, `PTA`, `Transition.guard`, which is not final), and `ComplexConstraint.addConstraint` returns a new node. Users write `guard = guard.addConstraint(...)` and forgetting the assignment is a silent bug. Pick one convention:
- **expressions and constraints immutable** (records), with combinators named as values (`and`, `plus`) so nobody expects mutation;
- **structure mutable** through builders (`PTA`, `Location`), with methods returning `this`.

### 4.5 Move online mode into its own class
`PTA.separateFileName`, `pendingOnlineLocations`, `addOnelineLocation` (typo), `drainPendingLocations`, `appendToSeparateFile`, `addSuffixToSeparateFile`, `Location.isWaiting`, and `ImitatorModel.toOnlineIMITATOR` are all specific to the CherLog online log. Problems they cause:
- `PTA.toIMITATOR()` **writes a file** as a side effect when `separateFileName` is set;
- `Location.toIMITATOR()` returns `""` for "waiting" locations, a concept the core model should not know about.

Proposal: an `IncrementalPtaWriter` (or `OnlinePtaSink`) wrapping a `PTA` and a `Path`, with `writeHeader()`, `append(Location)`, `close()` (suffix). `PTA` becomes a pure data object; the include directive is emitted by the printer when the model is configured with `printer.includeExternally(pta, path)`.

### 4.6 Naming and packaging
- The package `com.imitatorModel.imitatorModel` repeats itself. Suggested: `com.imitatormodel` (lowercase, per Java convention) with subpackages `expr`, `model`, `print`, `property`, `online`.
- Java-style names: `isContinuousInitiallyZero` instead of `is_continuous_initially_0`; `ImiBoolean` → `BoolVar`; `Int` → `IntVar` (`Int` reads like a primitive wrapper); `getAction()` returning a `Set` → `getActions()`; `getTo()` → `getTargets()`; `setStop(Clock)` (adds) → `addStop(Clock)`.
- `Transition` holding `Set<Action>` and `Set<Location>` destinations is a non-IMITATOR shortcut (it expands into the cross product when printed). Keep it as a builder convenience (`on(a, b).goTo(l)`) but store one action and one target per `Transition`, so the model matches the language.

### 4.7 `BigFraction`
Fine as is, but consider:
- `long`-based convenience constructors and `valueOf(String)` ("3/2", "0.25"), which would also help CherLog's numeric normalisation;
- rejecting infinity in `toString()`, or giving it a separate type `ExtendedRational`, so infinity cannot leak into printed output (§1.5).

---

## 5. Tests and tooling for the library itself
- There are no unit tests. Add JUnit 5 golden tests: build a model → `toIMITATOR()` → compare with an expected `.imi`, and optionally run `imitator -mode checksyntax` when the binary is on `PATH` (skip otherwise). The bugs in §1.1, §1.2, §1.3, §1.5 and §1.6 are each a one-line test.
- Add a round-trip smoke test for `toDNF()` (negation and disjunction splitting in guards).
- Replace `Main.java` with an `examples/` package that builds the train/gate example from the IMITATOR manual using the new fluent API. It doubles as documentation.
- Update `README.md`: it still says printing "was not much tested". Point it to the API (and to the Claude skill `imitator-java-api`).

---

## 6. Suggested migration order
1. §1 bug fixes plus regression tests (no API change).
2. §3.3 deterministic output (lets CherLog's test suite compare `.imi` files).
3. §2 fluent additions next to the existing API; migrate one CherLog pattern (e.g. `DetectOccurrence`) as a pilot.
4. §3.1 `Property` builder; replace the `generateProperty()` strings.
5. §3.2 `validate()`; enable it in CherLog's test run.
6. §4 internal rewrite (sealed types, printer, online writer) behind the now-stable fluent façade, then remove the deprecated constructors.
