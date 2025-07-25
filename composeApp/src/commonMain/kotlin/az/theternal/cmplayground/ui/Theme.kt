@file:OptIn(ExperimentalSharedTransitionApi::class)

package az.theternal.cmplayground.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import az.theternal.cmplayground.ui.utils.noLocalProvidedFor

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this@SharedTransitionLayout
        ) {
            MaterialTheme(
                content = content
            )
        }
    }
}

val LocalSharedTransitionScope = compositionLocalOf <SharedTransitionScope> {
    noLocalProvidedFor("SharedTransitionScope")
}

val LocalAnimatedVisibilityScope = compositionLocalOf <AnimatedVisibilityScope> {
    noLocalProvidedFor("AnimatedVisibilityScope")
}

