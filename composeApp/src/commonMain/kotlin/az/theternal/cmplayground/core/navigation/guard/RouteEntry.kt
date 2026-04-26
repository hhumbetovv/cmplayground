package az.theternal.cmplayground.core.navigation.guard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.core.navigation.navigator.LocalNavigator

inline fun <reified T : NavKey> EntryProviderScope<NavKey>.routeEntry(
    guards: List<NavGuard> = emptyList(),
    noinline content: @Composable (T) -> Unit,
) {
    entry<T> { routeKey ->
        GuardedRouteContent(
            key = routeKey,
            guards = guards,
        ) {
            content(routeKey)
        }
    }
}

@Composable
fun GuardedRouteContent(
    key: NavKey?,
    guards: List<NavGuard>,
    content: @Composable () -> Unit,
) {
    if (guards.isEmpty()) {
        content()
        return
    }

    val navigator = LocalNavigator.current
    val resultCache = rememberGuardResultCache(key)
    if (!resultCache.isEvaluated) {
        resultCache.result = evaluateGuards(
            key = key,
            guards = guards,
        )
        resultCache.isEvaluated = true
    }
    val result = resultCache.result

    when (result) {
        GuardResult.Allow -> content()
        GuardResult.Block -> {
            LaunchedEffect(key, result) {
                navigator.popBack()
            }
        }
        is GuardResult.Redirect -> {
            if (result.destination == key) {
                return
            }

            LaunchedEffect(key, result.destination) {
                navigator.replaceAll(result.destination)
            }
        }
    }
}

@Composable
private fun rememberGuardResultCache(
    key: NavKey?,
): GuardResultCache {
    return androidx.compose.runtime.remember(key) { GuardResultCache() }
}

private class GuardResultCache(
    var isEvaluated: Boolean = false,
    var result: GuardResult = GuardResult.Allow,
)

@Composable
private fun evaluateGuards(
    key: NavKey?,
    guards: List<NavGuard>,
): GuardResult {
    for (guard in guards) {
        when (val result = guard.evaluate(key)) {
            GuardResult.Allow -> Unit
            GuardResult.Block -> return GuardResult.Block
            is GuardResult.Redirect -> return result
        }
    }
    return GuardResult.Allow
}
