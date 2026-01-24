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
import az.theternal.cmplayground.feature.auth.navigation.authGraph
import az.theternal.cmplayground.feature.post.navigation.postGraph

@Composable
fun RootView(
    viewModel: RootViewModel = viewModel { RootViewModel() },
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    NavigatorProvider(
        navigator = RootNavigator(
            stack = state.backStack,
            buildRootStack = {
                viewModel.postIntent(Intent.UpdateBackStack(it))
            }
        )
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
                authGraph()
                postGraph()

                entry<EmptyRoute> { }
            }
        )
    }
}
