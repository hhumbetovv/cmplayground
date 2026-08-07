package az.theternal.cmplayground.core.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp

/**
 * Draws a border around a component every time it recomposes, cycling colour as the count grows:
 * blue on the first pass, then green, amber, red the more often it re-runs.
 *
 * The point of this playground is a claim — "only the component whose slice changed recomposes" —
 * and this makes the claim falsifiable: type in the search field with the toggle on and watch the
 * borders of the filter row, the summary bar and the untouched rows stay put.
 *
 * The counter is a plain field, not state: it is written in [SideEffect] (after a successful
 * composition) and read in the draw phase, so tracking never itself causes a recomposition.
 */
val LocalRecompositionHighlight = compositionLocalOf { false }

private class RecompositionCount {
    var value: Int = 0
}

private val HighlightStroke = 2.dp
private val HighlightPalette = listOf(
    Color(0xFF2196F3),
    Color(0xFF4CAF50),
    Color(0xFFFFC107),
    Color(0xFFFF9800),
    Color(0xFFF44336),
)

@Composable
fun Modifier.trackRecompositions(): Modifier {
    if (!LocalRecompositionHighlight.current) return this

    val counter = remember { RecompositionCount() }
    SideEffect { counter.value++ }

    return drawWithContent {
        drawContent()
        val strokeWidth = HighlightStroke.toPx()
        inset(strokeWidth / 2) {
            drawRect(
                color = HighlightPalette[counter.value.coerceAtMost(HighlightPalette.lastIndex)],
                style = Stroke(width = strokeWidth),
            )
        }
    }
}
