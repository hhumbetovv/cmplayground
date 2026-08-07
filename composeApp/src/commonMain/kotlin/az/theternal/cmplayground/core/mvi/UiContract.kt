package az.theternal.cmplayground.core.mvi

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * The input of a single component: a bundle of `State` references taken from the screen state, one
 * per thing the component displays.
 *
 * A component state is built once, when the screen state is constructed, and never replaced — the
 * references inside it are fixed even though the values behind them change. So the component's
 * parameters never change, the component never recomposes because of them, and recomposition
 * happens only in the scope that reads a `.value`.
 *
 * That is why these are `@Stable` and not `@Immutable`: the object is constant, the values it
 * points at are not.
 *
 * ```
 * @Stable
 * data class TasksSearchFieldState(
 *     val query: State<String>,
 *     val isClearVisible: State<Boolean>,
 * ) : ComponentState
 * ```
 */
@Stable
interface ComponentState

/** A user (or system) intention, turned into field writes by the state holder. */
@Immutable
interface UiIntent

/** A one-shot event — navigation, snackbar, keyboard — that must never be state. */
@Immutable
interface UiEffect
