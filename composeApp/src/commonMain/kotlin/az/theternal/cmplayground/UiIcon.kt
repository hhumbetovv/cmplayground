package az.theternal.cmplayground

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cmplayground.composeapp.generated.resources.Res
import cmplayground.composeapp.generated.resources.icons

@Composable
fun UiIcon(
    modifier: Modifier = Modifier,
    icon: IconData,
    color: Color = Color.Black,
    size: Dp = 24.dp,
) {
    Text(
        text = icon.value,
        fontFamily =  FontFamily(Font(resource = Res.font.icons)),
        color = color,
        textAlign = TextAlign.Center,
        fontSize = size.value.sp,
        lineHeight = size.value.sp,
        modifier = modifier
            .size(size)
    )
}
