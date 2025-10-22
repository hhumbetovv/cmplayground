package az.theternal.cmplayground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Color.White)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 20.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(modifier = Modifier.size(20.dp).background(Color.Red))
                UiIcon(
                    icon = IconData.HEART_01,
                    size = 20.dp,
                    color = Color.Blue,
                    modifier = Modifier.background(Color.Red.copy(alpha = 0.1f))
                )
                Text("Heart 20dp")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(modifier = Modifier.size(32.dp).background(Color.Red))
                UiIcon(
                    icon = IconData.SLIDER_02,
                    size = 32.dp,
                    color = Color.Green,
                    modifier = Modifier.background(Color.Blue.copy(alpha = 0.1f))
                )
                Text("Slider 32dp")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                UiIcon(
                    icon = IconData.CLOSE,
                    size = 150.dp,
                    color = Color.Blue,
                    modifier = Modifier.background(Color.Blue.copy(alpha = 0.1f))
                )
                Text("Close 150dp")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                UiIcon(
                    icon = IconData.CHEVRON_LEFT,
                    size = 24.dp,
                )
                Text("Chevron 24dp")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                UiIcon(
                    icon = IconData.CHEVRON_RIGHT,
                    size = 24.dp,
                )
                Text("Chevron 24dp")
            }
        }
    }
}