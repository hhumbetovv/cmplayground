package az.theternal.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The store's state for a screen, collected while the screen is at least started. */
@Composable
fun <S> Store<S, *, *>.collectAsState(): State<S> {
    return state.collectAsStateWithLifecycle()
}

/** Hands each effect to [onEffect], once. */
@Composable
fun <E> Store<*, *, E>.CollectEffects(onEffect: suspend (E) -> Unit) {
    val currentOnEffect = rememberUpdatedState(onEffect)
    LaunchedEffect(this) {
        effects.collect { effect -> currentOnEffect.value(effect) }
    }
}

/**
 * One component's state, mapped from the screen's: the component recomposes only when its own part
 * changes. ArenaGo's mapper style — the screen's state stays the logic's, every component gets the shape
 * it draws, and a component that later gets its own ViewModel already has its own state.
 */
@Composable
inline fun <T, R> State<T>.select(crossinline transform: (T) -> R): State<R> {
    return remember(this) { derivedStateOf { transform(value) } }
}

/** [select] whose mapping also reads [key] — it is rebuilt when the key changes. */
@Composable
inline fun <T, R> State<T>.select(
    key: Any?,
    crossinline transform: (T) -> R,
): State<R> {
    return remember(this, key) { derivedStateOf { transform(value) } }
}
