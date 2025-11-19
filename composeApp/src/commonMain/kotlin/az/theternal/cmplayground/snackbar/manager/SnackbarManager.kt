package az.theternal.cmplayground.snackbar.manager

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf

val LocalSnackbarManager = compositionLocalOf<SnackbarManager> {
    error("SnackbarManager not provided")
}

interface SnackbarManager {
    val currentSnackbar: State<SnackbarData?>

    val isVisible: State<Boolean>

    suspend fun showSnackbar(data: SnackbarData)

    suspend fun dismiss()
}
