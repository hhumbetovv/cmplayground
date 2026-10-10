# MVI reference

How the runtime works and why it is built this way. To write a screen, start with the [guide](guide.md); to
move a screen between levels, read [migrations](migrations.md). This page is what both of them link to for
the details.

The runtime is in `mvi/`, its test DSL in `mvi-test/`, the samples in `samples/`, and every sample is tested
in `samples/src/commonTest`. Run the tests with `./gradlew :mvi:jvmTest :samples:jvmTest`; run the app from
`androidApp` or `iosApp`.

## The one contract

Every level has the same outside, `Store<State, Intent, Effect>`: a `StateFlow` of one immutable state,
`postIntent`, and effects delivered once. The screen is written against that and nothing else.

Every level has the same inside too, `Logic<State, Intent, Effect>`. It is a plain class that sees a
`StoreScope`: the state, `reduce`/`setState`, `emit`, and a scope. The logic does not know who hosts it, and
there are five hosts:

| Host | For |
|---|---|
| `store(initial, logic)` in a ViewModel that is a `StoreOwner` | a screen, or a component with its own ViewModel |
| `Slice(logic, get, set, onEffect)` inside a parent logic | one part of a bigger screen, for as long as the screen lives |
| `Slot(get, set, create, onEffect)` inside a parent logic | a part that exists only while it is open: a dialog, a sheet |
| `ItemScope(stores, key)` around a list item | a component ViewModel per list item |
| `StoreHost(initial, scope, logic)`, or `storeTest { }` | a test |

Because of this, moving a piece of logic is a change of host and not a rewrite. That is what keeps migration
between levels cheap. The ViewModel holds no behaviour: it is one line, and the logic is tested without it.

```kotlin
class SearchViewModel(users: UserRepository) :
    ViewModel(),
    StoreOwner<SearchState, SearchIntent, SearchEffect> {
    override val store = store(SearchState(), SearchLogic(users))
}
```

`StoreOwner` is an interface, named after Android's owners (`LifecycleOwner`, `ViewModelStoreOwner`): the
ViewModel hands out its store and is not one itself. Being an interface, it leaves the superclass free for an
app's own base ViewModel. The screen is given `viewModel { … }.store`.
The feed sample shows the extreme case: the report screen's logic, unchanged, runs as a screen and as a sheet.

### The screen sees only what it draws

