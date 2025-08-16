package az.theternal.cmplayground.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BottomSheetManagerImpl internal constructor(
    private val listener: BottomSheetListener? = null,
) : BottomSheetManager {
    private val _sheet = MutableStateFlow<SheetData?>(null)
    override val sheet: StateFlow<SheetData?> = _sheet

    override fun show(
        dismissOnScrim: Boolean,
        content: @Composable BottomSheetScope.() -> Unit,
    ) {
        val controller = object : BottomSheetController {
            override fun dismiss() {
                hide()
            }
        }
        _sheet.value = SheetData(
            dismissOnScrim = dismissOnScrim,
            content = { BottomSheetScope(controller).content() }
        )
        listener?.onShown()
    }

    override fun hide() {
        if (_sheet.value != null) {
            _sheet.value = _sheet.value?.copy(requestDismiss = true)
        }
    }

    override fun onHidden() {
        _sheet.value = null
        listener?.onHidden()
    }
}

@Composable
fun rememberBottomSheetManager(
    listener: BottomSheetListener? = null,
) = remember {
    BottomSheetManagerImpl(
        listener
    )
}