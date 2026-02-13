package az.theternal.cmplayground.ui.components.wheelpicker

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

private const val INFINITE_MULTIPLIER = 1000

@Composable
fun WheelPicker(
    itemCount: Int,
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(),
    extendCount: Int = 2,
    infiniteScroll: Boolean = false,
    onSelectionChanged: ((Int) -> Unit)? = null,
    selectedBackground: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable (index: Int) -> Unit,
) {
    val virtualCount = if (infiniteScroll) itemCount * INFINITE_MULTIPLIER else itemCount

    // For infinite scroll, start at the middle so user can scroll both directions
    LaunchedEffect(infiniteScroll, itemCount) {
        if (infiniteScroll) {
            val midBase = (INFINITE_MULTIPLIER / 2) * itemCount
            val targetVirtual = midBase + state.selectedIndex
            state.lazyListState.scrollToItem(targetVirtual)
        }
    }

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
                virtualCount = virtualCount,
                itemHeightPx = itemHeight,
                extendCount = extendCount,
                infiniteScroll = infiniteScroll,
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
    state: WheelPickerState,
    onSelectionChanged: ((Int) -> Unit)?,
    selectedBackground: @Composable (BoxScope.() -> Unit)?,
    content: @Composable (index: Int) -> Unit,
) {
    val density = LocalDensity.current
    val itemHeightDp = with(density) { itemHeightPx.toDp() }
    val totalHeightDp = itemHeightDp * (extendCount * 2 + 1)

    // Selection tracking
    LaunchedEffect(state.lazyListState, itemCount) {
        snapshotFlow {
            if (state.lazyListState.isScrollInProgress) return@snapshotFlow -1

            val layoutInfo = state.lazyListState.layoutInfo
            val viewportCenter =
                layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2

            val virtualIndex = layoutInfo.visibleItemsInfo.minByOrNull {
                abs((it.offset + it.size / 2) - viewportCenter)
            }?.index ?: -1

            if (virtualIndex >= 0 && infiniteScroll) virtualIndex % itemCount
            else virtualIndex
        }
            .filter { it >= 0 }
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
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
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
            flingBehavior = flingBehavior,
        ) {
            items(virtualCount) { virtualIndex ->
                val realIndex = if (infiniteScroll) virtualIndex % itemCount else virtualIndex
                Box(
                    modifier = Modifier
                        .height(itemHeightDp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val info = state.lazyListState.layoutInfo
                            val viewportCenter =
                                info.viewportStartOffset + info.viewportSize.height / 2f

                            val itemInfo =
                                info.visibleItemsInfo.find { it.index == virtualIndex }
                            if (itemInfo != null) {
                                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                val distance = abs(itemCenter - viewportCenter)
                                val fraction =
                                    (distance / halfExtentPx.toFloat()).coerceIn(0f, 1f)

                                alpha = 1f - fraction * 0.6f

                                val s = 1f - fraction * 0.12f
                                scaleX = s
                                scaleY = s
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

// --- Cupertino-style fling behavior: long momentum coast + gentle snap ---

@Composable
private fun rememberCupertinoFlingBehavior(
    lazyListState: LazyListState,
): FlingBehavior {
    return remember(lazyListState) {
        CupertinoSnapFlingBehavior(lazyListState)
    }
}

private class CupertinoSnapFlingBehavior(
    private val lazyListState: LazyListState,
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // No velocity → just snap in place
        if (abs(initialVelocity) < 50f) {
            snapToNearestItem()
            return 0f
        }

        // Phase 1: Momentum coast with low friction (iOS-like long glide)
        val decaySpec = exponentialDecay<Float>(
            frictionMultiplier = 1f,
            absVelocityThreshold = 0.5f,
        )

        var previousValue = 0f
        var remainingVelocity = initialVelocity

        AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity,
        ).animateDecay(decaySpec) {
            val delta = value - previousValue
            previousValue = value
            val consumed = scrollBy(delta)
            remainingVelocity = velocity

            // Hit list boundary
            if (abs(delta) > 0.5f && abs(consumed) < abs(delta) * 0.5f) {
                cancelAnimation()
            }
        }

        // Phase 2: Gentle spring snap to nearest item center
        snapToNearestItem()

        return 0f
    }

    private suspend fun ScrollScope.snapToNearestItem() {
        val layoutInfo = lazyListState.layoutInfo
        val viewportCenter =
            layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2

        val closestItem = layoutInfo.visibleItemsInfo.minByOrNull {
            abs((it.offset + it.size / 2) - viewportCenter)
        } ?: return

        val targetOffset =
            (closestItem.offset + closestItem.size / 2 - viewportCenter).toFloat()

        if (abs(targetOffset) < 0.5f) return

        var previousSnapValue = 0f
        animate(
            initialValue = 0f,
            targetValue = targetOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 400f,
            ),
        ) { value, _ ->
            val delta = value - previousSnapValue
            previousSnapValue = value
            scrollBy(delta)
        }
    }
}
