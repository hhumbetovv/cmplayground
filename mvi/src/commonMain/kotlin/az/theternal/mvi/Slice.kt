package az.theternal.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Level 4. Runs [logic] on one part of a parent's state: it reads and reduces only that part, and its
 * effects go out as the parent's. The logic cannot tell it is a slice, so the same class is hosted as a
 * screen, a component ViewModel, or a slice of a bigger screen — moving it is a change of host, not of
 * code.
 *
 * Slices never see each other. When one needs something another has, the parent passes it — as the
 * parameters of an intent it sends — and rules that span slices (nothing is editable while publishing)
 * are the parent's.
 *
 * Attach every slice in the parent's `onStart`.
 */
class Slice<PS, PE, S, I, E>(
    private val logic: Logic<S, I, E>,
    private val get: (PS) -> S,
    private val set: PS.(S) -> PS,
    private val mapEffect: (E) -> PE,
) {
    private var scope: StoreScope<S, E>? = null

    fun attach(parent: StoreScope<PS, PE>) {
        val focused = FocusedScope(parent = parent, get = get, set = set, mapEffect = mapEffect)
        scope = focused
        logic.start(focused)
    }

    fun postIntent(intent: I) {
        val scope = checkNotNull(scope) { "The slice is not attached: attach it in the parent's onStart" }
        logic.handle(scope, intent)
    }
}

/** A slice whose effects are already the parent's type — or that has none (`Nothing`). */
fun <PS, E, S, I> slice(
    logic: Logic<S, I, E>,
    get: (PS) -> S,
    set: PS.(S) -> PS,
): Slice<PS, E, S, I, E> {
    return Slice(logic = logic, get = get, set = set, mapEffect = { effect -> effect })
}

private class FocusedScope<PS, PE, S, E>(
    private val parent: StoreScope<PS, PE>,
    private val get: (PS) -> S,
    private val set: PS.(S) -> PS,
    private val mapEffect: (E) -> PE,
) : StoreScope<S, E> {
    override val state: S get() = get(parent.state)
    override val states: Flow<S> = parent.states.map(get).distinctUntilChanged()
    override val coroutineScope: CoroutineScope get() = parent.coroutineScope

    override fun reduce(partial: Partial<S>) {
        parent.reduce(Partial { state -> state.set(partial.reduce(get(state))) })
    }

    override fun emit(effect: E) {
        parent.emit(mapEffect(effect))
    }
}
