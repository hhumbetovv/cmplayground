package az.theternal.cmplayground.feature.post.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.core.navigation.guard.routeEntry
import az.theternal.cmplayground.feature.auth.navigation.AuthGuard
import az.theternal.cmplayground.feature.post.feature.SecondScreenView

fun EntryProviderScope<NavKey>.secondScreenGraph() {
    routeEntry<SecondScreenRoute.SecondScreen>(
        guards = listOf(AuthGuard),
    ) {
        SecondScreenView()
    }
}
