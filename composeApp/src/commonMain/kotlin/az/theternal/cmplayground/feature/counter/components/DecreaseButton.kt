package az.theternal.cmplayground.feature.counter.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DecreaseButton(
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
    ) {
        Text("Decrease")
    }
}