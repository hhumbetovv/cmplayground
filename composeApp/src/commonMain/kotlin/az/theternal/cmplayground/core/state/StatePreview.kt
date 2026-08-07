package az.theternal.cmplayground.core.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

/** Write access to a [UiState] limited to previews. */
class UiStateEditor<S : UiState> internal constructor(val state: S) {
    fun <T> UiState.Field<T>.set(value: T) = write(value)
}

/**
 * Fills a [UiState] with preview values, but only inside `@Preview` rendering — at runtime the
 * block is skipped, so the same holder can be handed to the real screen and to previews.
 *
 * This is the one write path outside the state's owner, and it is inert wherever it would matter.
 */
@Composable
fun <S : UiState> S.preview(block: UiStateEditor<S>.() -> Unit): S {
    if (LocalInspectionMode.current) UiStateEditor(this).block()
    return this
}
