package az.theternal.cmplayground.feature.counter.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@Composable
fun CountText(
    count: Int,
) {
    Text(
        count.toString(),
        fontSize = 72.sp
    )
}