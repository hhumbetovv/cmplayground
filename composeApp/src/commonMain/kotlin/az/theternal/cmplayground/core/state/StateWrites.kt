package az.theternal.cmplayground.core.state

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel

/**
 * Write access to a [UiState] field, available only where a [ViewModel] is in context.
 *
 * The context parameter is the whole enforcement: a composable holding the screen state can read
 * every field and call none of these, because no `ViewModel` is in scope there. No wrapper type, no
 * sealed interface, no block to opt into — the requirement is in the signature.
 */
context(_: ViewModel)
fun <T> UiState.Field<T>.snapshot(value: T) = write(value)

/** The same write, derived from the current value: `state.filters.set { this + filter }`. */
context(_: ViewModel)
fun <T> UiState.Field<T>.snapshot(producer: T.() -> T) = write(value.producer())

/**
 * Applies several field writes as one atomic snapshot change and returns the holder, so it fits
 * where Orbit's `reduce` expects the new state:
 *
 * ```
 * reduce {
 *     state.set {
 *         isLoading.set(false)
 *         errorMessage.set(null)
 *     }
 * }
 * ```
 *
 * Without it each write is its own notification, and a composition can observe the new list one
 * frame before the loading flag clears.
 */
context(_: ViewModel)
fun <S : UiState> S.snapshot(block: S.() -> Unit): S {
    Snapshot.withMutableSnapshot { block() }
    return this
}
