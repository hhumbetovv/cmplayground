package az.theternal.cmplayground.app.root

import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.core.base.ViewEffect
import az.theternal.cmplayground.core.base.ViewIntent
import az.theternal.cmplayground.core.base.ViewState
import az.theternal.cmplayground.core.navigation.common.EmptyRoute
import az.theternal.cmplayground.core.navigation.common.NavStackBlock

sealed interface RootContract {
    sealed interface Intent : ViewIntent {
        data class UpdateBackStack(
            val block: NavStackBlock,
        ) : Intent
    }

    data class State(
        val backStack: List<NavKey> = listOf(EmptyRoute),
    ) : ViewState

    sealed interface Effect : ViewEffect { }
}