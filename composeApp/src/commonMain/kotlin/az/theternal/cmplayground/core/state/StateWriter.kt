package az.theternal.cmplayground.core.state

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel

/**
 * The write side of [UiState], reachable only inside a [reduceState] block.
 *
 * A screen ViewModel declares nothing for this — it stays `ViewModel(), ContainerHost<S, E>`. The
 * writer is a lambda receiver, not a supertype, so it never appears in the ViewModel's type and
 * never becomes part of its public surface.
 *
 * `sealed` with a private implementation is what closes the loopholes: the type cannot be
 * constructed, and it cannot be implemented outside this package either. The only instance in
 * existence is the one [reduceState] passes to its block.
 */
sealed interface StateWriter {

    fun <T> UiState.Field<T>.set(value: T)

    fun <T> UiState.Field<T>.update(producer: T.() -> T)
}

private object RealStateWriter : StateWriter {

    override fun <T> UiState.Field<T>.set(value: T) = write(value)

    override fun <T> UiState.Field<T>.update(producer: T.() -> T) = write(value.producer())
}

/**
 * Applies state writes as one atomic snapshot change.
 *
 * ```
 * class TasksViewModel : ViewModel(), ContainerHost<TasksState, TasksEffect> {
 *     val state = TasksState()
 *
 *     private fun changeQuery(query: String) = intent {
 *         reduceState { state.query.set(query) }
 *     }
 * }
 * ```
 *
 * Every write goes through here, so atomicity is the default rather than something to remember: no
 * composition can observe the new list one frame before the loading flag clears. It also makes
 * writes from a background thread — Orbit reduces on its own event loop — land together.
 *
 * The receiver is [ViewModel] because that is what every state holder in this project already is;
 * an equivalent overload on `ContainerHost` would scope it to Orbit hosts only. A state class that
 * owns itself needs neither — [UiState] has the same helpers `protected`, which is the UI-owned
 * case.
 */
fun ViewModel.reduceState(block: StateWriter.() -> Unit) {
    Snapshot.withMutableSnapshot { RealStateWriter.block() }
}
