package az.theternal.cmplayground

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import az.theternal.cmplayground.example.ExampleScreen
import az.theternal.cmplayground.ui.bottomsheet.BottomSheetHostProvider
import az.theternal.cmplayground.ui.bottomsheet.BottomSheetListener
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        BottomSheetHostProvider(
            listener = object : BottomSheetListener {
                override fun onHidden() {
                    println("BottomSheet Hidden")
                }

                override fun onShown() {
                    println("BottomSheet Shown")
                }
            }
        ) {
            ExampleScreen()
        }
    }
}

