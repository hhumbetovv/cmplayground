package az.theternal.cmplayground.feature.post.feature

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
import az.theternal.cmplayground.feature.auth.navigation.FirstScreenRoute
import az.theternal.cmplayground.feature.auth.provider.LocalAuthStatusUpdater

@Composable
fun SecondScreenView() {
    val navigator = LocalNavigator.current
    val setAuthStatus = LocalAuthStatusUpdater.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Second Screen")

        Button(
            onClick = {
                setAuthStatus(AuthStatus.UNAUTHENTICATED)
            }
        ) {
            Text("Set Unauthenticated")
        }

        Button(
            onClick = {
                navigator.popBackTo { it is FirstScreenRoute.FirstScreen }
            }
        ) {
            Text("Go First")
        }
    }
}
