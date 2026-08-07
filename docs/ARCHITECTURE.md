# State architecture

Compose state management built so that a state change recomposes only the component that state
belongs to — and so that the choice of state holder library stays replaceable.

## The model

```
state holder         →   XxxScreen        →   XxxScreenContent   →   widgets
(Orbit / StateFlow /     collects into        map / read             component state classes
 presenter / preview)    State<S>, no read    per component
```

Four rules:

1. **One immutable state class per screen**, reduced by the holder. Immutable collections only, no
   `MutableState` inside, nothing derived stored in it.
2. **One projection function per component** on that class, returning a component state class that
   lives next to its component. Derived values are computed here, never stored.
3. **The UI contract is `State<S>` plus a callbacks bag.** Nothing below `XxxScreen` knows the
   architecture, the intent type, or the holder.
4. **`map` to pass down, `read` to consume.** A composable that only forwards state must not read
   it.

### Naming

`XxxScreen` is the wiring — holder, state collection, effects, actions. `XxxScreenContent` is the
pure UI that takes `State<S>` + actions and nothing else. Only `XxxScreenContent` is previewable,
and only `XxxScreen` imports the architecture.

### Projections — `core/state/StateProjection.kt`

| Call | Reads here? | Use |
| --- | --- | --- |
| `state.map { slice() }` | no | pass a narrowed `State` to a child |
| `state.read { slice() }` | yes | consume a slice in this composable |
| `state.read()` | yes | consume a holder that is already narrow |
| `state.derive { slice() }` | n/a | derivation outside composition |

Two implementation notes:

- the selector is tracked with `rememberUpdatedState`, so a selector that captures something (a
  loop variable, a row id) re-derives instead of going stale — no `key(...)` wrapper needed at the
  call site. A non-capturing selector is a compiler singleton, so the common case costs nothing;
- `structuralEqualityPolicy()` is explicit, because a projection rebuilds its component state on
  every derivation and `equals` is what has to decide whether the child recomposes.

State is **not** read in the screen function:

```kotlin
val uiState by viewModel.collectAsState()   // reads state — the whole screen recomposes
val state = viewModel.collectAsState()      // does not read — the screen composes once
```

The `by` makes every projection below it pointless: the scope they feed has already been
invalidated by the time they run.

### Lists are normalised

A list in state is an id list plus a lookup, never a `List<Item>`:

```kotlin
val taskIds: ImmutableList<String>                  // order
val tasksById: ImmutableMap<String, TaskItemState>  // data
val visibleTaskIds: ImmutableList<String>           // filtered order
```

The reason is that a `List<Item>` cannot separate the two kinds of change. Editing one item
produces a new list, so the list component sees a changed input and recomposes even though its
structure did not move. With the split, an edit rewrites one map entry, both id lists keep their
identity, and the change reaches exactly one row:

- the list component reads `ids` only, so it recomposes when items are added, removed, reordered
  or filtered;
- each row derives its own entry by id (`state.map { tasksById[id] }`) inside its own composition
  scope, so it recomposes when that entry changes.

Rows therefore take `State<Item?>`, not `Item` — nullable because a removed row's group can outlive
its entry by a frame.

Filtering follows the same rule: it produces ids, not a second copy of the items.

### Reduced state vs. UI-owned state

| Question | Answer |
| --- | --- |
| Does it survive process death, or matter to the repository? | reduced state |
| Does the reducer need it to decide anything? | reduced state |
| Is it "is this section expanded", "which tab is open"? | a `mutableStateOf` in the composable |
| Is it a text field's in-flight value? | `TextInputState` locally, committed to the holder |

State the UI owns needs no framework, and none is provided:

```kotlin
@Stable
class TaskEditorUiState {
    var isNoteExpanded by mutableStateOf(false)
        private set

    fun toggleNote() { isNoteExpanded = !isNoteExpanded }
}
```

`private set` is the whole ownership story. `TaskEditorUiState` in `TaskEditorSheet.kt` is the
worked example: the note section's expansion never reaches the reducer.

`TextInputState` (`core/state/TextInputState.kt`) exists because a text field driven straight from
reduced state is only as smooth as the holder's turnaround, and Orbit's own answer for that —
`blockingIntent` — is not available in common code: it lives in Orbit's `jvmAndNative` source set.
The keystroke is applied locally and committed to the holder; state the holder produces on its own
(a clear button, a reset) still wins, while echoes of our own commits are ignored so a lagging
holder can never rewind what is being typed.

