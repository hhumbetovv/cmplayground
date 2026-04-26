package az.theternal.cmplayground.feature.auth.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface FirstScreenRoute : NavKey {

    @Serializable
    data object FirstScreen : FirstScreenRoute
}
