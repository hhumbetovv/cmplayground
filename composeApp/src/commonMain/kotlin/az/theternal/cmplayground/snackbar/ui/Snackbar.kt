package az.theternal.cmplayground.snackbar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.snackbar.manager.SnackbarAlign
import az.theternal.cmplayground.snackbar.manager.SnackbarData

@Composable
fun BoxScope.Snackbar(
    data: SnackbarData,
    isVisible: Boolean,
) {
    val targetOffset: (Int) -> Int = {
        when (data.align) {
            SnackbarAlign.TOP -> -it
            SnackbarAlign.BOTTOM -> it
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = targetOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = tween(500)
        ),
        exit = slideOutVertically(
            targetOffsetY = targetOffset,
            animationSpec = tween(1200, easing = FastOutLinearInEasing)
        ) + fadeOut(
            animationSpec = tween(500)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .align(
                alignment = when (data.align) {
                    SnackbarAlign.TOP -> Alignment.TopCenter
                    SnackbarAlign.BOTTOM -> Alignment.BottomCenter
                }
            )
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .statusBarsPadding()
                .fillMaxWidth()
                .background(
                    color = data.color,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.message,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )

                data.action?.invoke(this)
            }
        }
    }
}