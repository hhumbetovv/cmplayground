package az.theternal.cmplayground.snackbar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.snackbar.manager.SnackbarAlign
import az.theternal.cmplayground.snackbar.manager.SnackbarData


@Composable
fun SnackbarLight(
    data: SnackbarData,
    isVisible: Boolean,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 800,
                easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 800,
                easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            )
        ),
        modifier = Modifier
            .fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 24.dp)
        ) {
            val screenWidth = size.width
            val screenHeight = size.height
            val lightRadius = screenWidth * 1.5f
            val lightCenterX = screenWidth / 2f
            val spacing = lightRadius * 0.6f
            val lightCenterY = when (data.align) {
                SnackbarAlign.TOP -> -spacing
                SnackbarAlign.BOTTOM -> (screenHeight + spacing)
            }

            val mainLightBrush = Brush.radialGradient(
                colors = listOf(
                    data.color.copy(alpha = 0.6f),
                    Color.Transparent
                ),
                center = Offset(lightCenterX, lightCenterY),
                radius = lightRadius
            )

            drawCircle(
                brush = mainLightBrush,
                radius = lightRadius,
                center = Offset(lightCenterX, lightCenterY)
            )

            for (i in 1..3) {
                val layerRadius = lightRadius + (i * 100.dp.toPx())
                val layerAlpha = 0.015f / (i * 1.5f)

                val layerBrush = Brush.radialGradient(
                    colors = listOf(
                        data.color.copy(alpha = layerAlpha),
                        data.color.copy(alpha = layerAlpha * 0.7f),
                        Color.Transparent
                    ),
                    center = Offset(lightCenterX, lightCenterY),
                    radius = layerRadius
                )

                drawCircle(
                    brush = layerBrush,
                    radius = layerRadius,
                    center = Offset(lightCenterX, lightCenterY)
                )
            }
        }
    }
}