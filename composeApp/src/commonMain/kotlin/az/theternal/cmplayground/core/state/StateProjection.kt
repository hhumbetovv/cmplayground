package az.theternal.cmplayground.core.state

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Narrows one `State` into another without reading it.
 *
 * [UiState.derived] covers derivations written inside a state class; this covers the ones a
 * component has to build for itself — most of all a per-item slice of a keyed collection, where
 * the id is only known at the call site:
 *
 * ```
 * val task = remember(id) { tasksById.derive { this[id] } }
 * ```
 *
 * The `remember` is required: the derivation must outlive the composition pass, and it is keyed by
 * whatever the selector captures.
 */
fun <T, R> State<T>.derive(selector: T.() -> R): State<R> =
    derivedStateOf(structuralEqualityPolicy()) { selector(value) }
