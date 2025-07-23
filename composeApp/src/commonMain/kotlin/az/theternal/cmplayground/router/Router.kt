package az.theternal.cmplayground.router

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import az.theternal.cmplayground.feature.product.domain.entities.dummyProducts
import az.theternal.cmplayground.feature.product.presentation.details.ProductDetailsView
import az.theternal.cmplayground.feature.product.presentation.list.ProductListView

@Composable
fun Router() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.ProductList
    ) {

        composable<Route.ProductList> {
            ProductListView(
                onNavigateDetails = { itemId ->
                    navController.navigate(
                        Route.ProductDetails(itemId)
                    )
                }
            )
        }

        composable<Route.ProductDetails> { entry ->
            val route = entry.toRoute<Route.ProductDetails>()

            ProductDetailsView(
                item = dummyProducts.firstOrNull {
                    it.id == route.itemId
                }
            )
        }

    }
}