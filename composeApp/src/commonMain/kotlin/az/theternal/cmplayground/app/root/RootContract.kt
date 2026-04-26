package az.theternal.cmplayground.app.root

import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.core.base.ViewEffect
import az.theternal.cmplayground.core.base.ViewIntent
import az.theternal.cmplayground.core.base.ViewState
import az.theternal.cmplayground.core.navigation.common.EmptyRoute
import az.theternal.cmplayground.core.navigation.common.NavStackBlock
import az.theternal.cmplayground.feature.auth.model.AuthStatus

sealed interface RootContract {
    sealed interface Intent : ViewIntent {
        data class UpdateBackStack(
            val block: NavStackBlock,
        ) : Intent

        data class UpdateAuthStatus(
            val authStatus: AuthStatus,
        ) : Intent
    }

    data class State(
        val backStack: List<NavKey> = listOf(EmptyRoute),
        val authStatus: AuthStatus = AuthStatus.UNAUTHENTICATED,
    ) : ViewState

    sealed interface Effect : ViewEffect { }
}
