package az.theternal.cmplayground.ui.components.wheelpicker

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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import az.theternal.cmplayground.ui.components.wheelpicker.internal.applyCylindricalTransform
import az.theternal.cmplayground.ui.components.wheelpicker.internal.applyFlatTransform
import az.theternal.cmplayground.ui.components.wheelpicker.internal.infiniteCenterIndex
import az.theternal.cmplayground.ui.components.wheelpicker.internal.magnifier
import az.theternal.cmplayground.ui.components.wheelpicker.internal.rememberCupertinoFlingBehavior
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

/**
 * A customizable wheel picker with iOS-style behavior.
 *
 * Features:
 * - Dynamic height based on item content
 * - Smooth scroll with Cupertino-style fling behavior
 * - Optional cylindrical 3D effect
 * - Optional magnification for selected item
 * - Infinite scroll support
 * - Customizable selection background
 *
 * @param itemCount Total number of items
 * @param state State object to control and observe the picker
 * @param extendCount Number of items visible above and below center (total visible = extendCount * 2 + 1)
 * @param infiniteScroll Enable infinite scrolling (wraps at boundaries)
 * @param cylindrical Enable 3D cylindrical effect (iOS picker style)
 * @param magnification Scale factor for center item (1.0 = no magnification, >1.0 = enlarged)
 * @param onSelectionChanged Callback when selected item changes
 * @param selectedBackground Composable content for the selection indicator background
 * @param content Composable content for each item (receives item index)
 */
@Composable
fun WheelPicker(
    itemCount: Int,
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(),
    extendCount: Int = 2,
    infiniteScroll: Boolean = false,
    cylindrical: Boolean = false,
    magnification: Float = 1.1f,
    onSelectionChanged: ((Int) -> Unit)? = null,
    selectedBackground: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable (index: Int) -> Unit,
) {
    val virtualCount = if (infiniteScroll) Int.MAX_VALUE else itemCount

    // Initialize infinite scroll at center position
    LaunchedEffect(infiniteScroll, itemCount) {
        if (infiniteScroll) {
            state.lazyListState.scrollToItem(
                infiniteCenterIndex(itemCount, state.selectedIndex)
            )
        }
    }

    // Re-center when drifted too far (prevents Int overflow)
    LaunchedEffect(infiniteScroll, itemCount) {
        if (!infiniteScroll) return@LaunchedEffect
        snapshotFlow { state.lazyListState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    val current = state.lazyListState.firstVisibleItemIndex
                    val center = infiniteCenterIndex(itemCount, current % itemCount)
                    if (abs(current - center) > Int.MAX_VALUE / 10) {
                        val offset = state.lazyListState.firstVisibleItemScrollOffset
                        state.lazyListState.scrollToItem(center, offset)
                    }
                }
            }
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        // Measure first item to determine dynamic height
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
                virtualCount = virtualCount,
                itemHeightPx = itemHeight,
                extendCount = extendCount,
                infiniteScroll = infiniteScroll,
                cylindrical = cylindrical,
                magnification = magnification,
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
    virtualCount: Int,
    itemHeightPx: Int,
    extendCount: Int,
    infiniteScroll: Boolean,
    cylindrical: Boolean,
    magnification: Float,
    state: WheelPickerState,
    onSelectionChanged: ((Int) -> Unit)?,
    selectedBackground: @Composable (BoxScope.() -> Unit)?,
    content: @Composable (index: Int) -> Unit,
) {
    val density = LocalDensity.current
    val itemHeightDp = with(density) { itemHeightPx.toDp() }
    val totalHeightDp = itemHeightDp * (extendCount * 2 + 1)

    // Track selection changes continuously (no frame lag)
    LaunchedEffect(state.lazyListState, itemCount, itemHeightPx) {
        snapshotFlow {
            val firstIndex = state.lazyListState.firstVisibleItemIndex
            val scrollOffset = state.lazyListState.firstVisibleItemScrollOffset
            val virtualIndex = firstIndex +
                if (scrollOffset > itemHeightPx / 2) 1 else 0

            if (infiniteScroll) virtualIndex % itemCount else virtualIndex
        }
            .distinctUntilChanged()
            .drop(1)
            .collect { index ->
                state.selectedIndex = index
                onSelectionChanged?.invoke(index)
            }
    }

    val flingBehavior = rememberCupertinoFlingBehavior(state.lazyListState)

    Box(
        modifier = Modifier
            .height(totalHeightDp)
            .fillMaxWidth()
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        // Selection indicator background
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
                .fillMaxWidth()
                .magnifier(
                    enabled = magnification > 1f,
                    magnification = magnification,
                    itemHeightPx = itemHeightPx,
                ),
            state = state.lazyListState,
            contentPadding = PaddingValues(vertical = itemHeightDp * extendCount),
            flingBehavior = flingBehavior,
        ) {
            items(virtualCount) { virtualIndex ->
                val realIndex = if (infiniteScroll) virtualIndex % itemCount else virtualIndex

                Box(
                    modifier = Modifier
                        .height(itemHeightDp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Deterministic position calculation (no layoutInfo lookup)
                            val signedDistance =
                                ((virtualIndex - state.lazyListState.firstVisibleItemIndex) *
                                    itemHeightPx - state.lazyListState.firstVisibleItemScrollOffset)
                                    .toFloat()

                            if (cylindrical) {
                                applyCylindricalTransform(
                                    signedDistance = signedDistance,
                                    halfExtent = halfExtentPx.toFloat(),
                                    density = density,
                                )
                            } else {
                                applyFlatTransform(
                                    signedDistance = signedDistance,
                                    halfExtent = halfExtentPx.toFloat(),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    content(realIndex)
                }
            }
        }
    }
}
