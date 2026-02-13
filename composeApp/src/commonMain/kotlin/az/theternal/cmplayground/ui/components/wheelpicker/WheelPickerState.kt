package az.theternal.cmplayground.ui.components.wheelpicker

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class WheelPickerState(initialIndex: Int = 0) {

    internal val lazyListState = LazyListState(firstVisibleItemIndex = initialIndex)

    var selectedIndex by mutableStateOf(initialIndex)
        internal set

    val isScrollInProgress: Boolean
        get() = lazyListState.isScrollInProgress

    suspend fun scrollToItem(index: Int) {
        lazyListState.scrollToItem(index)
    }

    suspend fun animateScrollToItem(index: Int) {
        lazyListState.animateScrollToItem(index)
    }
}

@Composable
fun rememberWheelPickerState(initialIndex: Int = 0): WheelPickerState {
    return remember { WheelPickerState(initialIndex) }
}
