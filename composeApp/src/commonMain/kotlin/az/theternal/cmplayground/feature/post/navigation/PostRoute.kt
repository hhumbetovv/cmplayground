package az.theternal.cmplayground.feature.post.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface PostRoute : NavKey {

    @Serializable
    data object List : PostRoute

    @Serializable
    data class Details(
        val id: String
    ) : PostRoute
}