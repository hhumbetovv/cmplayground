# State architecture

Compose state management built so that a write reaches only the composables that read it — and so
that the choice of state holder library stays replaceable.

## The model

```
state holder      →   XxxScreen       →   XxxScreenContent   →   widgets
(ViewModel that       hands the           hands each             read .value
 owns the state)      state down          component its state    where they use it
```

State is a **stable object with fields**, not an immutable value that gets replaced:

```kotlin
@Stable
class TasksState : UiState() {
    val query = field("")
    val isLoading = field(true)
}
```

Each `field` is its own `MutableState`. A write invalidates exactly the composables that read that
field — no state copy, no equality check standing between the write and its readers, no component
recomposing because something unrelated changed in the same object.

Four rules:

1. **One state class per screen**, extending `UiState`, annotated `@Stable`. Fields hold immutable
   values; the field is the mutable part, never its contents.
2. **Only the owner writes.** `Field` exposes `State`; writing needs a `StateWriter`, and the only
   way to get one is inside a ViewModel's `reduceState { }` block.
3. **Nothing derived is stored.** Derived values are `derived { }`, so they cannot go stale.
4. **Components take a component state, never the screen state**, and read `.value` at the point of
   use.

### Naming

`XxxScreen` is the wiring — holder, effects, actions. `XxxScreenContent` is the pure UI that takes
the state object + actions and nothing else. Only `XxxScreenContent` is previewable, and only
`XxxScreen` imports the architecture.

### Component states

A component's input is a bundle of `State` references, built once when the screen state is
constructed:

```kotlin
@Stable
data class TasksSearchFieldState(
    val query: State<String>,
    val isClearVisible: State<Boolean>,
) : ComponentState

// in TasksState
val searchField = TasksSearchFieldState(
    query = query,
    isClearVisible = derived { query.value.isNotEmpty() },
)
```

The object is never replaced, so a component's parameters never change and it can never be
recomposed by its caller. It recomposes only in the scope where a `.value` is read — which is why
`isClearVisible` is read inside the trailing icon's lambda and not in the field's body: the icon
appears and disappears without touching the text field.

`@Stable` and not `@Immutable`: the object is constant, the values it points at are not.

The screen state has three sections in declaration order — fields, derivations, component states —
and the last is built from the first two, so nothing needs `remember` at the call site.

### Writing

A screen ViewModel declares nothing for state ownership — `ViewModel` and `ContainerHost`, no base
class, no marker interface:

```kotlin
class TasksViewModel : ViewModel(), ContainerHost<TasksState, TasksEffect> {
    val state = TasksState()

    private fun changeQuery(query: String) = intent {
        reduceState { state.query.set(query) }
    }
}
```

`reduceState` is a `ViewModel` extension that puts a `StateWriter` in scope for its block and applies
everything inside as one snapshot:

```kotlin
reduceState {
    state.isLoading.set(false)
    state.canLoadMore.set(page.hasMore)
    state.errorMessage.set(null)
}
```

`set` and `update` are members of `StateWriter`, so they exist only where `reduceState` put one in scope.
The type is `sealed` with a private implementation, which closes the three ways around it — the
compiler rejects all of them:

```kotlin
viewModel.state.query.set("x")     // unresolved reference: no receiver in scope
StateWriter()                      // cannot construct: no accessible constructor
object My : StateWriter { ... }    // cannot implement: sealed, declared in another package
```

Writes that a holder makes to its **own** fields use the matching `protected` helpers on `UiState`
instead — that is the UI-owned case, `TaskEditorUiState`.

Helper functions that write several fields are declared as `StateWriter` extensions inside the
ViewModel, so they are callable only from a `reduceState` block and can still see the ViewModel's own
`state`:

```kotlin
private fun StateWriter.putTask(id: String, transform: TaskItemState.() -> TaskItemState) { ... }

reduceState { putTask(id) { copy(isBusy = true) } }
```

