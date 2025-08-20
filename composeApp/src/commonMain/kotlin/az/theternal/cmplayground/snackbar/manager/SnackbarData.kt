package az.theternal.cmplayground.snackbar.manager

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class SnackbarData(
    val message: String,
    val color: Color = Color.Black,
    val durationMillis: Long = DEFAULT_SNACKBAR_DURATION,
    val align: SnackbarAlign = SnackbarAlign.BOTTOM,
    val action: (@Composable RowScope.() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
) {

    companion object {
        fun empty() = SnackbarData("")
        const val DEFAULT_SNACKBAR_DURATION = 2000L
    }
}

enum class SnackbarAlign {
    TOP,
    BOTTOM
}