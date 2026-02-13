package az.theternal.cmplayground.ui.components.wheelpicker.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.ui.components.wheelpicker.WheelPicker
import az.theternal.cmplayground.ui.components.wheelpicker.rememberWheelPickerState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun WheelPickerDemo() {
    val items = (1..30).map { "Item $it" }
    val state = rememberWheelPickerState(initialIndex = 5)

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Selected: ${items[state.selectedIndex]}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        WheelPicker(
            itemCount = items.size,
            modifier = Modifier.width(120.dp),
            state = state,
            infiniteScroll = true,
            cylindrical = true,
            extendCount = 2,
            magnification = 1.1f,
            selectedBackground = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                )
            },
        ) { index ->
            Text(
                text = items[index],
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
