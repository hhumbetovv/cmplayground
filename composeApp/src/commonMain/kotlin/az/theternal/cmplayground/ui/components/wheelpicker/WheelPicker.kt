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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Align index to nearest multiple of itemCount + selected offset
private fun infiniteCenterIndex(itemCount: Int, selectedIndex: Int): Int {
    val center = Int.MAX_VALUE / 2
    return center - (center % itemCount) + selectedIndex
}

@Composable
fun WheelPicker(
    itemCount: Int,
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(),
    extendCount: Int = 2,
    infiniteScroll: Boolean = false,
    cylindrical: Boolean = false,
    magnification: Float = 1f,
    onSelectionChanged: ((Int) -> Unit)? = null,
    selectedBackground: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable (index: Int) -> Unit,
) {
    val virtualCount = if (infiniteScroll) Int.MAX_VALUE else itemCount

    // Start at the center of Int range, aligned to selectedIndex
    LaunchedEffect(infiniteScroll, itemCount) {
        if (infiniteScroll) {
            state.lazyListState.scrollToItem(
                infiniteCenterIndex(itemCount, state.selectedIndex)
            )
        }
    }

    // Re-center when scroll stops to prevent ever drifting too far
    LaunchedEffect(infiniteScroll, itemCount) {
        if (!infiniteScroll) return@LaunchedEffect
        snapshotFlow { state.lazyListState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    val current = state.lazyListState.firstVisibleItemIndex
                    val center = infiniteCenterIndex(itemCount, current % itemCount)
                    // Re-center if drifted more than 10% from middle
                    if (abs(current - center) > Int.MAX_VALUE / 10) {
                        val offset = state.lazyListState.firstVisibleItemScrollOffset
                        state.lazyListState.scrollToItem(center, offset)
                    }
                }
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

    // Selection tracking — continuous, deterministic, no frame lag
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
        if (selectedBackground != null) {
            Box(
                modifier = Modifier
                    .height(itemHeightDp)
                    .fillMaxWidth(),
                content = selectedBackground,
            )
        }

        val halfExtentPx = itemHeightPx * extendCount

        val hasMagnifier = magnification > 1f

        LazyColumn(
            modifier = Modifier
                .height(totalHeightDp)
                .fillMaxWidth()
                .then(
                    if (hasMagnifier) {
                        Modifier.drawWithContent {
                            val bandHeight = itemHeightPx.toFloat()
                            val bandTop = (size.height - bandHeight) / 2f
                            val bandBottom = bandTop + bandHeight

                            // Normal content outside the center band
                            clipRect(
                                top = bandTop,
                                bottom = bandBottom,
                                clipOp = ClipOp.Difference,
                            ) {
                                this@drawWithContent.drawContent()
                            }

                            // Magnified content inside the center band
                            clipRect(top = bandTop, bottom = bandBottom) {
                                scale(
                                    magnification,
                                    pivot = Offset(size.width / 2f, size.height / 2f),
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                        }
                    } else Modifier
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
                            // Deterministic position from scroll state —
                            // no layoutInfo lookup, no frame lag
                            val signedDistance =
                                ((virtualIndex - state.lazyListState.firstVisibleItemIndex) *
                                    itemHeightPx - state.lazyListState.firstVisibleItemScrollOffset)
                                    .toFloat()
                            val halfExtent = halfExtentPx.toFloat()

                            if (cylindrical) {
                                val radius = halfExtent
                                val halfPi = (PI / 2.0).toFloat()
                                val angleRad = (signedDistance / radius)
                                    .coerceIn(-halfPi, halfPi)
                                val angleDeg =
                                    angleRad * (180f / PI.toFloat())

                                rotationX = -angleDeg
                                cameraDistance = 12f * density.density

                                val arcY =
                                    radius * sin(angleRad.toDouble()).toFloat()
                                translationY = arcY - signedDistance

                                val fraction =
                                    (abs(signedDistance) / halfExtent).coerceIn(0f, 1f)
                                alpha = 1f - fraction * 0.5f
                            } else {
                                val fraction =
                                    (abs(signedDistance) / halfExtent)
                                        .coerceIn(0f, 1f)
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
