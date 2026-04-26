package az.theternal.cmplayground.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.common.log
import az.theternal.cmplayground.core.navigation.guard.GuardResult
import az.theternal.cmplayground.core.navigation.guard.NavGuard
import az.theternal.cmplayground.feature.auth.model.AuthStatus
import az.theternal.cmplayground.feature.auth.provider.LocalAuthStatus

object AuthGuard : NavGuard {
    @Composable
    override fun evaluate(key: NavKey?): GuardResult {
        val authStatus = LocalAuthStatus.current
        return if (authStatus == AuthStatus.AUTHENTICATED) {
            GuardResult.Allow
        } else {
            log("reject")
            GuardResult.Block
        }
    }
}
