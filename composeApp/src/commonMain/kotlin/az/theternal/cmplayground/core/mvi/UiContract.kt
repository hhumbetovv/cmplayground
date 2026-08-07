package az.theternal.cmplayground.core.mvi

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Screen-level state. One immutable class per screen, owned by whatever holder the feature uses
 * (Orbit container, `MutableStateFlow`, a presenter function, ...).
 *
 * The UI never receives this type directly: it receives a `State<S>` and narrows it into
 * component states with [az.theternal.cmplayground.core.state.map] /
 * [az.theternal.cmplayground.core.state.read]. That is what keeps recomposition scoped to the
 * component whose slice actually changed.
 *
 * Rules that make the model hold:
 * - immutable properties only, immutable collections only (`kotlinx.collections.immutable`);
 * - no Compose `MutableState` inside — ephemeral, UI-owned state belongs in a
 *   [az.theternal.cmplayground.core.state.FieldState] holder instead;
 * - projection functions (`fun searchFieldState(): ...`) build the per-component state classes.
 */
@Stable
interface UiState

/**
 * State of a single component, produced by a projection function on a [UiState].
 *
 * A component state is rebuilt on every derivation, so it must be a `data class` of immutable
 * values: structural equality is exactly what decides whether the component recomposes.
 */
@Immutable
interface ComponentState

/** A user (or system) intention, reduced by the state holder into a new [UiState]. */
@Immutable
interface UiIntent

/** A one-shot event — navigation, snackbar, keyboard — that must never be part of [UiState]. */
@Immutable
interface UiEffect
