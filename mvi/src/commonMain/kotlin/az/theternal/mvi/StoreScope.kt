package az.theternal.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
     * Lives as long as the store. A failure inside it is reported, and cancels neither the store nor its
     * other work.
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
