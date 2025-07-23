package az.theternal.cmplayground.router.extensions

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import az.theternal.cmplayground.ui.LocalAnimatedVisibilityScope

inline fun <reified T : Any> NavGraphBuilder.animatedScopeComposable(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> {
        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides this
        ) {
            content(it)
        }
    }
}
