package az.theternal.cmplayground

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import az.theternal.cmplayground.feature.counter.CounterView
import org.jetbrains.compose.ui.tooling.preview.Preview

val LocalSnackbar = compositionLocalOf { SnackbarHostState() }

@Composable
@Preview
fun App() {
    val snackbarHostState = remember { SnackbarHostState() }

    MaterialTheme {
        CompositionLocalProvider(
            LocalSnackbar provides snackbarHostState
        ) {
            Scaffold(
                snackbarHost = {
                    LocalSnackbar.current
                }
            ) {
                CounterView()
            }
        }
    }
}