A logic keeps the state it needs, which is often not what the screen draws: phases, ids, counters.
`store.mapState { it.toUi() }` gives the screen a `Store<Ui, …>` (Orbit's internal and external state). The
sign-in sample keeps three different states behind one `Store<SignInUi, …>`, so its screen cannot tell which
level runs. The mapping runs on every read: it must be pure and cheap. Per-component mapping stays in the UI,
with `select { }`. When to use it, what it costs, and how to test through it: [guide, section 11](guide.md#11-internal-state).

### Effects

- **One collector.** Each effect goes to one collector. A second collector at the same time fails at once,
  because two collectors would race for each effect and lose half of them silently.
- **Never dropped.** An effect emitted while no one collects (the screen is in the background) waits for the
  next collector.
- **Handled before the screen can leave.** `CollectEffects` collects on `Dispatchers.Main.immediate`, so an
  effect taken from the store is not lost between the store and the screen.

### How a store runs: `StoreSettings`

These are set once for the app, through its base ViewModel or its DI:

- `start`:
  - `Lazily` (the default) runs the logic's `onStart` on first use: the first intent, or the first collector.
    A ViewModel created but never shown does no work.
  - `Eagerly` runs it when the store is created.
  - `Manually` runs it only when `start()` is called. Tests use it to look at the state before the start.
- `onError` gets every failure, in an intent or in launched work. The store keeps working.
- `observer` sees every intent, every change of state (with the `Partial` that made it), and every effect, in
  order. It is the hook for debug logs and analytics, and the test DSL is built on it. Named partials make
  the logs readable: `SearchPartial.Results`, not `<lambda>`.

## The levels

| Level | Use it when | Mechanism | Sample |
|---|---|---|---|
| **1 · Direct** | No request can overlap another, and there are no exclusive modes | `Logic`: intents call `setState`, a request is `launch`ed by hand | `report/` |
| **2 · Actions** | Work overlaps: search as you type, double taps, a request per list item | `ActionLogic`: intent → action (parameters gathered) → `process` under a `Policy` → `Partial`s | `search/`, `feed/` |
| **3 · Phases** | The screen is in exactly one of several phases, and the transitions are the logic | A sealed state of phases. Either plain code (`whileIn`, `runOn`) or a graph in the state-machine library (`machineLogic`) | `signin/level3/`, `createpost/publish/` |
| **4 · Slices** | Parts of the screen change for different reasons | A composition-root `Logic` with one `Slice` per part, each slice at its own level | `createpost/` |

Component ViewModels are not a level. They are a way of hosting: a component whose logic owns its IO gets
its own ViewModel (see `feed/`).

### Level 3, with or without the library

The phases, and the transitions between them, are the same either way. In the sign-in sample, both versions
call the same pure functions (`submitted()`, `answered(result)`, `ticked()`). Only the wiring differs:

| | Plain (`SignInPhasesLogic`) | Library (`signInLevel3Logic`) |
|---|---|---|
| An intent | `when (phase)`, then `when (intent)`; a phase that does not list it ignores it | An event; a phase without a handler ignores it |
| A phase's work | `whileIn(Phase::class) { }` starts on entering, is cancelled on leaving | `sideEffect { }` |
| A result | `setState { answered(result) }`, which is ignored if the phase was left | An internal event, `send(Answered(result))`, and a handler |
| Work an intent starts within a phase | `runOn(Phase::class) { }`, cancelled if the phase is left | A key on a side effect (`key = { it.resends }`) |
| Extra types | None | A sealed class of events |

**Start plain.** Move to the library when the graph is big enough that it reads better as one declaration,
or when its compile-time check (missing or duplicate phases) is worth a dependency. `whileIn` restarts its
work only on entering the phase, so it has no "key defaults to the whole state" trap; when the work depends
on something inside the phase, it takes a `key`.

## Moving between levels

The lines that say when to move up or down, a recipe for every move, and the sign-in screen written at every
level are in [migrations](migrations.md).

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
| **Slot** | A part that exists only while open | Its work dies when it closes; its outputs go to the parent |
| **Composition root** | Routing, rules that span slices, gathering across slices, and acting on their outputs | No IO of its own. See `CreatePostLogic` |
| **Binder** (`observe(flow) { }`) | A flow from outside (session, settings, event bus) mapped into the state | No decisions. Act2Act's `AppStateBinder` is one |
| **Gatherer** (`checkDraft()`) | Builds an action's parameters from the whole state, and validates them | A pure function, tested alone |
| **Transition** (`answered(result)`) | One move from a phase to the next | A pure function, shared by every wiring of level 3 |
| **Partial** | One reduction | A pure function |
| **Mapper** (`toBodyUi()`) | Logic state → a component's UI state | Lives in the UI layer and is pure |
| **Use case** | Domain orchestration across repositories | No presentation types |
| **Manager** | Runtime state shared across screens | Not in a ViewModel, and not a `StateFlow` in a repository |

### Rules for slices and slots

1. **Children never reference each other.** When one needs something another has, the parent passes it as
   the parameters of an intent (`PublishIntent.Submit(draft)`).
2. **Each part of the state has one writer.** The child writes its own part. The parent writes none of the
   children's parts, except to open or close a slot, or to reset them all.
3. **Rules that span children belong to the parent.** "Nothing is editable while publishing" is one `if` in
   the parent's routing, not a check in each slice.
4. **A child's effects are its outputs, and they go to the parent.** `onEffect` decides: forward one to the
   screen (`emit`), or act on it. When the publish slice says "published", the parent clears the draft and
   then tells the screen. When the report slot says "sent", the feed closes it. This is Decompose's rule: a
   child talks only to its parent. `slice(logic, get, set)` forwards everything as it is.
5. **A child is attached in the parent's `onStart`.** Each child gets a scope of its own inside the
   parent's (Decompose's child lifecycle): detaching a slice or closing a slot cancels its work and nothing
   else. A child that has been closed cannot write into the parent any more.

This is why Act2Act's `RootViewModel` split did not hold. Its coordinators take `context(RootViewModel)`, so
each one sees all of the state and all of the ViewModel. They also hold references to each other
(`pushCoordinator.initialize(routing = routingCoordinator)`), and the order they start in is hidden in
`init`. The split is in name only, and the coupling stays where it was. Rebuilt as slices, each coordinator
owns a part of the root state, its outputs go to the root logic, and the root logic routes between them.

### Component ViewModels in a list

`rememberItemStores(keys)` and `ItemScope(stores, key)` give every list item a ViewModel store of its own
(Decompose's `childItems`). An item's ViewModels live as long as the item is in the list, not as long as it
is on screen:

- Scrolling an item away and back keeps its state.
- A refresh that drops an item clears its ViewModels.
- While an item is off screen nothing collects its state, so its `whileSubscribed` work pauses. The feed's
  live like count ticks only for the cards on screen.

This replaces both earlier choices. Keeping item ViewModels in the screen's store kept every one until the
screen closed, and a store remembered by the item lost its state on every scroll.

## Work that only matters on screen

`whileSubscribed { }` and `observeWhileSubscribed(flow) { }` (Orbit's `repeatOnSubscription`) run while
someone collects the store's state, and stop a timeout after the last collector leaves. `collectAsState` is
lifecycle-aware, so "someone collects" means the screen is visible. Use them for live counts, a location, or
a socket. Use plain `observe` for what must stay current even off screen: a session, or settings.

## Surviving process death

`SavedStateHandle.keeper(key, serializer, save, restore)` (Decompose's `StateKeeper`, on Jetpack's handle and
kotlinx-serialization) keeps a part of the state. The store starts from the saved part and saves it again
whenever it changes:

- **Keep what the user would be upset to lose:** a draft, a selection, a step.
- **Never keep loaded data:** it is fetched again.
- **Never keep a busy phase.** The work behind it died with the process. The create-post sample keeps the
  draft, and a restored screen starts in `Idle`, not in `Uploading`.
- A saved value that no longer decodes, after an app update, is dropped.

## Testing

`storeTest(initial, logic) { }` (from `mvi-test`, after Orbit's test DSL) records every change of state and
every effect, in order, and the test takes them one at a time:

```kotlin
storeTest(LikeState(isLiked = false, count = 7), LikeLogic(FAILING_POST, posts)) {
    send(LikeIntent.Toggle)
    expectState { copy(isLiked = true, count = 8) }
    advance(1_000)
    expectState { copy(isLiked = false, count = 7) }
    expectEffect(LikeEffect.Failed)
}
```

- A state is asserted as a change from the one before, so the test reads like the reducer it checks.
- Nothing may be left at the end: a change the test did not expect fails it, and so does a failure the
  store reported.
- Time is the test's: `advance(millis)` moves it.
- `subscribe()` stands for a visible screen.
- `storeTest(start = false)` checks the state before `onStart` runs.

For behaviour shared by several implementations, assert on the mapped store instead, as
`SignInBehaviourTest` does.

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

`dev.klitsie.statemachine` 0.4.1 is one way to write level 3, behind `MachineLogic`, so screens do not
depend on it directly. Lessons from building with it:

- **Never use its default `SharingStarted.WhileSubscribed(5s)`.** Five seconds after the screen leaves the
  foreground, it cancels every side effect, such as an upload or a countdown. `machineLogic` runs it
  eagerly on the host's scope and writes every state into the host's state, which is also what lets it run
  as a slice.
- **A `sideEffect` restarts whenever its key changes, and the default key is the whole state.** A phase that
  updates its own data (an upload's progress, a countdown) has to key its work on what identifies it:
  `key = { it.draft }` or `key = { Unit }`. Otherwise the work restarts on every update.
- **Phases are immutable sealed classes.** They fit the immutable-state rule as they are.
- **The library is young and has one maintainer.** That is why it sits behind our own type, and why level 3
  also has a plain form. Its compiler plugin (which checks for missing or duplicate states) needs Kotlin 2.4
  and is not tried yet.

## What we took from Orbit and Decompose

Neither library is adopted whole. Orbit's intents run with no concurrency rule, which is weaker than
`ActionLogic`'s policies. Decompose replaces navigation and the ViewModel together. Their good ideas fit the
runtime as they are:

| Idea | From | Here |
|---|---|---|
| Work bound to a phase, cancelled on leaving it | Orbit `runOn` | `whileIn`, `runOn`: level 3 without a library |
| Internal and external state | Orbit `transformState` | `mapState` |
| One effect collector, effects cached while none collects | Orbit `SideEffectMode` | `Store.effects` |
| Start on first use | Orbit lazy `onCreate` | `Start.Lazily` |
| Work only while subscribed | Orbit `repeatOnSubscription` | `whileSubscribed`, `observeWhileSubscribed` |
| Test DSL | orbit-test | `mvi-test`: `storeTest { }` |
| Store settings: error handler | Orbit `RealSettings` | `StoreSettings`, plus an observer that Orbit does not have |
| A child talks only to its parent | Decompose components | Slice and slot outputs (`onEffect`) |
| Child lifecycle | Decompose `childContext` | A scope per slice; `detach()` |
| Children that come and go | Decompose `childSlot` | `Slot` |
| Per-item lifecycle | Decompose `childItems` | `ItemStores`, `ItemScope` |
| State that survives process death | Essenty `StateKeeper` | `StateKeeper` on `SavedStateHandle` |

Left out on purpose:

- **Decompose's `JetpackComponentContext`.** We are not hosting Decompose components.
- **`childStack` and Decompose's navigation.** Navigation stays with Navigation3 and its entries' ViewModels,
  whose back stack is already state.
- **Orbit's container and intent model.** `Logic` with policies already covers them.

## Back to arch-kit

Everything here maps onto arch-kit. The question is how far to take it:

- `Logic` + `StoreScope` replace "the ViewModel implements the plugins". Level 1 is arch-kit as it is today:
  `setState { copy() }` has the same name on purpose.
- `ActionLogic`/`Policy`, phases, `Slice`, `Slot`, `MachineLogic`, `StateKeeper`, item stores and the test
  DSL are new, and they are additive.
- `ContentState` is arch-kit's, with a `Throwable` in place of `Failure`.
- What is not here: the plugin registry, `UiString`, forms, `EventBus`, and DI factories. They stay as they
  are, and a `Logic` uses them through its constructor.

## Open questions

- Can the "lines" be Detekt rules? Counting flags and guards, the size of a `Logic`, and slice-to-slice
  references all look checkable.
- Should `Start.Lazily` stay the default once real screens use it? A screen that posts an intent from its
  ViewModel's constructor starts the logic early, which is harmless; nothing has needed `Eagerly` yet.
- In the playground, a sample's saved state is bridged into the composition by hand (`SamplesApp`). In an
  app, the Navigation3 entry's `SavedStateHandle` is passed instead; that wiring is not tried yet.
