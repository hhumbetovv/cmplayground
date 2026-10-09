package az.theternal.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * What work bound to a phase sees: the phase, a way to change it that does nothing once the phase is
 * left, and the store's effects. A late result can never overwrite the phase the user has moved on to.
 */
class PhaseScope<S : Any, T : S, E> internal constructor(
    private val store: StoreScope<S, E>,
    private val phase: KClass<T>,
    entered: T,
) {
    private var last: T = entered

    /** The phase now; once it is left, the last value it had. */
    val state: T
        get() {
            val current = store.state
            @Suppress("UNCHECKED_CAST")
            if (phase.isInstance(current)) last = current as T
            return last
        }

    /**
     * Moves from the phase to the state [block] returns — the same phase updated, or another one — and
     * answers that state. Ignored when the store is no longer in the phase, and then answers null.
     */
    fun setState(block: T.() -> S): S? {
        var moved: S? = null
        store.reduce(
            Partial { current ->
                @Suppress("UNCHECKED_CAST")
                if (phase.isInstance(current)) (current as T).block().also { moved = it } else current.also { moved = null }
            },
        )
        return moved
    }

    fun emit(effect: E) {
        store.emit(effect)
    }
}

/**
 * Work that belongs to a phase: [block] starts every time the state enters [phase], and is cancelled as
 * soon as the state leaves it. Call it in `onStart`. This is level 3 without a state-machine library —
 * the state is a sealed type of phases, intents are handled per phase, and the work of each phase is a
 * `whileIn` — and it is what Orbit's `runOn` does for intents.
 *
 * The work restarts only on entering the phase, so a phase that updates its own data (a countdown, an
 * upload's progress) keeps its work running. When the work depends on something inside the phase, give
 * it a [key]: it then also restarts when the key changes.
 *
 * Phases come from a `StateFlow`, so a phase left and entered again before the work's coroutine runs is
 * seen as never left.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <S : Any, T : S, E> StoreScope<S, E>.whileIn(
    phase: KClass<T>,
    key: (T) -> Any? = { },
    block: suspend PhaseScope<S, T, E>.(entered: T) -> Unit,
): Job {
    val store = this
    return launch {
        states
            .map { current ->
                @Suppress("UNCHECKED_CAST")
                if (phase.isInstance(current)) Entered(current as T, key(current)) else null
            }.distinctUntilChanged { old, new -> if (old == null || new == null) old === new else old.key == new.key }
            .collectLatest { entered ->
                if (entered != null) PhaseScope(store, phase, entered.value).block(entered.value)
            }
    }
}

/**
 * Runs [block] now if the state is in [phase], and cancels it as soon as the state leaves the phase —
 * Orbit's `runOn`. For work an intent starts that only makes sense in one phase: a resend that the user's
 * going back makes pointless. Null when the state is not in the phase: nothing runs.
 */
fun <S : Any, T : S, E> StoreScope<S, E>.runOn(
    phase: KClass<T>,
    block: suspend PhaseScope<S, T, E>.(current: T) -> Unit,
): Job? {
    val current = state
    if (!phase.isInstance(current)) return null
    @Suppress("UNCHECKED_CAST")
    val entered = current as T
    val store = this
    return launch {
        val work = launch { PhaseScope(store, phase, entered).block(entered) }
        val watcher = launch {
            states.first { state -> !phase.isInstance(state) }
            work.cancel()
        }
        work.join()
        watcher.cancel()
    }
}

private class Entered<T>(
    val value: T,
    val key: Any?,
)
