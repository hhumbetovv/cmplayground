package az.theternal.cmplayground.app.root

import az.theternal.cmplayground.app.root.RootContract.*
import az.theternal.cmplayground.core.base.BaseViewModel
import az.theternal.cmplayground.core.navigation.common.NavStackBlock
import az.theternal.cmplayground.feature.auth.model.AuthStatus
import az.theternal.cmplayground.feature.auth.navigation.FirstScreenRoute

class RootViewModel : BaseViewModel<Intent, State, Effect>() {
    override fun createState() = State()

    init {
        configureInitialRoute()
    }

    private fun configureInitialRoute() {
        // Can check auth/onboard state or deeplink uri
        updateBackStack {
            add(FirstScreenRoute.FirstScreen)
        }
    }

    override fun onIntentUpdate(intent: Intent) {
        when(intent) {
            is Intent.UpdateBackStack -> updateBackStack(intent.block)
            is Intent.UpdateAuthStatus -> updateAuthStatus(intent.authStatus)
        }
    }

    private fun updateBackStack(producer: NavStackBlock) {
        setState {
            copy(
                backStack = backStack
                    .toMutableList()
                    .apply(producer)
            )
        }
    }

    private fun updateAuthStatus(authStatus: AuthStatus) {
        setState { copy(authStatus = authStatus) }
    }
}
