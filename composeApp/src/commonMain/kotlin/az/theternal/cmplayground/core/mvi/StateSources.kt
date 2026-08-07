package az.theternal.cmplayground.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState

/**
 * The UI's only contract is `State<S>`, and every architecture already knows how to produce one:
 * Orbit through `collectAsState()`, a `StateFlow` holder through `collectAsStateWithLifecycle()`.
 * Those are called directly in the screen function — wrapping them would only rename them.
 *
 * What matters at the call site is not which function produced the `State`, it is that the screen
 * does **not** read it: `val state = viewModel.collectAsState()`, never
 * `val state by viewModel.collectAsState()`. The `by` subscribes the screen to every state change
 * and undoes the projection below it.
 *
 * The one case with no ready-made function is state that is already a value in composition, which
 * is what [asUiState] covers.
 */

/**
 * Lifts a plain state value into a `State<S>`, for previews and for presenter-style architectures
 * (Molecule, `@Composable` presenters) that return the state class on every composition.
 */
@Composable
fun <S : UiState> S.asUiState(): State<S> = rememberUpdatedState(this)
