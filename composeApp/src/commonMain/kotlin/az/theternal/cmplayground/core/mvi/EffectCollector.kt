package az.theternal.cmplayground.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Lifecycle-aware effect collection for holders that are not Orbit hosts.
 *
 * Orbit ships this as `collectSideEffect`, which the Orbit screen calls directly. There is no
 * equivalent for a plain `Flow`, so the alternative is repeating `LaunchedEffect` +
 * `repeatOnLifecycle` at every non-Orbit screen — this is the same body, written once.
 *
 * The lifecycle gate is the point: without it, a navigation effect can fire while the screen is
 * stopped.
 */
@Composable
fun <E : UiEffect> Flow<E>.collectUiEffects(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (E) -> Unit,
) {
    val effects = this
    val currentCollector by rememberUpdatedState(collector)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            effects.collect { currentCollector(it) }
        }
    }
}
