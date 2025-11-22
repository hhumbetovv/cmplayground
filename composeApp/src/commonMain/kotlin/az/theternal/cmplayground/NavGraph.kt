package az.theternal.cmplayground

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import cmplayground.composeapp.generated.resources.Res
import cmplayground.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

@Composable
fun CMPlaygroundNavHost() {
    val backStack = remember { mutableStateListOf<Destination>(Destination.Home) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.popBackStack() },
        entryProvider = { destination ->
            when (destination) {
                Destination.Home -> NavEntry(destination) {
                    HomeScreen(
                        onShowGreeting = { message ->
                            backStack.add(Destination.GreetingDetails(message))
                        }
                    )
                }

                is Destination.GreetingDetails -> NavEntry(destination) {
                    GreetingDetailsScreen(
                        message = destination.message,
                        onBack = { backStack.popBackStack() }
                    )
                }
            }
        }
    )
}

@Composable
private fun HomeScreen(
    onShowGreeting: (String) -> Unit,
) {
    var tapCount by remember { mutableIntStateOf(0) }

    Surface {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nav3 ile tek tuşla selamlama ekranına geç",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    tapCount++
                    val greeting = Greeting().greet()
                    val message = "#" + tapCount + " • " + greeting
                    onShowGreeting(message)
                }
            ) {
                Text("Selamı göster")
            }
        }
    }
}

@Composable
private fun GreetingDetailsScreen(
    message: String,
    onBack: () -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.compose_multiplatform),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Compose: $message",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(onClick = onBack) {
                Text("Geri dön")
            }
        }
    }
}

private sealed interface Destination: NavKey {
    data object Home : Destination
    data class GreetingDetails(val message: String) : Destination
}

private fun MutableList<Destination>.popBackStack() {
    if (size > 1) {
        removeAt(lastIndex)
    }
}
