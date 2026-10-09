# MVI patterns by complexity

One architecture, MVI, at four levels of complexity. A screen starts at the lowest level that fits and moves
up when it crosses a measurable line. Moving changes the screen's logic and nothing the user sees.

The runtime is in `mvi/`, the samples in `samples/`, and every sample is tested in `samples/src/commonTest`.
Run the tests with `./gradlew :mvi:jvmTest :samples:jvmTest`; run the app from `androidApp` or `iosApp`.

## The one contract

Every level has the same outside, `Store<State, Intent, Effect>`: a `StateFlow` of one immutable state,
`postIntent`, and effects delivered once. The screen is written against that and nothing else.

Every level has the same inside too, `Logic<State, Intent, Effect>`. It is a plain class that sees a
`StoreScope`: the state, `reduce`/`setState`, `emit`, and a scope. The logic does not know who hosts it, and
there are three hosts:

| Host | For |
|---|---|
| `StoreViewModel(initial, logic)` | a screen, or a component with its own ViewModel |
| `Slice(logic, get, set)` inside a parent logic | one part of a bigger screen |
| `StoreHost(initial, scope, logic)` | a test |

Because of this, moving a piece of logic is a change of host and not a rewrite. That is what keeps migration
between levels cheap. The ViewModel holds no behaviour: it is one line, and the logic is tested without it.

## The levels

| Level | Use it when | Mechanism | Sample |
|---|---|---|---|
| **1 · Direct** | No request can overlap another, and there are no exclusive modes | `Logic`: intents call `setState`, a request is `launch`ed by hand | `report/` |
| **2 · Actions** | Work overlaps: search as you type, double taps, a request per list item | `ActionLogic`: intent → action (parameters gathered) → `process` under a `Policy` → `Partial`s | `search/`, `feed/` |
| **3 · Machine** | The screen is in exactly one of several phases, and the transitions are the logic | `machineLogic`: intent → event → a `StateMachine` graph; work is a phase's `sideEffect` | `signin/level3/`, `createpost/publish/` |
| **4 · Slices** | Parts of the screen change for different reasons | A composition-root `Logic` with one `Slice` per part, each slice at its own level | `createpost/` |

Component ViewModels are not a level. They are a way of hosting: a component whose logic owns its IO gets
its own `StoreViewModel` (see `feed/`).

## Moving up: the lines

These are measurable on purpose. Each one is visible in a diff or in review.

- **1 → 2:** a second request can start while the first runs. In code, that is a `Job?` field, an
  `if (isLoading) return` that protects against a double tap, or a check that a result still matches its
  input (`if (state.query == query)`). Any one of these means level 2. A `Policy` names the rule once.
- **→ 3:** three or more flags exclude each other (`isSubmitting`, `isVerifying`, `isLocked`, `step`), or a
  handler starts with a guard that reads two flags. The flags then allow impossible combinations, and every
  new rule has to touch all the guards. With phases, the impossible combinations cannot be written.
