package az.theternal.cmplayground

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.feature.counter.CounterView
import az.theternal.cmplayground.feature.notifier.NotifierView
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
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CounterView()
                    NotifierView()
                }
            }
        }
    }
}