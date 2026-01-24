package az.theternal.cmplayground.feature.auth.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AuthRoute : NavKey {

    @Serializable
    data object Login : AuthRoute

    @Serializable
    data object Register : AuthRoute
}