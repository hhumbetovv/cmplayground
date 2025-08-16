package az.theternal.cmplayground.ui.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@Composable
fun BottomSheetHostProvider(
    listener: BottomSheetListener? = null,
    content: @Composable () -> Unit,
) {
    val manager = rememberBottomSheetManager(listener)

    CompositionLocalProvider(
        LocalBottomSheetManager provides manager
    ) {
        Box(Modifier.fillMaxSize()) {
            content()

            BottomSheetHost(manager)
        }
    }
}

val LocalBottomSheetManager = compositionLocalOf<BottomSheetManager> {
    error("BottomSheetManager not provided")
}
