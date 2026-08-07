package az.theternal.cmplayground.core.state

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Narrows one `State` into another without reading it.
 *
 * This is the only piece of state machinery in the project. Everything the UI receives is built out
 * of it, once, in a screen's `rememberXxxScreenState`.
 *
 * ### The two-layer rule
 *
 * ```
 * // layer 1 — one trivial selector per field of the screen state
 * val tasksById = source.derive { tasksById }
 *
 * // layer 2 — real work, chained off layer 1 and never off `source`
 * val doneCount = tasksById.derive { count { (_, task) -> task.isDone } }
 * ```
 *
 * A derivation that reads another derivation is not recomputed while its input's value is
 * unchanged, so `doneCount` runs when the map changes and not when a keystroke changes the query.
 * Chain it off `source` instead and it runs on every change — measured in `DerivationChainTest`.
 *
 * Two cases may read `source` directly:
 * - a **single-field selector** (layer 1), which is what makes the chaining possible;
 * - a **cheap derivation that genuinely depends on several fields**, such as `phase()`. Splitting
 *   that into layer-1 states would duplicate the rule it encodes; re-running a `when` over three
 *   fields is not worth it.
 *
 * Anything that filters, counts, sorts or allocates belongs in layer 2, chained.
 *
 * ### Where to call it
 *
 * Inside a `remember`, so the derivation outlives the composition pass. A derivation created at a
 * call site is rebuilt on every recomposition and caches nothing.
 *
 * The selector is a receiver lambda: `derive { tasksById }`, not `derive { it.tasksById }`.
 */
fun <T, R> State<T>.derive(selector: T.() -> R): State<R> =
    derivedStateOf(structuralEqualityPolicy()) { selector(value) }
