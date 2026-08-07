package az.theternal.cmplayground.core.mvi

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Screen-level state. One immutable class per screen, owned by whatever holder the feature uses
 * (Orbit container, `MutableStateFlow`, a presenter function, ...).
 *
 * The UI never receives this type. A screen's `rememberXxxScreenState` turns a `State<S>` of it into
 * one component state per component, built once — see
 * [az.theternal.cmplayground.core.state.derive].
 *
 * Rules that make the model hold:
 * - immutable properties only, immutable collections only (`kotlinx.collections.immutable`);
 * - no Compose types inside — the state class must be usable without a composition;
 * - nothing derived is stored: `isFiltered`, `phase()` and the counters are computed, so they cannot
 *   disagree with the fields they came from.
 */
@Stable
interface UiState

/**
 * The input of a single component. Two shapes, chosen per component:
 *
 * ```
 * // values — passed as State<X>, read whole, compared as a unit
 * @Immutable
 * data class TasksErrorViewState(val message: String) : ComponentState
 *
 * // State fields — passed as X, built once, read at each point of use
 * @Stable
 * data class TasksSearchFieldState(
 *     val query: State<String>,
 *     val isClearVisible: State<Boolean>,
 * ) : ComponentState
 * ```
 *
 * Values when the component is small and its fields change together: it stays a plain value that a
 * test can build and compare without Compose.
 *
 * `State` fields when the fields change independently, when the component has inner scopes that can
 * usefully recompose alone, or when a projection behind a field is expensive. The object is then
 * constant, so the component's parameters never change and its caller cannot recompose it.
 *
 * Either way the class is **homogeneous** — all values or all `State`. Mixing them means a component
 * recomposes for the plain fields and not for the others, which nobody can predict from the call
 * site.
 */
@Stable
interface ComponentState

/** A user (or system) intention, reduced by the state holder into a new [UiState]. */
@Immutable
interface UiIntent

/** A one-shot event — navigation, snackbar, keyboard — that must never be part of [UiState]. */
@Immutable
interface UiEffect
