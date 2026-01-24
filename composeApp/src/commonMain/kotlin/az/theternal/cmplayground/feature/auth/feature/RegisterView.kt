package az.theternal.cmplayground.feature.auth.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.navigation.navigator.LocalNavigator
import az.theternal.cmplayground.feature.auth.navigation.AuthRoute
import az.theternal.cmplayground.feature.post.navigation.PostRoute

@Composable
fun RegisterView() {
    val navigator = LocalNavigator.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Navigate To Home",
            modifier = Modifier.clickable(
                onClick = {
                    navigator.replaceAll(PostRoute.List)
                }
            )
        )

        Text(
            text = "Login",
            modifier = Modifier.clickable(
                onClick = {
                    navigator.popBackTo { it is AuthRoute.Login }
                }
            )
        )
    }
}