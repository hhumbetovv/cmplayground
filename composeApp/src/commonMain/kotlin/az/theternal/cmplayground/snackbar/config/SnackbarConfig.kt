package az.theternal.cmplayground.snackbar.config

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import az.theternal.cmplayground.snackbar.manager.SnackbarData
import az.theternal.cmplayground.snackbar.ui.Snackbar
import az.theternal.cmplayground.snackbar.ui.SnackbarLight

typealias SnackbarContent = @Composable BoxScope.(SnackbarData, Boolean) -> Unit

abstract class SnackbarConfig {

    open val backgroundContent: SnackbarContent = { data, isVisible ->
        SnackbarLight(data, isVisible)
    }

    open val snackbarContent: SnackbarContent = { data, isVisible ->
        Snackbar(data, isVisible)
    }

}

class DefaultSnackbarConfig : SnackbarConfig()

val LocalSnackbarConfig = staticCompositionLocalOf<SnackbarConfig> {
    DefaultSnackbarConfig()
}