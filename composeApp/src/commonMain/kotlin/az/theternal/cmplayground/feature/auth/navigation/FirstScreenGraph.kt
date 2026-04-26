package az.theternal.cmplayground.feature.auth.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.feature.auth.feature.FirstScreenView

fun EntryProviderScope<NavKey>.firstScreenGraph() {
    entry<FirstScreenRoute.FirstScreen> {
        FirstScreenView()
    }
}
