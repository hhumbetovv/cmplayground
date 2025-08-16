package az.theternal.cmplayground.snackbar.config

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.snackbar.manager.SnackbarData
import az.theternal.cmplayground.snackbar.manager.SnackbarManager

@Composable
fun SnackbarHost(
    modifier: Modifier = Modifier,
    snackbarManager: SnackbarManager
) {
    val config = LocalSnackbarConfig.current

    val currentSnackbar by snackbarManager.currentSnackbar
    val isVisible by snackbarManager.isVisible

    var lastNonNullSnackbar by remember { mutableStateOf<SnackbarData?>(null) }

    val snackbarData = remember(currentSnackbar) {
        if (currentSnackbar != null) {
            lastNonNullSnackbar = currentSnackbar
        }
        lastNonNullSnackbar ?: SnackbarData.empty()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        config.backgroundContent(
            this,
            snackbarData,
            isVisible && currentSnackbar != null
        )

        config.snackbarContent(
            this,
            snackbarData,
            isVisible && currentSnackbar != null,
        )
    }
}

