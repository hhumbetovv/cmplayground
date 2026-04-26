package az.theternal.cmplayground.feature.post.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface SecondScreenRoute : NavKey {

    @Serializable
    data object SecondScreen : SecondScreenRoute
}
