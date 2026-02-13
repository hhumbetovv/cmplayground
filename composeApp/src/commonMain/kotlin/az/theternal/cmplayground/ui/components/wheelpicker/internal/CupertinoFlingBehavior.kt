package az.theternal.cmplayground.ui.components.wheelpicker.internal

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

/**
 * iOS-style fling behavior with smooth momentum decay and gentle snap.
 * Two-phase animation:
 * 1. Exponential decay with low friction (long glide)
 * 2. Spring snap to nearest item
 */
@Composable
internal fun rememberCupertinoFlingBehavior(
    lazyListState: LazyListState,
): FlingBehavior {
    return remember(lazyListState) {
        CupertinoSnapFlingBehavior(lazyListState)
    }
}

private class CupertinoSnapFlingBehavior(
    private val lazyListState: LazyListState,
) : FlingBehavior {

    @Suppress("SameReturnValue")
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < 50f) {
            snapToNearestItem()
            return 0f
        }

        // Phase 1: Momentum coast with gentle friction
        val decaySpec = exponentialDecay<Float>(
            frictionMultiplier = 1f,
            absVelocityThreshold = 0.5f,
        )

        var previousValue = 0f

        AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity,
        ).animateDecay(decaySpec) {
            val delta = value - previousValue
            previousValue = value
            val consumed = scrollBy(delta)

            // Hit list boundary
            if (abs(delta) > 0.5f && abs(consumed) < abs(delta) * 0.5f) {
                cancelAnimation()
            }
        }

        // Phase 2: Gentle spring snap
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
