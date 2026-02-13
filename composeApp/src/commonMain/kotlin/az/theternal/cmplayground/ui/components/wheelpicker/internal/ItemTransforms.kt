package az.theternal.cmplayground.ui.components.wheelpicker.internal

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.Density
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Applies cylindrical 3D transformation to item.
 * Items follow a cylinder surface with rotation and arc translation.
 */
internal fun GraphicsLayerScope.applyCylindricalTransform(
    signedDistance: Float,
    halfExtent: Float,
    density: Density,
) {
    val radius = halfExtent
    val halfPi = (PI / 2.0).toFloat()
    val angleRad = (signedDistance / radius).coerceIn(-halfPi, halfPi)
    val angleDeg = angleRad * (180f / PI.toFloat())

    rotationX = -angleDeg
    cameraDistance = 12f * density.density

    // Arc translation: move items along cylinder surface
    val arcY = radius * sin(angleRad.toDouble()).toFloat()
    translationY = arcY - signedDistance

    val fraction = (abs(signedDistance) / halfExtent).coerceIn(0f, 1f)
    alpha = 1f - fraction * 0.5f
}

/**
 * Applies flat scale transformation to item (default mode).
 * Items scale down slightly as they move away from center.
 */
internal fun GraphicsLayerScope.applyFlatTransform(
    signedDistance: Float,
    halfExtent: Float,
) {
    val fraction = (abs(signedDistance) / halfExtent).coerceIn(0f, 1f)
    alpha = 1f - fraction * 0.6f
    val scale = 1f - fraction * 0.12f
    scaleX = scale
    scaleY = scale
}
