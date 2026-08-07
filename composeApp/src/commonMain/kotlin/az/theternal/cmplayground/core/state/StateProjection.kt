package az.theternal.cmplayground.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Narrows a state holder to a slice **without reading it here**.
 *
 * This is the "pass it down" half of the model: the returned [State] is read at its point of use,
 * so the caller does not subscribe to the source and does not recompose when unrelated parts of
 * the screen state change.
 *
 * ```
 * // parent: does not recompose when `query` changes
 * TasksSearchField(state = state.map { searchFieldState() }, ...)
 * ```
 *
 * The selector is a receiver lambda (`map { searchFieldState() }`, not `map { it.searchFieldState() }`).
 *
 * Two deliberate implementation choices:
 * - the derivation is keyed on the source holder only, and the selector is tracked through
 *   [rememberUpdatedState]. A selector that captures something (a loop index, a row id) therefore
 *   re-derives instead of going stale, and no `key(...)` wrapper is needed at the call site. A
 *   non-capturing selector is a compiler singleton, so the common case costs nothing;
 * - [structuralEqualityPolicy] is explicit: component states are rebuilt on every derivation, and
 *   `equals` — not identity — is what must decide whether the consumer recomposes.
 */
@Composable
fun <T, R> State<T>.map(selector: T.() -> R): State<R> {
    val currentSelector by rememberUpdatedState(selector)
    return remember(this) {
        derivedStateOf(structuralEqualityPolicy()) { currentSelector(value) }
    }
}

/**
 * Reads one slice **here**. Recomposes the calling component only when the selected value changes.
 *
 * This is [map] followed by a read, which is the whole mental model: `map` to pass down, `read` to
 * consume.
 */
@Composable
fun <T, R> State<T>.read(selector: T.() -> R): R = map(selector).value

/**
 * Reads the whole holder. Recomposes on every change of it, so only call it where the holder is
 * already narrow enough — typically on a component state that a [map] chain produced.
 */
fun <T> State<T>.read(): T = value

/**
 * Non-composable counterpart of [map], for derivations built outside composition.
 *
 * Inside composition use [map]: it remembers the derivation instead of allocating a new one per
 * recomposition.
 */
fun <T, R> State<T>.derive(selector: T.() -> R): State<R> =
    derivedStateOf(structuralEqualityPolicy()) { selector(value) }
