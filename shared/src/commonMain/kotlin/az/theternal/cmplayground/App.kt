package az.theternal.cmplayground

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import az.theternal.profile.impl.presentation.profile.ProfileScreen

@Composable
@Preview
fun App() {
    val dependencies = remember { Dependencies() }

    MaterialTheme {
        CompositionLocalProvider(
            values = dependencies.providedValues.toTypedArray()
        ) {
            ProfileScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