- **→ 4:** the logic passes about 300 lines, or its intents fall into groups that do not touch each other's
  state (Act2Act's `CreatePostViewModel`: tags, poll, answers, submit). Each group becomes a slice.
- **slice → component ViewModel:** the part is drawn in more than one screen, or once per list item, and
  owns its IO (a like button, a follow button).
- **→ use case:** the logic is needed by two screens, or it orchestrates several repositories. A use case
  that only forwards one repository call is not written.

Moving down is allowed. When a screen loses features, take it back down a level.

### The migration, shown

`signin/` is one screen at three levels: the same intents, the same `SignInUi`, and the same screen.
`SignInBehaviourTest` runs seven behaviours against all three:

- **Level 1** works, and shows the cost: five flags, a guard at the top of every handler, and two `Job`s kept
  by hand so that two countdowns never tick at once.
- **Level 2** keeps the same state and moves the concurrency into policies. The jobs disappear, but the
  flags and the guards stay. That is the level 3 signal.
- **Level 3** has phases: `Editing`, `Submitting`, `Locked`, `EnteringCode`, `Verifying`, `SignedIn`. There
  are no flags and no guards, and the countdowns are side effects of their phases.

Every level maps its own state onto `SignInUi`, so the screen never sees which level is running.

## The pipeline, stage by stage

`Intent → (gather) → Action/Event → process → Partial → reduce → State → render`, with `Effect` beside
`State`.

| Stage | Owns | Without it |
|---|---|---|
| **Intent** | What the user did, in the user's words | The screen decides behaviour (`onClick = { vm.submit(email.trim()) }`) |
| **Gather** (intent → action or event) | Reading the state, collecting the parameters, validating, and the synchronous changes | `process` reads a state that changed while it was suspended. This is the "Event" layer of the original idea: it builds what the action needs |
| **Action / Event** | What to do, as data, with every parameter | No place to name a concurrency rule. Nothing plain to assert on |
| **Policy** (level 2) | How a new action meets a running one with the same key | Hand-kept `Job`s and staleness checks |
| **Process** | IO only | — |
| **Partial** | One change, applied in order to the state as it is at that moment | Two results that arrive together overwrite each other. Named partials also show up in logs and are testable on their own; `setState { }` is an anonymous one |
| **Effect** | One-off work: navigation, a message | Kept in state, it runs again on recomposition or when the user comes back |

A stage that adds nothing at a level is skipped. Level 1 goes from intent straight to `setState`.

## Splitting a big ViewModel

A use case is the only split we have today. These are the others, each with its boundary:

| Piece | Holds | Boundary |
|---|---|---|
| **Slice** | One part of the state, its intents, and its work | Never sees its siblings. It can be hosted alone |
| **Composition root** | Routing, rules that span slices, and gathering across slices | No IO of its own. See `CreatePostLogic` |
| **Binder** (`observe(flow) { }`) | A flow from outside (session, settings, event bus) mapped into the state | No decisions. Act2Act's `AppStateBinder` is one |
| **Gatherer** (`checkDraft()`) | Builds an action's parameters from the whole state, and validates them | A pure function, tested alone |
| **Partial** | One reduction | A pure function |
| **Mapper** (`toBodyUi()`) | Logic state → a component's UI state | Lives in the UI layer and is pure |
| **Use case** | Domain orchestration across repositories | No presentation types |
| **Manager** | Runtime state shared across screens | Not in a ViewModel, and not a `StateFlow` in a repository |

### Rules for slices

1. **Slices never reference each other.** When one needs something another has, the parent passes it as the
   parameters of an intent (`PublishIntent.Submit(draft)`).
2. **Each part of the state has one writer.** The slice writes its own part. The parent writes none of the
   slices' parts.
3. **Rules that span slices belong to the parent.** "Nothing is editable while publishing" is one `if` in
   the parent's routing, not a check in each slice.
4. **A slice is attached in the parent's `onStart`.** Its effects are mapped into the parent's effects, or
   pass through as they are when the type already fits (`StoreScope` is contravariant in effects).

This is why Act2Act's `RootViewModel` split did not hold. Its coordinators take `context(RootViewModel)`, so
each one sees all of the state and all of the ViewModel. They also hold references to each other
(`pushCoordinator.initialize(routing = routingCoordinator)`), and the order they start in is hidden in
`init`. The split is in name only, and the coupling stays where it was. Rebuilt as slices, each coordinator
owns a part of the root state, and the root logic routes between them.

## State style

There is one immutable state per store, and it changes only through partials. The UI gets component states
from mappers built on `select { }` (ArenaGo's `StateSelection`), so each component recomposes only for its
own part, and a component that later gets its own ViewModel already has its own state type.

**Act2Act's `StateHolder` is not MVI.** It is a mutable class of independent `stateOf` fields that any code
in the ViewModel can `set`:

- There is no single transition point. Logging, replaying, and tracking down "who changed this" are not
  possible.
- The state is not a value. It cannot be compared in a test, saved, or diffed.
- A change to several fields at once needs `snapshot { }`, and forgetting it produces intermediate states.

Its benefit is per-field recomposition, and immutable state with mappers gives the same benefit. New code
should use one immutable state and mappers.

## The state machine library

`dev.klitsie.statemachine` 0.4.1 is used for level 3, behind `MachineLogic`, so screens do not depend on it
directly. Lessons from building with it:

- **Never use its default `SharingStarted.WhileSubscribed(5s)`.** Five seconds after the screen leaves the
  foreground, it cancels every side effect, such as an upload or a countdown. `machineLogic` runs it
  eagerly on the host's scope and writes every state into the host's state, which is also what lets it run
  as a slice.
- **A `sideEffect` restarts whenever its key changes, and the default key is the whole state.** A phase that
  updates its own data (an upload's progress, a countdown) has to key its work on what identifies it:
  `key = { it.draft }` or `key = { Unit }`. Otherwise the work restarts on every update.
- **Phases are immutable sealed classes.** They fit the immutable-state rule as they are.
- **The library is young and has one maintainer.** That is why it sits behind our own type. Its compiler
  plugin (which checks for missing or duplicate states) needs Kotlin 2.4 and is not tried yet.

## Back to arch-kit

Everything here maps onto arch-kit. The question is how far to take it:

- `Logic` + `StoreScope` replace "the ViewModel implements the plugins". Level 1 is arch-kit as it is today:
  `setState { copy() }` has the same name on purpose.
- `ActionLogic`/`Policy`, `Slice`, and `MachineLogic` are new, and they are additive.
- `ContentState` is arch-kit's, with a `Throwable` in place of `Failure`.
- What is not here: the plugin registry, `UiString`, forms, `EventBus`, and DI factories. They stay as they
  are, and a `Logic` uses them through its constructor.

## Open questions

- Should a slice expose outputs to its parent other than effects, for example "the tag limit is reached, so
  disable X"? So far, the parent reading the slice's state has been enough.
- Should component ViewModels live per screen (as here) or per list item (Act2Act's
  `rememberViewModelStoreOwner`)? Per item frees memory, but the like state is lost when the item scrolls
  out of view.
- Can the "lines" be Detekt rules? Counting flags and guards, the size of a `Logic`, and slice-to-slice
  references all look checkable.
