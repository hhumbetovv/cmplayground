package az.theternal.cmplayground.ui.bottomsheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DEFAULT_SCRIM_ALPHA = 0.45f
private const val DEFAULT_DURATION = 300


@Composable
fun BoxScope.BottomSheetHost(
    manager: BottomSheetManager,
    scrimAlpha: Float = DEFAULT_SCRIM_ALPHA,
    animationMillis: Int = DEFAULT_DURATION
) {
    val scrimColor = Color.Black
    val sheet = manager.sheet.collectAsState().value
    val scope = rememberCoroutineScope()

    var sheetHeightPx by remember(sheet) { mutableStateOf<Float?>(null) }
    val animationValue = remember { Animatable(0f) }

    var shouldRenderSheet by remember { mutableStateOf(sheet != null) }

    LaunchedEffect(sheet, sheetHeightPx) {
        if (sheet != null && sheetHeightPx != null && !sheet.requestDismiss) {
            shouldRenderSheet = true
            animationValue.snapTo(0f)
            animationValue.animateTo(1f, tween(animationMillis))
        }
    }

    LaunchedEffect(sheet?.requestDismiss) {
        if (sheet?.requestDismiss == true) {
            animationValue.animateTo(0f, tween(animationMillis))
            delay(animationMillis.toLong())
            shouldRenderSheet = false
            manager.onHidden()
        }
    }

    LaunchedEffect(sheet) {
        if (sheet == null && shouldRenderSheet) {
            animationValue.animateTo(0f, tween(animationMillis))
            delay(animationMillis.toLong())
            shouldRenderSheet = false
        }
    }

    LaunchedEffect(sheet) {
        if (sheet != null) {
            shouldRenderSheet = true
        }
    }

    var lastNonNullSheet by remember { mutableStateOf<SheetData?>(null) }

    val sheetData = remember(sheet) {
        if (sheet != null) {
            lastNonNullSheet = sheet
        }
        lastNonNullSheet ?: SheetData {}
    }

    if (shouldRenderSheet) {
        Box(
            Modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = animationValue.value * scrimAlpha))
                .then(
                    other = if(!sheetData.requestDismiss) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        manager.hide()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .offset {
                    IntOffset(
                        0,
                        ((1f - animationValue.value) * (sheetHeightPx ?: 1000f)).roundToInt()
                    )
                }
                .onSizeChanged { size ->
                    sheetHeightPx = size.height.toFloat()
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val height = sheetHeightPx ?: return@launch
                            val currentOffset = (1f - animationValue.value) * height
                            val newOffset = (currentOffset + delta).coerceIn(0f, height)
                            val newAnimationValue = 1f - (newOffset / height)
                            animationValue.snapTo(newAnimationValue)
                        }
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                            val height = sheetHeightPx ?: return@launch
                            val currentOffset = (1f - animationValue.value) * height
                            val shouldDismiss = currentOffset > height * 0.5f || velocity > 3000f

                            if (shouldDismiss) {
                                manager.hide()
                            } else {
                                animationValue.animateTo(1f, tween(animationMillis))
                            }
                        }
                    }
                )
        ) {
            sheetData.content()
        }
    }
}