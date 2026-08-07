package az.theternal.cmplayground.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Text field value that is rendered from UI-owned state and committed to the state holder.
 *
 * A text field driven straight from reduced state (`value = state.query`) is at the mercy of how
 * fast the holder turns the keystroke around. Orbit reduces on its own event loop; a `StateFlow`
 * holder may hop dispatchers; either way a slow turnaround shows up as laggy or reordered text.
 * Orbit's own answer, `blockingIntent`, is not available in common code — it only exists in
 * Orbit's `jvmAndNative` source set.
 *
 * So the keystroke is applied locally first (one `MutableState` write, no reducer round trip) and
 * forwarded to the holder, which stays the source of truth for everything derived from it
 * (filtering, validation, submit enablement).
 *
 * External writes still win: a value the holder produces on its own — a clear button, a reset, a
 * restored draft — is pushed into the field. Echoes of our own commits are ignored, so a holder
 * that lags a few keystrokes behind can never rewind what is being typed.
 */
@Stable
class TextInputState internal constructor(
    initial: String,
    private val onCommit: (String) -> Unit,
) {
    var value by mutableStateOf(initial)
        private set

    private var lastCommitted = initial

    fun onValueChange(newValue: String) {
        value = newValue
        lastCommitted = newValue
        onCommit(newValue)
    }

    internal fun syncExternal(external: String) {
        if (external == lastCommitted) return
        lastCommitted = external
        value = external
    }
}

@Composable
fun rememberTextInput(value: String, onValueChange: (String) -> Unit): TextInputState {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val input = remember { TextInputState(value) { currentOnValueChange(it) } }
    LaunchedEffect(value) { input.syncExternal(value) }
    return input
}
