package az.theternal.cmplayground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.snackbar.manager.LocalSnackbarManager
import az.theternal.cmplayground.snackbar.manager.SnackbarAlign
import az.theternal.cmplayground.snackbar.manager.SnackbarData

@Composable
fun ExampleScreen() {

    val snackbarManager = LocalSnackbarManager.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Button(
            onClick = {
                snackbarManager.showSnackbar(
                    SnackbarData(
                        message = "Some Error Message",
                        color = Color.Red,
                        align = SnackbarAlign.TOP
                    )
                )
            }
        ) {
            Text("Show Snackbar at top")
        }

        Button(
            onClick = {
                snackbarManager.showSnackbar(
                    SnackbarData(
                        message = "Some Info Message",
                        color = Color.Blue,
                    )
                )
            }
        ) {
            Text("Show Snackbar at bottom")
        }
    }
}