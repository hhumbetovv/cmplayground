package az.theternal.cmplayground

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.snackbar.config.SnackbarConfig
import az.theternal.cmplayground.snackbar.config.SnackbarContent
import az.theternal.cmplayground.snackbar.config.SnackbarHostProvider
import az.theternal.cmplayground.snackbar.manager.SnackbarAlign
import az.theternal.cmplayground.snackbar.manager.SnackbarData
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        SnackbarHostProvider(
            config = object : SnackbarConfig() {
                override val snackbarContent: SnackbarContent = { data, isVisible ->
                    CustomizedSnackbar(data, isVisible)
                }


            }
        ) {
            ExampleScreen()
        }
    }
}

@Composable
fun BoxScope.CustomizedSnackbar(
    data: SnackbarData,
    isVisible: Boolean
) {

    val offSet: (Int) -> Int = {
        when (data.align) {
            SnackbarAlign.TOP -> -it
            SnackbarAlign.BOTTOM -> it
        }
    }

    val alignment = when(data.align) {
        SnackbarAlign.TOP -> Alignment.TopCenter
        SnackbarAlign.BOTTOM -> Alignment.BottomCenter
    }

    val safePadding = when(data.align) {
        SnackbarAlign.TOP -> Modifier.statusBarsPadding()
        SnackbarAlign.BOTTOM -> Modifier.navigationBarsPadding()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = offSet,
            animationSpec = tween(500)
        ),
        exit = slideOutVertically(
            targetOffsetY = offSet,
            animationSpec = tween(500)
        ),
        modifier = Modifier
            .align(alignment)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(data.color)
                .then(safePadding)
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                )
        ) {
            Text(
                text = data.message,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}