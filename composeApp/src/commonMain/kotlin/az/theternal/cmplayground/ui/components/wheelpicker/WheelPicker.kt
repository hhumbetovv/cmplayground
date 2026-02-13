package az.theternal.cmplayground.ui.components.wheelpicker

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

@Composable
fun WheelPicker(
    itemCount: Int,
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(),
    extendCount: Int = 2,
    onSelectionChanged: ((Int) -> Unit)? = null,
    selectedBackground: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable (index: Int) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val itemPlaceable = subcompose("measure") {
            Box(modifier = Modifier.fillMaxWidth()) { content(0) }
        }.first().measure(constraints.copy(minHeight = 0, minWidth = 0))

        val itemHeight = itemPlaceable.height
        if (itemHeight == 0) {
            return@SubcomposeLayout layout(0, 0) {}
        }

        val visibleCount = extendCount * 2 + 1
        val totalHeight = itemHeight * visibleCount
        val width = constraints.maxWidth.coerceIn(constraints.minWidth, constraints.maxWidth)

        val wheelPlaceable = subcompose("content") {
            WheelPickerContent(
                itemCount = itemCount,
                itemHeightPx = itemHeight,
                extendCount = extendCount,
                state = state,
                onSelectionChanged = onSelectionChanged,
                selectedBackground = selectedBackground,
                content = content,
            )
        }.first().measure(Constraints.fixed(width, totalHeight))

        layout(width, totalHeight) {
            wheelPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun WheelPickerContent(
    itemCount: Int,
    itemHeightPx: Int,
    extendCount: Int,
    state: WheelPickerState,
    onSelectionChanged: ((Int) -> Unit)?,
    selectedBackground: @Composable (BoxScope.() -> Unit)?,
    content: @Composable (index: Int) -> Unit,
) {
    val density = LocalDensity.current
    val itemHeightDp = with(density) { itemHeightPx.toDp() }
    val totalHeightDp = itemHeightDp * (extendCount * 2 + 1)

    // Selection tracking via scroll state observation
    LaunchedEffect(state.lazyListState) {
        snapshotFlow {
            if (state.lazyListState.isScrollInProgress) return@snapshotFlow -1

            val layoutInfo = state.lazyListState.layoutInfo
            val viewportCenter =
                layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2

            layoutInfo.visibleItemsInfo.minByOrNull {
                abs((it.offset + it.size / 2) - viewportCenter)
            }?.index ?: -1
        }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .drop(1) // skip initial emission
            .collect { index ->
                state.selectedIndex = index
                onSelectionChanged?.invoke(index)
            }
    }

    Box(
        modifier = Modifier
            .height(totalHeightDp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // Selected item background (drawn behind the list)
        if (selectedBackground != null) {
            Box(
                modifier = Modifier
                    .height(itemHeightDp)
                    .fillMaxWidth(),
                content = selectedBackground,
            )
        }

        val halfExtentPx = itemHeightPx * extendCount

        LazyColumn(
            modifier = Modifier
                .height(totalHeightDp)
                .fillMaxWidth(),
            state = state.lazyListState,
            contentPadding = PaddingValues(vertical = itemHeightDp * extendCount),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state.lazyListState),
        ) {
            items(itemCount) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeightDp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val info = state.lazyListState.layoutInfo
                            val viewportCenter =
                                info.viewportStartOffset + info.viewportSize.height / 2f

                            val itemInfo = info.visibleItemsInfo.find { it.index == index }
                            if (itemInfo != null) {
                                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                val distance = abs(itemCenter - viewportCenter)
                                val fraction =
                                    (distance / halfExtentPx.toFloat()).coerceIn(0f, 1f)

                                // Gradual alpha fade
                                alpha = 1f - fraction * 0.6f

                                // Subtle scale reduction for depth
                                val s = 1f - fraction * 0.12f
                                scaleX = s
                                scaleY = s
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    content(index)
                }
            }
        }
    }
}
