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
2. **Only the owner writes.** `Field` exposes `State`; the `set` functions take a `ViewModel`
   context parameter, so they do not resolve anywhere else.
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
class, no marker interface, no writer type:

```kotlin
class TasksViewModel : ViewModel(), ContainerHost<TasksState, TasksEffect> {

    val state: TasksState get() = container.stateFlow.value

    private fun changeQuery(query: String) = intent {
        reduce {
            state.query.set(query)
            state
        }
    }
}
```

Writes go through Orbit's own `reduce`, so they are serialised on its event loop like any other
reduction. The holder's identity never changes, so the block hands the same `state` back — the
reduction is an identity one, and what actually changed is the field.

`set` is a context-parameter extension:

```kotlin
context(_: ViewModel)
fun <T> UiState.Field<T>.set(value: T) = write(value)

context(_: ViewModel)
fun <T> UiState.Field<T>.set(producer: T.() -> T) = write(value.producer())
```

It resolves inside a ViewModel and nowhere else — a composable holding the screen state can read
every field and call none of these. That is the whole enforcement; there is no wrapper type and no
block to opt into.

For several fields at once, `set { }` on the holder batches them into one snapshot and returns the
holder, which is exactly what `reduce` wants back:

```kotlin
reduce {
    state.set {
        isLoading.set(false)
        canLoadMore.set(page.hasMore)
        errorMessage.set(null)
    }
}
```

Without it each write is its own notification, and a composition can observe the new list one frame
before the loading flag clears. Orbit's event loop serialises reductions against each other; it does
not make the writes inside one reduction atomic against composition.

**How strong is the guarantee.** A context parameter is satisfied by any implicit receiver of that
type, so `with(viewModel) { state.query.set("x") }` compiles from anywhere — verified against the
compiler. It is the same strength a marker interface gave, with the machinery deleted rather than
made airtight. Tightening it means a context type a composable cannot obtain (Orbit's `Syntax`, say),
at the cost of shutting out the non-Orbit holder.

State a holder owns itself — `TaskEditorUiState` — uses the matching `protected` helpers on
`UiState` instead, and needs no context at all.

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

Orbit is used exactly as it ships. `TasksScreen` calls `collectAsState()` and `collectSideEffect()`
directly; intents are serialised on the event loop; effects go through `postSideEffect`; the
container is left at its defaults and is the state's home.

`reduce` is Orbit's own, and it is where every write happens. The only unusual thing is what the
state is: a holder of fields rather than a value, so the reduction returns the same instance it was
given. Nothing collects the container's `stateFlow` — the UI reads fields, and the snapshot system
delivers each write.

Pinned to **11.0.0**: `ContainerHost<STATE, SIDE_EFFECT>` and `ViewModel.container(...)`. Orbit 12
renames those to `OrbitContainerHost<INTERNAL, EXTERNAL, SIDE_EFFECT>` and `orbitContainer(...)`,
deprecating the old names — upgrading is two lines in `TasksViewModel` and two imports in
`TasksScreen`, because nothing else imports Orbit.

### Toolchain

Kotlin **2.4.10**, Compose Multiplatform **1.11.1**. Context parameters are stable in 2.4, so the
`-Xcontext-parameters` flag 2.3 needed is gone and the build sets no compiler options at all.

`iosX64` (the Intel simulator) is no longer a target: Compose Multiplatform stopped publishing for
it.

### What this costs

Worth stating plainly, because the trade is real:

- there is no single immutable snapshot of the screen, so no free state restoration, no time-travel
  debugging, and no "assert the whole state" test — tests assert fields;
- skipping no longer protects a careless call site. Under an immutable state class, reading too
  high still ended in an equality check; here, a `.value` read high in the tree recomposes
  everything below it. The rule "read at the point of use" is load-bearing, not stylistic;
- `@Stable` on the state classes is a promise the compiler cannot verify — it holds because every
  field is a `MutableState`;
- a write must sit inside `reduce { }` and the block has to hand the holder back, so a single-field
  write is three lines rather than one;
- the write guarantee is scoping, not sealing: see **How strong is the guarantee** above.

## Plugging in another architecture

The state class and the components know nothing about the holder, so the architecture-specific
surface is a single composable, the screen function. To add a holder:

1. own a `UiState` and write it with `set` from inside the ViewModel — through `reduce { }` if the
   holder has one, directly otherwise;
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
| state | `UiState` + `set` in `reduce { }` | `UiState` + `set` directly |
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
| atomic multi-field write | `TasksViewModel.loadPage` (`state.set { }`) |
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
