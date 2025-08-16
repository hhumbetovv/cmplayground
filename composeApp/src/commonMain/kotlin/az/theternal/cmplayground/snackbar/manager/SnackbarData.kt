package az.theternal.cmplayground.snackbar.manager

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class SnackbarData(
    val message: String,
    val color: Color = Color.Black,
    val align: SnackbarAlign = SnackbarAlign.BOTTOM,
    val action: (@Composable RowScope.() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
) {

    companion object {
        fun empty() = SnackbarData("")
    }
}

enum class SnackbarAlign {
    TOP,
    BOTTOM
}