Atomicity being the default matters more than under a reducer. A reducer publishes one new object;
separate field writes are separate notifications, so a composition could otherwise observe the new
list one frame before the loading flag clears. It also makes writes from a background thread —
Orbit reduces on its own event loop — land together.

### Lists are normalised

A list in state is an id list plus a lookup, never a `List<Item>`:

```kotlin
val taskIds = field<ImmutableList<String>>(persistentListOf())
val tasksById = field<ImmutableMap<String, TaskItemState>>(persistentMapOf())
val visibleTaskIds = field<ImmutableList<String>>(persistentListOf())
```

A `List<Item>` cannot separate the two kinds of change: editing one item produces a new list, so
the list component sees a changed input and recomposes even though its structure did not move.
With the split, an edit rewrites one map entry, both id lists keep their value, and the change
reaches exactly one row:

- the list component reads `ids` only, so it recomposes when items are added, removed, reordered or
  filtered;
- each row derives its own entry by id and keeps it across recompositions:

```kotlin
items(items = ids, key = { it }) { id ->
    TaskRow(state = remember(id) { state.tasksById.derive { this[id] } }, ...)
}
```

Rows therefore take `State<Item?>` — nullable because a removed row's group can outlive its entry
by a frame. Filtering follows the same rule: it produces ids, not a second copy of the items.

### Who owns which state

| Question | Answer |
| --- | --- |
| Does it survive process death, or matter to the repository? | screen state, owned by the ViewModel |
| Does the holder need it to decide anything? | screen state |
| Is it "is this section expanded", "which tab is open"? | a `UiState` owned by the composable |
| Is it a text field's in-flight value? | `TextInputState`, committed to the holder |

The machinery is the same in both cases — a `UiState` and whoever is allowed to write it — only
the owner differs.
`TaskEditorUiState` in `TaskEditorSheet.kt` is the UI-owned example: whether the note section is
unfolded never reaches the ViewModel.

`TextInputState` (`core/state/TextInputState.kt`) exists because a text field driven straight from
the holder is only as smooth as the holder's turnaround, and Orbit's own answer for that —
`blockingIntent` — is not available in common code: it lives in Orbit's `jvmAndNative` source set.
The keystroke is applied locally and committed to the holder; a value the holder produces on its
own (a clear button, a reset) still wins, while echoes of our own commits are ignored so a lagging
holder can never rewind what is being typed.

### Effects

One-shot events never live in state. Orbit hosts use `postSideEffect` and are collected with
Orbit's own `collectSideEffect`; a non-Orbit holder uses a buffered `Channel` and is collected with
`collectUiEffects` (`core/mvi/EffectCollector.kt`), which is the `LaunchedEffect` +
`repeatOnLifecycle` body Orbit already has and a plain `Flow` does not.

A dialog is **not** an effect — it is a field on the screen state, so it survives rotation.

### Orbit

Orbit keeps everything it is good at: intents are serialised on its event loop, `intent { }` gives
each one a coroutine scope tied to the container, effects go through `postSideEffect`, and
`collectSideEffect` is called directly with no wrapper. The container is left at its defaults.

What is gone is `reduce`. The container's state is the field holder and its identity never changes,
so there is nothing to reduce into and nothing to collect: the UI reads fields, and the snapshot
system delivers each write. `Syntax.state` still returns the holder, so intents read like
`state.query.set(query)`.

Pinned to **11.0.0**: `ContainerHost<STATE, SIDE_EFFECT>` and `ViewModel.container(...)`. Orbit 12
renames those to `OrbitContainerHost<INTERNAL, EXTERNAL, SIDE_EFFECT>` and `orbitContainer(...)`,
deprecating the old names — upgrading is two lines in `TasksViewModel` and one import in
`TasksScreen`, because nothing else imports Orbit.

### What this costs

Worth stating plainly, because the trade is real:

