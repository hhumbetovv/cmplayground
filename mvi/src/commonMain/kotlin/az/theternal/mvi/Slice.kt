package az.theternal.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

/**
 * Level 4. Runs [logic] on one part of a parent's state: it reads and reduces only that part. The logic
 * cannot tell it is a slice, so the same class is hosted as a screen, a component ViewModel, or a slice
 * of a bigger screen — moving it is a change of host, not of code.
 *
 * A slice's effects are its outputs, and they go to the parent, not to the screen: [onEffect] decides —
 * forward one to the screen (`emit`), or act on it (change the parent's state, send another slice an
 * intent). This is Decompose's rule: a child talks only to its parent, and the parent alone knows its
 * children.
 *
 * Slices never see each other. When one needs something another has, the parent passes it — as the
 * parameters of an intent it sends — and rules that span slices (nothing is editable while publishing)
 * are the parent's.
 *
 * Attach every slice in the parent's `onStart`. Each one gets a scope of its own inside the parent's —
 * Decompose's child lifecycle — so [detach] stops all of its work and nothing else.
 */
class Slice<PS, PE, S, I, E>(
    private val logic: Logic<S, I, E>,
    private val get: (PS) -> S,
    private val set: PS.(S) -> PS,
    private val onEffect: StoreScope<PS, PE>.(E) -> Unit,
) {
    private var scope: ChildScope<PS, PE, S, E>? = null

    fun attach(parent: StoreScope<PS, PE>) {
        check(scope == null) { "The slice is attached already" }
        val child = ChildScope(parent = parent, get = get, set = set, onEffect = onEffect)
        scope = child
        logic.start(child)
    }

    /** Cancels the slice's work. Its part of the state stays as it was. */
    fun detach() {
        scope?.close()
        scope = null
    }

    fun postIntent(intent: I) {
        val scope = checkNotNull(scope) { "The slice is not attached: attach it in the parent's onStart" }
        logic.handle(scope, intent)
    }
}

/** A slice whose effects go to the screen as they are — or that has none (`Nothing`). */
fun <PS, E, S, I> slice(
    logic: Logic<S, I, E>,
    get: (PS) -> S,
    set: PS.(S) -> PS,
): Slice<PS, E, S, I, E> {
    return Slice(logic = logic, get = get, set = set, onEffect = { effect -> emit(effect) })
}

/**
 * A child's view of its parent's store: one part of the state, effects routed through [onEffect], and a
 * scope of its own. Once closed, it changes nothing and emits nothing: a closed child's late result is
 * dropped, not written over whatever the parent holds there now.
 */
internal class ChildScope<PS, PE, S, E>(
    private val parent: StoreScope<PS, PE>,
    private val get: (PS) -> S?,
    private val set: PS.(S) -> PS,
    private val onEffect: StoreScope<PS, PE>.(E) -> Unit,
) : StoreScope<S, E> {
    private var last: S = checkNotNull(get(parent.state)) { "A child starts with its state in the parent's" }
    private var isOpen = true

    override val state: S
        get() {
            get(parent.state)?.let { last = it }
            return last
        }

    override val states: Flow<S> = parent.states.mapNotNull(get).distinctUntilChanged()
    override val subscribers: StateFlow<Int> get() = parent.subscribers

    override val coroutineScope: CoroutineScope = CoroutineScope(
        parent.coroutineScope.coroutineContext + SupervisorJob(parent.coroutineScope.coroutineContext[Job]),
    )

    override fun reduce(partial: Partial<S>) {
        if (!isOpen) return
        parent.reduce(
            Partial { state ->
                val part = get(state)
                if (part == null) state else state.set(partial.reduce(part))
            },
        )
    }

    override fun emit(effect: E) {
        if (isOpen) parent.onEffect(effect)
    }

    fun close() {
        isOpen = false
        coroutineScope.cancel()
    }
}
