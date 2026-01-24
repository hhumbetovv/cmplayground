package az.theternal.cmplayground.core.navigation.common

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

enum class SlideDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    NONE,
    ;
}

object NavAnimations {
    private val fadeIn = fadeIn(
        animationSpec = tween(NavigationDefaults.FADE_DURATION)
    )
    private val fadeOut = fadeOut(
        animationSpec = tween(NavigationDefaults.FADE_DURATION)
    )

    fun <T : NavKey> defaultPushTransition(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideIn = slideInHorizontally(
            animationSpec = tween(durationMillis = duration, easing = easing),
            initialOffsetX = { fullWidth -> fullWidth / NavigationDefaults.PUSH_OFFSET_DIVIDER }
        )
        val slideOut = slideOutHorizontally(
            animationSpec = tween(durationMillis = duration, easing = easing),
            targetOffsetX = { fullWidth -> -fullWidth / NavigationDefaults.POP_OFFSET_DIVIDER }
        )
        ContentTransform(
            targetContentEnter = slideIn + fadeIn,
            initialContentExit = slideOut + fadeOut,
        )
    }

    fun <T : NavKey> defaultPopTransition(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideIn = slideInHorizontally(
            animationSpec = tween(durationMillis = duration, easing = easing),
            initialOffsetX = { fullWidth -> -fullWidth / NavigationDefaults.POP_OFFSET_DIVIDER }
        )
        val slideOut = slideOutHorizontally(
            animationSpec = tween(durationMillis = duration, easing = easing),
            targetOffsetX = { fullWidth -> fullWidth / NavigationDefaults.PUSH_OFFSET_DIVIDER }
        )
        ContentTransform(
            targetContentEnter = slideIn + fadeIn,
            initialContentExit = slideOut + fadeOut,
        )
    }

    fun <T : NavKey> defaultPredictivePopTransition(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = {
        val slideIn = slideInHorizontally(
            animationSpec = tween(durationMillis = duration, easing = easing),
            initialOffsetX = { fullWidth ->
                if (it != 0) {
                    -fullWidth / NavigationDefaults.POP_OFFSET_DIVIDER + (it / NavigationDefaults.PUSH_OFFSET_DIVIDER)
                } else {
                    -fullWidth / NavigationDefaults.POP_OFFSET_DIVIDER
                }
            }
        )
        val slideOut = slideOutHorizontally(
            animationSpec = tween(durationMillis = duration, easing = easing),
            targetOffsetX = { fullWidth ->
                if (it != 0) {
                    fullWidth / NavigationDefaults.PUSH_OFFSET_DIVIDER + (it / 2)
                } else {
                    fullWidth / NavigationDefaults.PUSH_OFFSET_DIVIDER
                }
            }
        )

        ContentTransform(
            targetContentEnter = slideIn + fadeIn,
            initialContentExit = slideOut + fadeOut,
        )
    }

    fun <T : NavKey> slideTo(
        direction: AnimatedContentTransitionScope<Scene<T>>.() -> SlideDirection,
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = FastOutSlowInEasing,
    ): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {

        val enter: EnterTransition
        val exit: ExitTransition

        when (direction()) {
            SlideDirection.LEFT -> {
                enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    initialOffsetX = { fullWidth -> fullWidth }
                )
                exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    targetOffsetX = { fullWidth -> -fullWidth }
                )
            }

            SlideDirection.RIGHT -> {
                enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    initialOffsetX = { fullWidth -> -fullWidth }
                )
                exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    targetOffsetX = { fullWidth -> fullWidth }
                )
            }

            SlideDirection.UP -> {
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    initialOffsetY = { fullHeight -> fullHeight }
                )
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    targetOffsetY = { fullHeight -> -fullHeight }
                )
            }

            SlideDirection.DOWN -> {
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    initialOffsetY = { fullHeight -> -fullHeight }
                )
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    targetOffsetY = { fullHeight -> fullHeight }
                )
            }

            SlideDirection.NONE -> {
                enter = EnterTransition.None
                exit = ExitTransition.None
            }
        }

        ContentTransform(
            targetContentEnter = enter + fadeIn,
            initialContentExit = exit + fadeOut,
        )
    }
}

