package az.theternal.cmplayground.router

import az.theternal.cmplayground.feature.product.domain.entities.ProductEntity
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {

    @Serializable
    data object ProductList : Route

    @Serializable
    data class ProductDetails(
        val itemId: String,
    ) : Route

}