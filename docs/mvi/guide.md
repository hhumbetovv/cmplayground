# MVI guide

How to write a screen with this architecture, for a developer who sees it for the first time. Read it top to
bottom once. After that, sections 3, 4 and 9 are the ones you come back to.

The architecture has many parts: levels, policies, phases, slices, slots and item stores. **A new screen
needs almost none of them.** Every screen starts at level 1, which is a data class, a sealed interface and
one class with a `when`. The other parts exist for the day a screen outgrows that, and
[migrations](migrations.md) says when that day has come.

Contents:

1. [The model](#1-the-model)
2. [Your first screen](#2-your-first-screen)
3. [Choosing a level](#3-choosing-a-level)
4. [The rules](#4-the-rules)
5. [Screens that need fewer than three types](#5-screens-that-need-fewer-than-three-types)
6. [Keeping the generics quiet](#6-keeping-the-generics-quiet)
7. [Testing](#7-testing)
8. [Names and files](#8-names-and-files)
9. [Review checklist](#9-review-checklist)
10. [Common mistakes](#10-common-mistakes)

## 1. The model

```
              postIntent(intent)
  Screen ────────────────────────▶ Store ──▶ Logic.onIntent
    ▲                                            │
    │  collectAsState()                          ├── setState { copy(…) } ──▶ State ───┐
    └────────────────────────────────────────────┼─────────────────────────────────────┘
    ▲                                            │
    │  CollectEffects { }, each effect once      └── emit(effect) ──────────▶ Effects ─┐
    └──────────────────────────────────────────────────────────────────────────────────┘
```

A screen has three types and one class:

| Piece | What it is | Example |
|---|---|---|
| **State** | Everything the screen draws, as one immutable value | `data class ReportState(val reason: ReportReason?, val isSending: Boolean)` |
| **Intent** | What the user did, in the user's words | `ReportIntent.Send`, `ReportIntent.EditComment(text)` |
| **Effect** | Something to do once: navigate, show a message | `ReportEffect.Sent` |
| **Logic** | The behaviour: takes intents, changes the state, emits effects | `class ReportLogic : Logic<ReportState, ReportIntent, ReportEffect>` |

Around them, two pieces that hold no behaviour:

- **The Store** runs a logic. It holds the state, delivers the effects, and hands every intent to the logic
  in order. You never write one. You get it from `store(initial, logic)`.
- **The ViewModel** keeps the store alive across configuration changes. It is a `StoreOwner`, and its body is
  one line.

The words you will meet later, all of them optional at the start:

| Word | Meaning | First needed at |
|---|---|---|
| Partial | One named change of the state, with its reduction beside it | Level 2, or whenever logs need names |
| Action | What to do about an intent, with every parameter gathered | Level 2 |
| Policy | What happens when an action starts while the same one runs: Parallel, Latest, Drop, Queue | Level 2 |
| Phase | One of the exclusive modes a screen is in, as a sealed type | Level 3 |
| Slice | A part of a big screen with its own logic | Level 4 |
| Slot | A part that exists only while open: a dialog, a sheet | When a dialog has logic of its own |
| Component ViewModel | A part with its own ViewModel, such as a like button per card | When a part repeats, or appears on several screens |
| Keeper | Saves part of the state across process death | When losing the input would upset the user |

## 2. Your first screen

The report screen in `samples/report/` is a complete level 1 screen. Building it takes five files' worth of
code in three files.

```
report/
  ReportContract.kt   State, Intent, Effect
  ReportLogic.kt      the logic, and the ViewModel under it
  ReportScreen.kt     the screen: a route that takes the store, and stateless content
commonTest/…/report/
  ReportLogicTest.kt
```

### Step 1. The contract

```kotlin
data class ReportState(
    val reason: ReportReason? = null,
    val comment: String = "",
    val isSending: Boolean = false,
) : ViewState {
    val canSend: Boolean
        get() = reason != null && (reason != ReportReason.Other || comment.isNotBlank()) && !isSending
}

sealed interface ReportIntent : ViewIntent {
    data class SelectReason(val reason: ReportReason) : ReportIntent
    data class EditComment(val text: String) : ReportIntent
    data object Send : ReportIntent
}

sealed interface ReportEffect : ViewEffect {
    data object Sent : ReportEffect
    data class Failed(val message: String) : ReportEffect
}
```

- Every field has a default, so `ReportState()` is the initial state.
- `canSend` is computed from other fields, so it is a property and not a field. A field could disagree with
  the others.
- Intents carry the user's raw input (`text`, not `text.trim()`). The logic decides what to do with it.

### Step 2. The logic

```kotlin
class ReportLogic(
    private val postId: String,
    private val posts: PostRepository,
) : Logic<ReportState, ReportIntent, ReportEffect> {
    override fun StoreScope<ReportState, ReportEffect>.onIntent(intent: ReportIntent) {
        when (intent) {
            is ReportIntent.SelectReason -> setState { copy(reason = intent.reason) }
            is ReportIntent.EditComment -> setState { copy(comment = intent.text) }
            ReportIntent.Send -> send()
        }
    }

    private fun StoreScope<ReportState, ReportEffect>.send() {
        val reason = state.reason
        if (!state.canSend || reason == null) return
        val comment = state.comment.trim()              // gathered before anything suspends
        setState { copy(isSending = true) }
        launch {
            safeCall { posts.report(postId = postId, reason = reason, comment = comment) }
                .onSuccess { emit(ReportEffect.Sent) }
                .onFailure { error -> emit(ReportEffect.Failed(error.message.orEmpty())) }
            setState { copy(isSending = false) }
        }
    }
}
```

Inside `onIntent` you have the `StoreScope`, which provides:

- `state`: the state now;
- `setState { copy(…) }`: a change;
- `emit(effect)`: an effect;
- `launch { }`: work that dies with the store.

You also have `safeCall { }`, a `Result` that never swallows a cancellation. Dependencies come in through the
constructor. Nothing here knows about Android, Compose or the ViewModel.

### Step 3. The ViewModel

```kotlin
class ReportViewModel(
    postId: String,
    posts: PostRepository,
) : ViewModel(),
    StoreOwner<ReportState, ReportIntent, ReportEffect> {
    override val store = store(ReportState(), ReportLogic(postId, posts))
}
```

That is the whole class, and it stays this way. If you are about to add a function or a field to it, the
code belongs in the logic. `ViewModel()` can be your app's own base ViewModel instead: `StoreOwner` is an
interface.

### Step 4. The screen

```kotlin
@Composable
fun ReportScreen(
    store: Store<ReportState, ReportIntent, ReportEffect>,
    snackbar: SnackbarHostState,
    onDone: () -> Unit,
) {
    store.CollectEffects { effect ->
        when (effect) {
            ReportEffect.Sent -> onDone()
            is ReportEffect.Failed -> snackbar.showSnackbar(effect.message)
        }
    }
    ReportContent(state = store.collectAsState(), postIntent = store::postIntent)
}

@Composable
private fun ReportContent(state: State<ReportState>, postIntent: (ReportIntent) -> Unit) {
    val reason by state.select { it.reason }       // recomposes only when the reason changes
    // …
}
```

- Only this first composable, the route, sees the `Store`. Everything below it gets a `State` (narrowed with
  `select { }`) and a `postIntent` lambda, so it can be previewed and reused without a store.
- Effects are collected once, here, with `CollectEffects`.

### Step 5. Wiring it in

```kotlin
ReportScreen(viewModel { ReportViewModel(postId, posts) }.store, snackbar, onDone = navigator::back)
```

### Step 6. The test

```kotlin
private val posts = FakePostRepository(latency = 1_000)

@Test
fun aSentReportSaysSo() = runTest {
    storeTest(ReportState(), ReportLogic("p1", posts)) {
        send(ReportIntent.SelectReason(ReportReason.Spam))
        expectState { copy(reason = ReportReason.Spam) }
        send(ReportIntent.Send)
        expectState { copy(isSending = true) }
        advance(1_000)
        expectEffect(ReportEffect.Sent)
        expectState { copy(isSending = false) }
    }
}
```

The test needs no ViewModel and no Compose. It checks every change in order, and it fails if a change
happens that it did not expect. `ReportLogicTest` also covers the failure and the ignored send. More in
[Testing](#7-testing).

## 3. Choosing a level

**Start at level 1. Always.** Then answer these questions in order. The first "yes" says where the screen
goes. [Migrations](migrations.md) has the exact signals and the steps for each move.

| # | Question | Yes → |
|---|---|---|
| 1 | Can a second request start while the first is still running? Search as you type, a follow button per row, a double tap on submit. | **Level 2**: actions under policies |
| 2 | Is the screen in exactly one of several modes, so that three or more flags exclude each other? `isSubmitting`, `isVerifying`, `isLocked`, `step`. | **Level 3**: phases |
| 3 | Is the logic past about 300 lines, or do its intents fall into groups that never touch each other's state? | **Level 4**: slices |
| — | Otherwise | **Stay at level 1** |

Some questions are not about the level but about where a piece of logic is hosted. They can come up at any
level:

| Question | Yes → |
|---|---|
| Does a part exist only sometimes (a dialog, a sheet, an inline editor), and must its work stop when it closes? | **Slot** |
| Is a part drawn on several screens, or once per list item, and does it do its own IO? | **Component ViewModel** |
| Do two screens need the same logic, or does it coordinate several repositories? | **Use case** |
| Does a live source matter only while the screen is visible (a live count, a location)? | **`observeWhileSubscribed`** |
| Would the user be upset to lose what they typed when the app is killed in the background? | **Keeper** |

Levels are not a ranking. A level 1 screen is not unfinished, and a level 3 screen is not better. Each level
costs more code than the one below, and it pays for itself only past its line.

## 4. The rules

Each rule has a reason. A rule broken on purpose needs a comment that gives a reason as good.

### State

1. **One immutable state per store.** It is a `data class`, or at level 3 a sealed type of phases. Do not use
   `var` fields, mutable collections, or a `MutableStateFlow` per field. The state can then be compared in a
   test, saved, logged and diffed.
2. **Store facts, compute the rest.** `canSend` is a property. A stored copy of it could go stale.
3. **A request's result is a `ContentState`** (`Idle`, `Loading(previous)`, `Success`, `Error`), not three
   fields that can contradict each other.
4. **What a screen must still show after a rotation is state, not an effect.** An error banner is state; a
   one-off snackbar is an effect.

### Intents

5. **Named after what the user did:** `EditQuery`, `Send`, `ToggleFollow`. Not `StartSearch` or
   `CallApi`.
6. **They carry raw input.** Trimming, validating and deciding happen in the logic, never in an `onClick`.

### Effects

7. **Only for things that happen once:** navigation, a message, haptics.
8. **They are the logic's outputs, and someone handles every one.** The route collects them with
   `CollectEffects`. A child's effects go to its parent (see Children).
9. **One collector per store.** A second collector at the same time fails at once, so two screens cannot
   silently share effects.

### Logic

10. **Read the state before you suspend.** Copy what the work needs into locals, or into an action at
    level 2, before `launch`. After a suspension, `state` may already be different.
11. **IO goes through `safeCall { }`,** and each failure ends as a state or an effect. The store reports
    uncaught failures to `onError` and keeps running, but a failure the user never sees is a bug.
12. **Dependencies come through the constructor.** A logic must not use Android, Compose, `viewModelScope`
    or a service locator.
13. **Do not keep `Job`s.** A `Job?` field is the signal to move to level 2, where a policy does that work.

### ViewModel

14. **A ViewModel is `ViewModel(), StoreOwner<S, I, E>` with `override val store = store(…)` and nothing
    else.** It has no functions, no other state, and no `init` blocks that do work.

### Screen

15. **Only the route composable touches the `Store`.** Everything below it takes `State<…>` and a
    `postIntent` lambda.
16. **Each component reads its part with `select { }`,** so it recomposes only when its part changes. When a
    component needs a different shape, write a pure mapper (`toBodyUi()`) in the UI layer.

### Children (slices and slots)

17. **Children never reference each other.** When one needs something another has, the parent passes it in
    an intent's parameters.
18. **Each part of the state has one writer,** the child that owns it. The parent writes a child's part only
    to open or close a slot, or to reset everything.
19. **Rules that span children belong to the parent,** for example "nothing is editable while publishing".
20. **Attach children in the parent's `onStart`.**

## 5. Screens that need fewer than three types

The contract stays `Store<S, I, E>`. A channel the screen does not need gets the type `Nothing`. `Nothing`
has no values, so the compiler stops anyone from using that channel: the logic cannot `emit`, and the screen
cannot `postIntent`.

| The screen has | Write | Example |
|---|---|---|
| State and intents, no effects | `Logic<S, I, Nothing>` | A form slice, a toggle: `PollLogic`, `BodyLogic`. No `CollectEffects` on the screen. |
| State only | `Logic<S, Nothing, Nothing>` with `override fun StoreScope<S, Nothing>.onIntent(intent: Nothing) = Unit` | A read-only screen fed by `observe(flow)` in `onStart` |
| Effects only | `Logic<Unit, Nothing, E>`, with the work in `onStart` | A splash that picks a route |
| Nothing at all | No ViewModel | A static page |

A screen with effects and no state is rare. It almost always has a loading or error state you have not
written yet. If it really has none, the work probably belongs to the parent or to a use case, and not to a
screen.

When a missing channel is needed later, replace `Nothing` with the new type. The screen and the tests stay
as they are.

## 6. Keeping the generics quiet

You write the type arguments once per feature, and inference fills them in everywhere else:

- **ViewModel:** `override val store = store(ReportState(), ReportLogic(…))` infers the type. The type
  arguments appear once, in `StoreOwner<…>`.
- **Screen:** only the route takes `Store<S, I, E>`. The components below it take plain values and lambdas.
- **Tests:** `storeTest(initial, logic) { }` infers the types.
- **Logic:** the receiver `StoreScope<S, E>` repeats on every handler. When a file has many handlers, a
  file-private alias is allowed: `private typealias Scope = StoreScope<SignInPhase, SignInEffect>`, then
  `override fun Scope.onIntent(…)`.
- **A slot whose `onEffect` closes the slot itself** needs its type written out, because a property that
  refers to itself cannot be inferred (see `FeedLogic`). This is a known cost.

Never hide the types behind `Any`, star projections or casts. The type checks are what make a move between
levels safe.

## 7. Testing

Every logic gets a test in `commonTest`, written with `storeTest` from `mvi-test`:

```kotlin
storeTest(initial, logic) {
    send(intent)                         // as the screen would
    expectState { copy(field = value) }  // each change, as a change from the state before
    expectEffect(effect)
    advance(1_000)                       // virtual time: delays, debounces, timeouts
    subscribe()                          // a visible screen, for whileSubscribed work
}
```

- **Test behaviour through intents,** not private functions. The intents, states and effects are the
  contract.
- **The test fails if anything is left over:** a state change, an effect, or an error the test did not
  expect.
- **Pure pieces get plain unit tests:** gatherers (`checkDraft()`), transitions (`answered(result)`), named
  partials and mappers.
- **What to cover at each level:**

  | Level | What to cover |
  |---|---|
  | 1 | The happy path, each failure path, and each guard (what is ignored and when) |
  | 2 | Each policy: a fast second intent cancels, is dropped, queues, or runs alongside, as declared |
  | 3 | Each transition, and that an intent in the wrong phase does nothing |
  | 4 | Each slice alone, plus the parent's routing, its cross-slice rules and its handling of child outputs |

- **Before moving a screen between levels, test it through its store.** Assert only on what the screen sees
  and on effects, so the same test passes before and after the move. `SignInBehaviourTest` runs one suite
  against four implementations.

## 8. Names and files

| Thing | Name | File |
|---|---|---|
| State, Intent, Effect | `ReportState`, `ReportIntent`, `ReportEffect` | `ReportContract.kt` |
| Actions, partials (level 2) | `SearchAction`, `SearchPartial` | `SearchContract.kt` |
| Phases (level 3) | `SignInPhase.Editing`, `SignInPhase.Submitting` | beside the logic |
| Logic | `ReportLogic` | `ReportLogic.kt` |
| ViewModel | `ReportViewModel` | `ReportLogic.kt`, under the logic |
| Screen | `ReportScreen` (route) and `ReportContent` (stateless) | `ReportScreen.kt` |
| Mappers to component state | `toBodyUi()` | `CreatePostMappers.kt` |
| Slices | a subpackage each: `createpost/body/`, `createpost/poll/` | `Body.kt` (contract and logic together while small) |

Name intents after what the user did (`EditQuery`), actions after what to do (`Search`), partials after the
change (`Loading`, `Results`), and phases after the state the screen is in (`Verifying`).

## 9. Review checklist

For every pull request that adds or changes a screen:

- [ ] The screen is at the lowest level whose line it has crossed (see [migrations](migrations.md)). A level
      above that has a comment saying why.
- [ ] The state is immutable, and nothing stored can be computed from other fields.
- [ ] Intents say what the user did and carry raw input.
- [ ] Effects are one-off only, and every effect is handled.
- [ ] The logic reads the state into locals or an action before suspending.
- [ ] IO is wrapped in `safeCall`, and each failure ends as a state or an effect.
- [ ] The logic has no `Job` fields, no `if (isLoading) return` guards against overlap, and no "is this
      result still current" checks. Any of these means level 2.
- [ ] No three flags exclude each other, and no handler starts with a guard that reads two flags. Either
      means level 3.
- [ ] The ViewModel is a `StoreOwner` with one line.
- [ ] Only the route composable takes the `Store`.
- [ ] Children do not reference each other, and each part of the state has one writer.
- [ ] The logic has a `storeTest`, and pure pieces have unit tests.

## 10. Common mistakes

| Mistake | Why it hurts | Instead |
|---|---|---|
| `onClick = { vm.store.postIntent(Submit(email.trim())) }` | The screen decides behaviour, and that rule is not tested | `postIntent(Submit)`. The logic trims |
| A navigation flag in the state (`val navigateHome: Boolean`) | It fires again after a rotation, or never resets | An effect |
| An error message as an effect when the screen must keep showing it | It is gone after a rotation | State |
| `launch { val q = state.query; … }` | `state` is read after the intent, maybe after other changes | `val q = state.query` before `launch`, or an action |
| A `Job?` field to cancel the previous search | Hand-made concurrency that every new path must remember | Level 2, `Policy.Latest` |
| `isLoading`, `isVerifying`, `isLocked`, `step` side by side | Impossible combinations can be written, and every rule touches every guard | Level 3 phases |
| A function on the ViewModel (`fun refresh()`) | The behaviour is split between the ViewModel and the logic, and untested | An intent |
| One slice calling another (`tags.postIntent(…)` from the poll slice) | A hidden coupling: the split exists in name only | The parent routes |
| A parent writing into a child's state | Two writers for one part of the state, so changes race | Send the child an intent |
| A `StateHolder` with mutable fields | No single transition point, so no logs, no replay, no value to compare | One immutable state and mappers |
| Moving up a level "to be ready" | Code that pays for a problem the screen does not have | Wait for the line |
