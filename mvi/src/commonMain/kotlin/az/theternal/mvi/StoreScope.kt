package az.theternal.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * What presentation logic sees of the store that hosts it: the state, the one way to change it, the way
 * out for effects, and a scope that lives as long as the store. A slice sees its own part of the state
 * only, and does not know it. It takes effects of its own type or any narrower one, so a slice with no
 * effects (`Nothing`) or with a subtype of the parent's effects attaches as it is.
 */
interface StoreScope<S, in E> {
    /** The state now. After a suspension, read it again: it may have changed meanwhile. */
    val state: S

    /** The state now and every state after it. */
    val states: Flow<S>

    /**
     * How many collect the store's state now. A screen collects it only while it is visible
     * (`collectAsState` is lifecycle-aware), so this is what [whileSubscribed] runs on.
     */
    val subscribers: StateFlow<Int>

    /**
     * Lives as long as the store — or, for a slice, as long as it is attached. A failure inside it is
     * reported, and cancels neither the store nor its other work.
     */
    val coroutineScope: CoroutineScope

    fun reduce(partial: Partial<S>)

    fun emit(effect: E)
}

/** An anonymous [Partial]: `setState { copy(name = value) }`. */
fun <S, E> StoreScope<S, E>.setState(block: S.() -> S) {
    reduce(Partial { state -> state.block() })
}

fun StoreScope<*, *>.launch(block: suspend CoroutineScope.() -> Unit): Job {
    return coroutineScope.launch(block = block)
}

/**
 * Collects [flow] while the store lives. This is a binder: a source outside the screen — a session, a
 * setting, an event bus — mapped into the state, with no other logic in it.
 */
fun <T> StoreScope<*, *>.observe(
    flow: Flow<T>,
    onEach: suspend (T) -> Unit,
): Job {
    return launch { flow.collect { value -> onEach(value) } }
}

/**
 * Runs [block] while someone collects the state — the screen is visible — and cancels it
 * [stopTimeoutMillis] after the last collector leaves. It runs again when one comes back. Orbit's
 * `repeatOnSubscription`: for work that only matters on screen, such as live counts, a location, a socket.
 *
 * The timeout keeps a rotation or a quick trip to another app from restarting the work.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun StoreScope<*, *>.whileSubscribed(
    stopTimeoutMillis: Long = DEFAULT_STOP_TIMEOUT_MILLIS,
    block: suspend CoroutineScope.() -> Unit,
): Job {
    return launch {
        subscribers
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .transformLatest { subscribed ->
                if (!subscribed) delay(stopTimeoutMillis)
                emit(subscribed)
            }.distinctUntilChanged()
            .collectLatest { subscribed -> if (subscribed) kotlinx.coroutines.coroutineScope(block) }
    }
}

/** A binder ([observe]) that runs only while the screen is visible ([whileSubscribed]). */
fun <T> StoreScope<*, *>.observeWhileSubscribed(
    flow: Flow<T>,
    stopTimeoutMillis: Long = DEFAULT_STOP_TIMEOUT_MILLIS,
    onEach: suspend (T) -> Unit,
): Job {
    return whileSubscribed(stopTimeoutMillis) { flow.collect { value -> onEach(value) } }
}

const val DEFAULT_STOP_TIMEOUT_MILLIS = 5_000L
