package az.theternal.cmplayground.feature.auth.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.feature.auth.feature.LoginView
import az.theternal.cmplayground.feature.auth.feature.RegisterView

fun EntryProviderScope<NavKey>.authGraph() {
    entry<AuthRoute.Login> {
        LoginView()
    }

    entry<AuthRoute.Register> {
        RegisterView()
    }
}