- there is no single immutable snapshot of the screen, so no free state restoration, no time-travel
  debugging, and no "assert the whole state" test — tests assert fields;
- skipping no longer protects a careless call site. Under an immutable state class, reading too
  high still ended in an equality check; here, a `.value` read high in the tree recomposes
  everything below it. The rule "read at the point of use" is load-bearing, not stylistic;
- `@Stable` on the state classes is a promise the compiler cannot verify — it holds because every
  field is a `MutableState`;
- every write is wrapped in `reduceState { }`, which is a few characters of ceremony on single-field
  writes. Kotlin 2.2's context parameters would remove it — `context(_: ViewModel)` on `set` gives
  the same scoping without the block — but that is not available on Kotlin 2.1.

## Plugging in another architecture

The state class and the components know nothing about the holder, so the architecture-specific
surface is a single composable, the screen function. To add a holder:

1. own a `UiState` and write it from `reduceState { }` (a `ViewModel` extension — an equivalent on
   `ContainerHost` would scope it to Orbit hosts);
2. expose effects as a `Flow<E>` and collect them with `collectUiEffects` (or the library's own
   collector, as the Orbit screen does);
3. build the actions bag from whatever the holder exposes — intents, methods, callbacks.

Two are already in the tree and share every component below `XxxScreen`:

| | `feature/tasks` | `feature/taskdetail` |
| --- | --- | --- |
| holder | Orbit `ContainerHost` | plain `ViewModel` |
| input | `TasksIntent` sealed interface | public methods |
| effects | `postSideEffect` | buffered `Channel` |
| collected with | `collectSideEffect` | `collectUiEffects` |
| state | `UiState` + `reduceState { }` | identical |
| view layer | identical contract | identical contract |

## Case coverage

| Case | Where |
| --- | --- |
| loading / error / empty / content phases | `TasksScreenContent.TasksBodySection` |
| error with retry, and a switch to trigger it | `TasksErrorView`, `TasksIntent.SimulateFailure` |
| search input without holder lag | `TasksSearchField` + `TextInputState` |
| heavy work off the main thread, stale results dropped | `TasksViewModel.applyFilters` |
| multi-select filters, per-chip derivation | `TasksFilterRow` |
| derived counters, never stored | `TasksState.summary` |
| list that skips when an item changes | `TaskListState`, `TaskList`, `TaskRow` |
| per-item async with a per-item busy flag | `TasksViewModel.putTask` |
| pagination without recomposing on scroll | `TaskList` (`snapshotFlow`) |
| atomic multi-field write | `TasksViewModel.loadPage` (`reduceState { }`) |
| form in a modal sheet, validation, saving state | `TaskEditorSheet` |
| UI-owned state next to holder-owned state | `TaskEditorUiState` |
| confirm dialog held as state | `TaskDeleteDialog` |
| effects: navigation and messages | `TasksEffect`, `TasksScreen` |
| previews with hand-set fields | `preview { }`, `TasksScreenContentPreview` |
| second architecture, same UI contract | `feature/taskdetail` |

## Verifying the claim

Turn on **Recompositions** in the app bar: every tracked component draws a border when it
recomposes, cycling blue → green → amber → red as the count grows
(`core/debug/RecompositionHighlighter.kt`). Type in the search field and the filter row, the
summary bar and the untouched rows stay quiet; tick one task and only that row and the counters
move.

Compose compiler reports are written to `composeApp/build/compose_reports` on every build — check
there that composables are `skippable` and the state classes `stable`.

## Conventions

- Component state classes are `@Stable` data classes of `State` references, declared next to their
  component; the screen state builds them.
- Components take plain callbacks, never intents. Callback names describe the local event
  (`onTaskClick`), not the consequence (`onNavigateToDetail`).
- Actions bags default every field to a no-op, which is what makes them valid preview arguments,
  and are built once with `remember`.
- No magic numbers in composables: named `private val` dimensions per file.
