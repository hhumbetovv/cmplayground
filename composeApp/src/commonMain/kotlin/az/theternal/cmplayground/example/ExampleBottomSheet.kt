package az.theternal.cmplayground.example

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.ui.bottomsheet.BottomSheet
import az.theternal.cmplayground.ui.bottomsheet.BottomSheetScope

@Composable
fun BottomSheetScope.ExampleBottomSheet() {
    BottomSheet {
        Button(
            onClick = {
                controller.dismiss()
            },
            modifier = Modifier.padding(42.dp)
        ) {
            Text(
                text = "Bottom Sheet content",
            )
        }
    }
}
