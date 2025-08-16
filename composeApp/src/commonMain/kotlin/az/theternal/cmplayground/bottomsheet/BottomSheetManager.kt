package az.theternal.cmplayground.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

interface BottomSheetManager {

    val sheet: StateFlow<SheetData?>

    fun show(
        dismissOnScrim: Boolean = true,
        content: @Composable BottomSheetScope.() -> Unit,
    )

    fun hide()

    fun onHidden()

}

interface BottomSheetListener {
    fun onShown() {}
    fun onHidden() {}
}

interface BottomSheetController {
    fun dismiss()
}

@Stable
class BottomSheetScope internal constructor(
    val controller: BottomSheetController
)

