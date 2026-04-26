package az.theternal.cmplayground.app.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import az.theternal.cmplayground.app.root.RootContract.Intent
import az.theternal.cmplayground.core.navigation.common.EmptyRoute
import az.theternal.cmplayground.core.navigation.common.NavAnimations
import az.theternal.cmplayground.core.navigation.navigator.NavigatorProvider
import az.theternal.cmplayground.core.navigation.result.ResultManagerProvider
import az.theternal.cmplayground.feature.auth.navigation.firstScreenGraph
import az.theternal.cmplayground.feature.auth.provider.AuthStatusProvider
import az.theternal.cmplayground.feature.post.navigation.secondScreenGraph

@Composable
fun RootView(
    viewModel: RootViewModel = viewModel { RootViewModel() },
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    ResultManagerProvider {
        NavigatorProvider(
            navigator = RootNavigator(
                stack = state.backStack,
                buildRootStack = {
                    viewModel.postIntent(Intent.UpdateBackStack(it))
                }
            )
        ) {
            AuthStatusProvider(
                authStatus = state.authStatus,
                onAuthStatusChanged = { authStatus ->
                    viewModel.postIntent(Intent.UpdateAuthStatus(authStatus))
                }
            ) {
                NavDisplay(
                    onBack = {
                        viewModel.postIntent(
                            Intent.UpdateBackStack { removeLastOrNull() }
                        )
                    },
                    backStack = state.backStack,
                    transitionSpec = NavAnimations.defaultPushTransition(),
                    popTransitionSpec = NavAnimations.defaultPopTransition(),
                    predictivePopTransitionSpec = NavAnimations.defaultPredictivePopTransition(),
                    entryProvider = entryProvider {
                        firstScreenGraph()
                        secondScreenGraph()

                        entry<EmptyRoute> { }
                    }
                )
            }
        }
    }
}
