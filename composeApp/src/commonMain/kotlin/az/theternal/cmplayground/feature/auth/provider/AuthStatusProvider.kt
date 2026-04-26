package az.theternal.cmplayground.feature.auth.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import az.theternal.cmplayground.feature.auth.model.AuthStatus

val LocalAuthStatus = compositionLocalOf { AuthStatus.UNAUTHENTICATED }
val LocalAuthStatusUpdater = compositionLocalOf<(AuthStatus) -> Unit> { {} }

@Composable
fun AuthStatusProvider(
    authStatus: AuthStatus,
    onAuthStatusChanged: (AuthStatus) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAuthStatus provides authStatus,
        LocalAuthStatusUpdater provides onAuthStatusChanged,
        content = content,
    )
}
