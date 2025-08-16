package az.theternal.cmplayground.snackbar.config

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.snackbar.manager.LocalSnackbarManager
import az.theternal.cmplayground.snackbar.manager.rememberSnackbarManager

@Composable
fun SnackbarHostProvider(
    config: SnackbarConfig = DefaultSnackbarConfig(),
    content: @Composable () -> Unit,
) {
    val snackbarManager = rememberSnackbarManager()

    CompositionLocalProvider(
        LocalSnackbarManager provides snackbarManager,
        LocalSnackbarConfig provides config,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            SnackbarHost(
                snackbarManager = snackbarManager
            )
        }
    }
}