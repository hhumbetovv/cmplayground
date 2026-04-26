package az.theternal.cmplayground.feature.auth.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.navigation.navigator.LocalNavigator
import az.theternal.cmplayground.feature.auth.model.AuthStatus
import az.theternal.cmplayground.feature.auth.provider.LocalAuthStatus
import az.theternal.cmplayground.feature.auth.provider.LocalAuthStatusUpdater
import az.theternal.cmplayground.feature.post.navigation.SecondScreenRoute

@Composable
fun FirstScreenView() {
    val navigator = LocalNavigator.current
    val authStatus = LocalAuthStatus.current
    val setAuthStatus = LocalAuthStatusUpdater.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("First Screen")

        Button(
            onClick = {
                setAuthStatus(
                    if (authStatus == AuthStatus.AUTHENTICATED) {
                        AuthStatus.UNAUTHENTICATED
                    } else {
                        AuthStatus.AUTHENTICATED
                    }
                )
            }
        ) {
            Text("Toggle Auth: $authStatus")
        }

        Button(
            onClick = {
                navigator.push(SecondScreenRoute.SecondScreen)
            }
        ) {
            Text("Go Second")
        }
    }
}
