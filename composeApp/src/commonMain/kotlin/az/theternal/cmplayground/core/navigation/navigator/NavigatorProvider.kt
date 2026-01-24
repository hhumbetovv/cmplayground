package az.theternal.cmplayground.core.navigation.navigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator not provided in composable tree")
}

@Composable
fun NavigatorProvider(
    navigator: Navigator,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalNavigator provides navigator,
        content = content,
    )
}