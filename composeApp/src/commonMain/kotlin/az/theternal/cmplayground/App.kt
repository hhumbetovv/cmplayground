package az.theternal.cmplayground

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import az.theternal.cmplayground.ui.components.wheelpicker.samples.WheelPickerDemo
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        WheelPickerDemo()
    }
}
