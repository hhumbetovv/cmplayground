package az.theternal.cmplayground.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.ui.bottomsheet.LocalBottomSheetManager

@Composable
fun ExampleScreen() {
    val bottomSheetManager = LocalBottomSheetManager.current

    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    bottomSheetManager.show {
                        ExampleBottomSheet()
                    }
                }
            ) {
                Text("Show BottomSheet")
            }
        }
    }
}
