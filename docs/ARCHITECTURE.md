# State architecture

Compose state management built so that a change recomposes only the component that change belongs
to — and so that the choice of state holder library stays replaceable.

## The model

```
state holder      →   XxxScreen       →   XxxScreenContent   →   widgets
(reduces one          builds the          hands each             read at the
 immutable value)     component states    component its state    point of use
```

Four rules:

1. **One immutable state class per screen**, reduced by the holder. Immutable collections only, no
   Compose types inside, nothing derived stored in it.
2. **One component state per component**, built once in `rememberXxxScreenState` and never replaced.
3. **The state is never read in `Screen` or `Content`** — reading it there subscribes the whole
   screen to every change and makes everything below it pointless.
4. **Derivations are chained, not flat** — see [Two layers](#two-layers).

### Naming

`XxxScreen` is the wiring — holder, effects, actions. `XxxScreenContent` is the pure UI that takes
the screen state bag + actions and nothing else. Only `XxxScreenContent` is previewable, and only
`XxxScreen` imports the architecture.

## Component states — two shapes

A component state is either a bag of values or a bag of `State`, never a mix:

```kotlin
// values — passed as State<X>, read whole, compared as a unit
@Immutable
data class TasksErrorViewState(val message: String) : ComponentState

// State fields — passed as X, built once, read at each point of use
@Stable
data class TasksSearchFieldState(
    val query: State<String>,
    val isClearVisible: State<Boolean>,
) : ComponentState
```

**Values** when the component is small and its fields change together. It stays an ordinary value: a
test can build and compare it without Compose, it prints, and equality still decides whether the
component recomposes.

**`State` fields** when the fields change at different times, when the component has inner scopes —
trailing lambdas, lazy items, slots — that can usefully recompose alone, or when a projection behind
a field is expensive. The object is then constant, so the component's parameters never change and
its caller can never recompose it; recomposition happens in whichever scope read the field.

`@Stable`, not `@Immutable`, for the second shape: the object is constant, the values behind it are
not.

On this screen:

| component | shape | why |
| --- | --- | --- |
| `TasksSearchField` | `State` | `query` moves per keystroke, `isClearVisible` almost never |
| `TasksFilterRow` | `State` | chips derive their own boolean; the row must not recompose |
| `TasksSummaryBar` | `State` | counters move independently; `doneCount` is the expensive one |
| `TaskList` | `State` | `ids` and `tasksById` must move separately — see [Lists](#lists-are-normalised) |
| `TaskRow` | value | a row's fields change together, and it is already the narrowest slice |
| `TasksErrorView`, `TasksEmptyView` | value | one field each |
| `TaskEditorSheet`, `TaskDeleteDialog` | value | a form created, filled and discarded as a unit |
| `TaskDetailHeader` | `State` | ticking the task moves `isDone`, not the title |
| `TaskDetailNoteCard` | value | one field |

## Two layers

`rememberXxxScreenState` is the one place the screen state is taken apart, and its body has two
layers:

```kotlin
// layer 1 — one trivial selector per field
val tasksById = source.derive { tasksById }

// layer 2 — real work, chained off layer 1 and never off `source`
val doneCount = tasksById.derive { count { (_, task) -> task.isDone } }
```

A derivation reading another derivation is not recomputed while its input's value is unchanged, so
`doneCount` counts when the map changes and not when a keystroke changes the query. Chain it off
`source` instead and it runs on every change.

Measured in `DerivationChainTest`:

| | cheap selector | expensive derivation |
| --- | --- | --- |
| unrelated field changed, chained | ran | **did not run** |
| relevant field changed, chained | — | ran |
| unrelated field changed, reading the state directly | — | **ran, for nothing** |

Two things may read `source` directly: layer-1 selectors, and cheap derivations that genuinely
depend on several fields (`phase()`, `isFiltered`). Anything that filters, counts, sorts or
allocates belongs in layer 2, chained.

The entire core is one function:

```kotlin
fun <T, R> State<T>.derive(selector: T.() -> R): State<R> =
    derivedStateOf(structuralEqualityPolicy()) { selector(value) }
```

## Lists are normalised

A list in state is an id list plus a lookup, never a `List<Item>`:

```kotlin
val taskIds: ImmutableList<String>
val tasksById: ImmutableMap<String, TaskItemState>
val visibleTaskIds: ImmutableList<String>
```

A `List<Item>` cannot separate the two kinds of change: editing one item produces a new list, so the
list component sees a changed input even though its structure did not move. With the split, an edit
rewrites one map entry, both id lists keep their value, and the change reaches exactly one row:

```kotlin
items(items = state.ids.value, key = { it }) { id ->
    TaskRow(state = remember(id) { state.tasksById.derive { this[id] } }, …)
}
```

Rows take `State<TaskItemState?>` — nullable because a removed row's group can outlive its entry by
a frame. Filtering follows the same rule: it produces ids, not a second copy of the items.

## UI-owned state

No framework, and none needed:

```kotlin
@Stable
class TaskEditorUiState {
    var isNoteExpanded by mutableStateOf(false)
        private set

    fun toggleNote() { isNoteExpanded = !isNoteExpanded }
}
```

`private set` is the whole ownership story for state the UI owns. Screen state, by contrast, is
immutable and lives in the holder — a composable cannot write it because there is nothing to write.

`TextInputState` (`core/state/TextInputState.kt`) exists because a text field driven straight from
the holder is only as smooth as the holder's turnaround, and Orbit's own answer for that —
`blockingIntent` — is not available in common code: it lives in Orbit's `jvmAndNative` source set.
The keystroke is applied locally and committed to the holder; a value the holder produces on its own
(a clear button, a reset) still wins, while echoes of our own commits are ignored so a lagging
holder can never rewind what is being typed.

## Effects

One-shot events never live in state. Orbit hosts use `postSideEffect` and are collected with Orbit's
own `collectSideEffect`; a non-Orbit holder uses a buffered `Channel` and is collected with
`collectUiEffects` (`core/mvi/EffectCollector.kt`), which is the `LaunchedEffect` +
`repeatOnLifecycle` body Orbit already has and a plain `Flow` does not.

A dialog is **not** an effect — it is a field on the screen state, so it survives rotation.

## Orbit

`TasksScreen` calls `collectAsState()` and `collectSideEffect()` directly; there is no wrapper over
Orbit anywhere. The container is left at its defaults, and the ViewModel reduces the ordinary way:

```kotlin
private fun changeQuery(query: String) = intent {
    reduce { state.copy(query = query) }
}
```

The state it produces is never read in the screen:

```kotlin
val state = rememberTasksScreenState(viewModel.collectAsState())   // not read here, or inside
```

Pinned to **11.0.0**: `ContainerHost<STATE, SIDE_EFFECT>` and `ViewModel.container(...)`. Orbit 12
renames those to `OrbitContainerHost<INTERNAL, EXTERNAL, SIDE_EFFECT>` and `orbitContainer(...)`,
deprecating the old names — upgrading is two lines in `TasksViewModel` and two imports in
`TasksScreen`, because nothing else imports Orbit.

## Plugging in another architecture

The state class, the component states and every widget know nothing about the holder, so the
architecture-specific surface is a single composable, the screen function. To add a holder:

1. expose the screen state as a `State<S>` — `collectAsState()` for an Orbit host,
   `collectAsStateWithLifecycle()` for a `StateFlow` — and do not read it;
2. expose effects as a `Flow<E>` and collect them with `collectUiEffects`, or the library's own
   collector;
3. build the actions bag from whatever the holder exposes — intents, methods, callbacks.

Two are in the tree and share every component below `XxxScreen`:

| | `feature/tasks` | `feature/taskdetail` |
| --- | --- | --- |
| holder | Orbit `ContainerHost` | plain `ViewModel` + `MutableStateFlow` |
| input | `TasksIntent` sealed interface | public methods |
| effects | `postSideEffect` | buffered `Channel` |
| collected with | `collectAsState` / `collectSideEffect` | `collectAsStateWithLifecycle` / `collectUiEffects` |
| screen state | `rememberTasksScreenState` | `rememberTaskDetailScreenState` |
| view layer | identical contract | identical contract |

## Case coverage

| Case | Where |
| --- | --- |
| loading / error / empty / content phases | `TasksScreenContent.TasksBodySection` |
| error with retry, and a switch to trigger it | `TasksErrorView`, `TasksIntent.SimulateFailure` |
| search input without holder lag | `TasksSearchField` + `TextInputState` |
| heavy work off the main thread, stale results dropped | `TasksViewModel.applyFilters` |
| multi-select filters, per-chip derivation | `TasksFilterRow` |
| derived counters, never stored, never recomputed for nothing | `rememberTasksScreenState` |
| list that skips when an item changes | `TaskListState`, `TaskList`, `TaskRow` |
| per-item async with a per-item busy flag | `TasksViewModel.updateTask` |
| pagination without recomposing on scroll | `TaskList` (`snapshotFlow`) |
| form in a modal sheet, validation, saving state | `TaskEditorSheet` |
| UI-owned state next to holder-owned state | `TaskEditorUiState` |
| confirm dialog held as state | `TaskDeleteDialog` |
| effects: navigation and messages | `TasksEffect`, `TasksScreen` |
| previews without a holder | `TasksScreenContentPreview` |
| second architecture, same UI contract | `feature/taskdetail` |
| the chaining assumption, asserted | `DerivationChainTest` |

## Verifying the claim

Turn on **Recompositions** in the app bar: every tracked component draws a border when it
recomposes, cycling blue → green → amber → red as the count grows
(`core/debug/RecompositionHighlighter.kt`). Type in the search field and the filter row, the summary
bar and the untouched rows stay quiet; tick one task and only that row and the counters move.

Compose compiler reports are written to `composeApp/build/compose_reports` on every build — check
there that composables are `skippable` and the state classes `stable`.

## Where it can go wrong

- **Layer 2 reading `source`.** The one mistake that silently costs everything. Not a compile error,
  not visible in the compose report — only in the recomposition highlighter or a counter test.
- **`remember` placement.** Derivations must be created once; building them at a call site recreates
  them per recomposition and caches nothing.
- **Mixed component states.** A class with one plain field and one `State` field recomposes for the
  plain one and not the other, which nobody can predict from the call site.
- **Depth.** Each layer adds a validation hop per read. Two layers is the sweet spot.

## Conventions

- Component state classes are declared next to their component; `rememberXxxScreenState` builds them.
- Components take plain callbacks, never intents. Callback names describe the local event
  (`onTaskClick`), not the consequence (`onNavigateToDetail`).
- Actions bags default every field to a no-op, which is what makes them valid preview arguments, and
  are built once with `remember`.
- No magic numbers in composables: named `private val` dimensions per file.
