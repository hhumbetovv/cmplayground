package az.theternal.cmplayground.ui.bottomsheet

import androidx.compose.runtime.Composable

data class SheetData(
    val dismissOnScrim: Boolean = true,
    val requestDismiss: Boolean = false,
    val content: @Composable () -> Unit,
)