### Effects

One-shot events never live in state. Orbit hosts use `postSideEffect` and are collected with
Orbit's own `collectSideEffect`; a non-Orbit holder uses a buffered `Channel` and is collected with
`collectUiEffects` (`core/mvi/EffectCollector.kt`), which is the `LaunchedEffect` +
`repeatOnLifecycle` body Orbit already has and a plain `Flow` does not.

A dialog is **not** an effect — it is `deleteTarget` in the state class, so it survives rotation.

### Orbit

`TasksScreen` calls `collectAsState()` and `collectSideEffect()` directly; there is no wrapper over
Orbit anywhere. The container is left at its defaults — initial state plus `onCreate`.

Pinned to **11.0.0**: `ContainerHost<STATE, SIDE_EFFECT>` and `ViewModel.container(...)`. Orbit 12
renames those to `OrbitContainerHost<INTERNAL, EXTERNAL, SIDE_EFFECT>` and `orbitContainer(...)`,
deprecating the old names — upgrading is two lines in `TasksViewModel` and two imports in
`TasksScreen`, because nothing else imports Orbit.

## Plugging in another architecture

The architecture-specific surface is a single composable, the screen function. To add a holder:

1. produce a `State<S>` with whatever that architecture already ships —
   `collectAsStateWithLifecycle()` for a `StateFlow`, `collectAsState()` for an Orbit host,
   `asUiState()` for a presenter that returns the state per composition — and do not read it;
2. expose effects as a `Flow<E>` and collect them with `collectUiEffects` (or the library's own
   collector, as the Orbit screen does);
3. build the actions bag from whatever the holder exposes — intents, methods, callbacks.

Two are already in the tree and share every component below `XxxScreen`:

| | `feature/tasks` | `feature/taskdetail` |
| --- | --- | --- |
| holder | Orbit `ContainerHost` | `ViewModel` + `MutableStateFlow` |
| input | `TasksIntent` sealed interface | public methods |
| effects | `postSideEffect` | buffered `Channel` |
| collected with | `collectAsState` / `collectSideEffect` | `collectAsStateWithLifecycle` / `collectUiEffects` |
| view layer | identical contract | identical contract |

## Case coverage

| Case | Where |
| --- | --- |
| loading / error / empty / content phases | `TasksScreenContent.TasksBodySection` |
| error with retry, and a switch to trigger it | `TasksErrorView`, `TasksIntent.SimulateFailure` |
| search input without reducer lag | `TasksSearchField` + `TextInputState` |
| heavy work off the main thread, stale results dropped | `TasksViewModel.applyFilters` |
| multi-select filters, per-chip narrowing | `TasksFilterRow` |
| derived counters, never stored | `TasksState.summaryState` |
| list that skips when an item changes | `TaskListState`, `TaskList`, `TaskRow` |
| per-item async with a per-item busy flag | `TasksViewModel.updateTask` |
| pagination without recomposing on scroll | `TaskList` (`snapshotFlow`) |
| form in a modal sheet, validation, saving state | `TaskEditorSheet` |
| UI-owned state next to reduced state | `TaskEditorUiState` |
| confirm dialog held as state | `TaskDeleteDialog` |
| effects: navigation and messages | `TasksEffect`, `TasksScreen` |
| previews without a holder | `TasksScreenContentPreview`, `TaskDetailScreenContentPreview` |
| second architecture, same UI contract | `feature/taskdetail` |

## Verifying the claim

Turn on **Recompositions** in the app bar: every tracked component draws a border when it
recomposes, cycling blue → green → amber → red as the count grows
(`core/debug/RecompositionHighlighter.kt`). Type in the search field and the filter row, the
summary bar and the untouched rows stay quiet.

Compose compiler reports are written to `composeApp/build/compose_reports` on every build — check
there that composables are `skippable` and state classes `stable` before trusting a projection.

## Conventions

- Component state classes are `@Immutable` data classes declared next to their component; the
  screen state only knows how to fill them.
- Components take plain callbacks, never intents. Callback names describe the local event
  (`onTaskClick`), not the consequence (`onNavigateToDetail`).
- Actions bags default every field to a no-op, which is what makes them valid preview arguments,
  and are built once with `remember` so passing `actions.onX` down never breaks skipping.
- No magic numbers in composables: named `private val` dimensions per file.
