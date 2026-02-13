package az.theternal.cmplayground.ui.components.wheelpicker.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale

/**
 * Creates a magnification effect for the center selection band.
 * Content outside the band renders normally, content inside is magnified.
 */
internal fun Modifier.magnifier(
    enabled: Boolean,
    magnification: Float,
    itemHeightPx: Int,
): Modifier {
    if (!enabled) return this

    return drawWithContent {
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
}
