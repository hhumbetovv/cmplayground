# MVI cookbook

The cases real screens have, each with one recommended answer. Read the [guide](guide.md) first: this page
assumes its model and its rules. Every case has the same shape: **the situation, the rule, the code, the
test.**

In the snippets, `Scope` is the logic class's own `private typealias Scope = StoreScope<State, Effect>`
([guide, section 6](guide.md#6-keeping-the-generics-quiet)); a snippet that shows only handlers leaves the
alias line out.

Code marked *sample* exists in `samples/` and is tested there. Code marked *pattern* uses only the runtime's
API, but there is no sample of it yet.

| # | Case | Level | Where |
|---|---|---|---|
| 1 | [Load on open, with retry](#1-load-on-open-with-retry) | 1 | pattern |
| 2 | [Refresh without blanking the screen](#2-refresh-without-blanking-the-screen) | 2 | sample: `feed` |
| 3 | [Pagination](#3-pagination) | 2 | pattern |
| 4 | [Search as you type](#4-search-as-you-type) | 2 | sample: `search` |
| 5 | [Optimistic update with rollback](#5-optimistic-update-with-rollback) | 2 | sample: `search`, `feed/Like` |
| 6 | [A form: validation and errors](#6-a-form-validation-and-errors) | 1 | sample: `report` |
| 7 | [A multi-step flow](#7-a-multi-step-flow) | 3 | sample: `signin` |
| 8 | [Observing a source outside the screen](#8-observing-a-source-outside-the-screen) | any | pattern |
| 9 | [Arguments in, results out](#9-arguments-in-results-out) | any | pattern |
| 10 | [A dialog or sheet with logic](#10-a-dialog-or-sheet-with-logic) | any | sample: `feed` |
| 11 | [Timers, countdowns, polling](#11-timers-countdowns-polling) | 3 | sample: `signin` |
| 12 | [Selection mode in a list](#12-selection-mode-in-a-list) | 1 | pattern |
| 13 | [Platform calls: permissions, share, haptics](#13-platform-calls-permissions-share-haptics) | any | pattern |
| 14 | [Reacting to the lifecycle](#14-reacting-to-the-lifecycle) | any | pattern |
| 15 | [Where an error goes](#15-where-an-error-goes) | any | rules |
| 16 | [One piece per list item](#16-one-piece-per-list-item) | any | sample: `feed/Like` |
| 17 | [Draft that survives the process](#17-draft-that-survives-the-process) | any | sample: `createpost` |
| 18 | [Testing recipes](#18-testing-recipes) | any | |

Internal state (cursors, attempts, ids the screen does not draw) is [guide section 11](guide.md#11-internal-state).

## 1. Load on open, with retry

**Situation.** The screen needs data when it opens, and the user can retry after a failure.

**Rule.** The data is one `ContentState`. Load in `onStart`, so a screen that is never shown loads nothing
(`Start.Lazily`). Retry is an intent named after what the user did.

```kotlin
data class ProfileState(val profile: ContentState<Profile> = ContentState.Idle) : ViewState
sealed interface ProfileIntent : ViewIntent { data object Retry : ProfileIntent }

class ProfileLogic(private val userId: String, private val users: UserRepository) :
    Logic<ProfileState, ProfileIntent, Nothing> {
    private typealias Scope = StoreScope<ProfileState, Nothing>

    override fun Scope.onStart() = load()

    override fun Scope.onIntent(intent: ProfileIntent) = when (intent) {
        ProfileIntent.Retry -> load()
    }

    private fun Scope.load() {
        setState { copy(profile = profile.reload()) }
        launch {
            val result = safeCall { users.profile(userId) }
            setState { copy(profile = result.toContentState()) }
        }
    }
}
```

Beware: two taps on Retry start two loads. That is the signal for level 2 (case 2).

**Test.** Success; failure then `Retry` then success; the state after `Retry` is `Loading`.

## 2. Refresh without blanking the screen

**Situation.** Pull to refresh, while the list stays on screen.

**Rule.** `reload()` keeps the previous data in `Loading(previous)`. A refresh is `Policy.Drop`: a second pull
while one runs is ignored. See `FeedLogic`.

```kotlin
override fun policy(action: Unit) = Policy.Drop

override suspend fun Scope.process(action: Unit) {
    setState { copy(posts = posts.reload()) }
    val result = safeCall { repository.feed() }
    setState { copy(posts = result.toContentState()) }
}
```

A failed refresh with data on screen should not replace the list with an error screen. Either:

- keep it simple: `Error` replaces the list, and Retry is shown (right for most screens); or
- keep the list and tell the user once: on failure, restore `previous` with `setState` and `emit` a message.

Choose one per product, and use it on every list.

## 3. Pagination

**Situation.** A list that loads the next page when the user scrolls near the end.

**Rule.** The items and the cursor are state. The cursor is internal, so it is hidden with `mapState`
([guide 11.1](guide.md#11-internal-state)). Loading the next page is `Policy.Drop`, so scrolling fast does not
load a page twice. A refresh and a next page are different actions with different keys, and a refresh must
cancel a next page in flight: give both one key under `Policy.Latest`, or reset the list in the refresh.

```kotlin
data class FeedState(
    val items: List<Post> = emptyList(),
    val cursor: String? = null,                 // internal: where the next page starts
    val hasMore: Boolean = true,
    val page: ContentState<Unit> = ContentState.Idle,   // the state of the page now loading
) : ViewState

sealed interface FeedAction {
    data object Refresh : FeedAction
    data class LoadMore(val cursor: String) : FeedAction   // the cursor is gathered from the state
}
```

```kotlin
override fun Scope.toAction(intent: FeedIntent): FeedAction? = when (intent) {
    FeedIntent.Refresh -> FeedAction.Refresh
    FeedIntent.ReachedEnd -> state.cursor?.takeIf { state.hasMore }?.let(FeedAction::LoadMore)
}

override fun policy(action: FeedAction) = Policy.Latest
override fun key(action: FeedAction): Any = "feed"      // a refresh cancels a next page, and the reverse

override suspend fun Scope.process(action: FeedAction) {
    val cursor = (action as? FeedAction.LoadMore)?.cursor
    setState { copy(page = page.reload()) }
    safeCall { repository.feed(cursor) }
        .onSuccess { page ->
            setState {
                copy(
                    items = if (cursor == null) page.items else items + page.items,
                    cursor = page.next,
                    hasMore = page.next != null,
                    page = ContentState.Success(Unit),
                )
            }
        }.onFailure { error -> setState { copy(page = ContentState.Error(error)) } }
}
```

Points:

- `LoadMore` carries the cursor it was started from. A result for an old cursor can only come from an action
  that `Latest` already cancelled.
- A failed next page keeps `items` and sets `page = Error`: the list stays, and the footer shows Retry.
- The UI posts `ReachedEnd` from a `LaunchedEffect` that watches the last visible index. The logic decides if
  it matters.

**Test.** First page; `ReachedEnd` appends and moves the cursor; `ReachedEnd` while loading does not load
twice; `Refresh` during a next page keeps only the refresh's result; the last page sets `hasMore = false`.

## 4. Search as you type

**Situation.** A query changes on every keystroke, and answers return in any order.

**Rule.** `Policy.Latest` with a `delay` at the start of `process`. That is the debounce, and the
cancellation of old answers, with no `Job` field. The typed text is reduced at once in `toAction` (the field
must not lag), and the request is the action. See `SearchLogic`.

**Test.** Several `EditQuery` within the debounce produce one request; an old slow answer does not overwrite
a new one; an empty query clears the results; `Retry` skips the debounce.

## 5. Optimistic update with rollback

**Situation.** A like, a follow, a toggle: the screen shows the result before the backend agrees.

**Rule.**

1. Gather the **wish** when the user taps (`liked = !state.isLiked`), and send "set X", never "toggle".
2. Change the state at once, in `toAction`.
3. Keep `before` in the action, and put it back on failure, with an effect that says so.
4. Use `Policy.Latest` with a key per thing (`"follow:$id"`), so rapid taps send the last wish only, and
   different things do not cancel each other.

See `SearchLogic.setFollowing` and `LikeLogic`. Do not roll back by recomputing (`!liked`): a second tap may
have happened in between. Roll back to the saved value.

**Test.** Success keeps the change; failure restores `before` and emits the effect; three quick taps send one
request with the last wish.

## 6. A form: validation and errors

**Situation.** Fields, rules, an error under a field, a submit button.

**Rule.**

- Fields are state, and the **intent carries the raw text**. Trimming and validating happen in the logic.
- Whether Send is allowed is a **computed property** (`canSend`). It is never a stored flag.
- Show a field's error from the state, and decide *when* to show it with data, not with the UI:

  | When to show | State |
  |---|---|
  | While typing, from the first character | `fieldError` computed from the text |
  | After the user left the field | a `touched: Set<Field>` in the state, error computed from text and `touched` |
  | Only after a submit | a `submitted: Boolean` in the state |

- A rule the server decides (name taken) comes back as a result: put it in state as `serverError`, and clear it
  when the field changes.
- Submit is `Drop` at level 2, or `isSending` plus a guard at level 1. See `ReportLogic`.
- Never navigate from the state. A successful submit is an effect (`Sent`).

```kotlin
data class SignUpState(
    val email: String = "",
    val password: String = "",
    val submitted: Boolean = false,
    val isSending: Boolean = false,
    val serverError: String? = null,
) : ViewState {
    val emailError: String? get() = if (submitted && !email.contains('@')) "Not an email" else null
    val canSend: Boolean get() = !isSending
}
```

Here `canSend` is true even when the form is invalid on purpose: the first press reveals the errors
(`submitted = true`). A disabled button that gives no reason is worse than an enabled one that explains. If the
product wants it disabled, compute `canSend` from the fields.

**Test.** Each rule through its error; the submit that is invalid sets `submitted` and does not call the
repository; a server error appears, and editing the field clears it.

## 7. A multi-step flow

**Situation.** Steps (details → address → confirm), or a flow with exclusive modes (editing, submitting,
verifying, locked).

**Rule.**

- If the steps are only **which fields to show** and the data is shared, keep level 1 with a `step` field,
  and keep `step` in a keeper.
- If the modes **exclude each other and carry different data** (`Verifying` has a code, `Editing` has fields),
  go to level 3: a sealed type of phases. Intents in the wrong phase do nothing, by construction.
- Back from a step is an intent (`Back`) that moves the phase. The system back button is the screen posting
  that intent, or an effect `Exit` when there is nothing to go back to.

See `signin/` for the same flow at all three forms, and [migrations](migrations.md) for when to move.

**Test.** Each transition; each intent in a phase that must ignore it; back through every step keeps the
entered data.

## 8. Observing a source outside the screen

**Situation.** The session, a setting, a cart that other screens change.

**Rule.** The source is a `Flow` from a repository. A binder (`observe`) maps it into the state in `onStart`,
and has no other logic. The screen never owns the source, and never copies it into a field of its own.

```kotlin
override fun Scope.onStart() {
    observe(cart.items) { items -> setState { copy(items = items) } }
}
```

- If it matters only while the screen is visible (a live count, a location), use `observeWhileSubscribed`.
- A change **to** the source is an intent that calls the repository. The state changes when the source emits
  back. That gives one writer, and two screens never disagree.
- If you need the change to appear before the repository confirms, that is case 5.

**Test.** A fake repository exposes a `MutableStateFlow`. Emit into it and assert the state.

## 9. Arguments in, results out

**Situation.** A screen gets an id; a picker screen returns a choice.

**Arguments** are constructor parameters of the logic, passed through the ViewModel factory:
`ReportViewModel(postId, posts)`. They are not state (they never change), and not intents. If the argument
can change while the screen lives (a deep link to another item), it is an intent (`Open(id)`), and the id is
state.

**Results.** The picked value comes back through the navigation layer, and the route turns it into an
**intent**: `LaunchedEffect(result) { postIntent(PickerIntent.Picked(result)) }`. The logic never reads the
navigation result itself. This keeps the logic free of Android and navigation, and makes the result testable
like any other intent.

**Going back.** Navigation and closing are **effects** (`Done`, `OpenPost(id)`). The route handles them with
the navigator. The state never holds "should navigate".

**Test.** The picked intent changes the state; the effect is emitted once.

## 10. A dialog or sheet with logic

**Situation.** A confirm dialog, a report sheet, an inline editor.

**Rule.** It depends on what the part does:

| The part | Use |
|---|---|
| Only displays, and its answer is one tap | State in the parent (`confirmDelete: PostId?`) and intents `Confirm` / `Dismiss`. No slot |
| Has its own IO or several fields, and its work must stop when it closes | **Slot** |
| Is also its own screen elsewhere | Slot with that screen's logic, unchanged |

A slot's state in the parent is `null` when closed, so "is it open" is state and survives rotation. The
child's effects are its outputs, and the parent decides what they mean (`Sent` closes the slot and shows a
message). See `FeedLogic`.

**Test.** Open, send, closes with a message; close while sending cancels the work; an intent to a closed slot
is dropped.

## 11. Timers, countdowns, polling

**Situation.** A resend-code countdown, a poll for a payment status, a debounce that is not a search.

**Rule.**

- A timer that belongs to **a mode** (the `Verifying` phase) is `whileIn(Phase::class) { … }`: it starts on
  entering and is cancelled on leaving, and its `setState` does nothing after the phase is left. The
  remaining seconds are **state in the phase**.
- A timer that belongs to **the visible screen** is `whileSubscribed`.
- A timer that belongs to **one request** is `delay` inside an action.
- Never use `delay` in a UI composable to drive the logic.

```kotlin
whileIn(SignInPhase.Verifying::class) { phase ->
    var left = phase.secondsLeft
    while (left > 0) {
        delay(1_000)
        left -= 1
        setState { copy(secondsLeft = left) } ?: return@whileIn
    }
}
```

**Test.** `advance(1_000)` per tick; leaving the phase stops the ticks; re-entering restarts them.

## 12. Selection mode in a list

**Situation.** Long-press to select items, then act on them.

**Rule.** The selection is a `Set<Id>` in the state, not a flag on each item, so the items stay unchanged and
the screen computes `isSelecting = selection.isNotEmpty()`. Intents: `ToggleSelect(id)`, `ClearSelection`,
`DeleteSelected`. The action for `DeleteSelected` gathers the ids before launching, then clears the selection.
When items are removed from the list, remove them from the set as well (or compute the visible selection as
`selection intersect items.ids`).

**Test.** Toggle on and off; deleting clears; an id that left the list is not counted.

## 13. Platform calls: permissions, share, haptics

**Situation.** The logic needs something that only the platform can do.

**Rule.** Two directions:

- **The logic asks the screen** with an effect (`RequestLocationPermission`, `Share(text)`, `Haptic`). The
  route does it and answers with an **intent** (`PermissionResult(granted)`). The logic stays free of the
  platform, and both directions are testable.
- **The logic needs a capability that is not UI** (a clipboard, a clock, a file system). It is a dependency
  behind an interface in the constructor, with a fake in tests.

The state keeps what the screen must still show after a rotation (`permission: Granted | Denied | Unknown`).
The request itself is the effect.

**Test.** The intent makes the effect; the result intent changes the state.

## 14. Reacting to the lifecycle

**Situation.** Refresh when the screen comes back, stop something when it leaves.

**Rule.**

- Work that should stop while the screen is hidden: `whileSubscribed` / `observeWhileSubscribed`. It follows
  the screen's visibility with a timeout, so a rotation does not restart it.
- A one-time action on every return (refresh on resume): the route posts an intent (`Resumed`) from
  `LifecycleResumeEffect`. The logic decides whether it matters (for example, ignores it if the data is
  fresh).
- Never put `onResume` on the ViewModel.

**Test.** `subscribe()` starts the work; `advance` past the timeout after the subscriber leaves stops it.

## 15. Where an error goes

| The failure | Goes to | Example |
|---|---|---|
| The user can read it and it stays until they act | **State** (`ContentState.Error`, `serverError`) | The feed did not load |
| The user should be told once, and the screen is fine | **Effect** (a message) | A follow failed and was rolled back |
| The user cannot do anything about it, and the logic is correct | `onError` (log, report) | A bug in an observer |
| A coding mistake (`check`, `require`) | Crash in debug, `onError` in release | The slot was not attached |

Rules:

- **Every `safeCall` ends as a state or an effect.** A `safeCall` whose failure is ignored is a bug, and a
  reviewer rejects it.
- **Do not show `error.message` to users.** Map the error to a typed reason in the logic (`NoNetwork`,
  `NotFound`), and let the UI own the text. Raw messages stay in logs.
- **Retry** is an intent. A failed result keeps what is shown (case 2) when the screen can still be used.
- **Cancellation is not an error.** `safeCall` rethrows it, so a cancelled search never reaches the user.

## 16. One piece per list item

**Situation.** A like button on every card, a download button on every row.

**Rule.** When the piece does its own IO, it is a **component ViewModel** hosted with `ItemScope`, keyed by
the item's id. It lives while the item is in the list and is cleared when it is dropped. The list passes only
the **initial values**, and the component then owns its state. A refresh of the list does not reset it, and
that is the trade-off: if the list must be the source of truth, the logic stays in the list screen and the
item is a stateless row with an intent carrying the id.

| The row | Use |
|---|---|
| Only displays; actions go to the list's logic with an id | Stateless row |
| Does its own IO, and the list does not need its result | Component ViewModel (`LikeLogic`) |
| Does its own IO, and the list needs the result | Stateless row, logic in the list |

**Test.** `LikeLogic` alone, as its own store. The list's test does not know about it.

## 17. Draft that survives the process

**Situation.** The user typed a long text, and the app is killed in the background.

**Rule.** A keeper saves a **part** (what the user typed or chose), and merges it back into the initial
state. Never loaded data, never a phase. See `createpost/`, and
[guide 11.4](guide.md#114-internal-state-and-process-death).

**Test.** Create the store with the saved part and assert the restored state; save, change, assert what is
written. A saved value that no longer decodes is dropped, and the screen starts clean.

## 18. Testing recipes

| I want to… | Do |
|---|---|
| Test a time-based rule | `advance(ms)`: the clock is virtual |
| See the state before `onStart` | `storeTest(…, start = false) { … }`, then `start()` when ready |
| Check what the screen sees through `mapState` | `StoreHost(…).mapState(toUi)` and compare the `Ui` |
| Test work that runs while visible | `subscribe()` first |
| Test a failure | A fake whose method throws, then `expectState` and `expectEffect` |
| Check nothing happens | `expectNoItems()` after the intent |
| Test a rule that is the same at several levels | One suite over a list of versions (`SignInBehaviourTest`) |
| Test a computed property | A plain unit test on the state |
| Test a mapper | A plain unit test, no store |
| Test a slice alone | The slice's logic as its own store; the parent's test covers routing only |

A good test of a behaviour has four parts: the starting state, the intents, the expected states and effects
in order, and a fake with a deliberate latency. If a test needs a real clock, a real network, or the
Android framework, the logic is holding something it should have received.
