package az.theternal.cmplayground

import androidx.compose.runtime.Composable
import az.theternal.cmplayground.router.Router
import az.theternal.cmplayground.ui.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {

    AppTheme {
        Router()
    }
}