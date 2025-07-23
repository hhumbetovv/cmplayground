package az.theternal.cmplayground.router

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import az.theternal.cmplayground.feature.product.domain.entities.dummyProducts
import az.theternal.cmplayground.feature.product.presentation.details.ProductDetailsView
import az.theternal.cmplayground.feature.product.presentation.list.ProductListView
import az.theternal.cmplayground.router.extensions.animatedScopeComposable

@Composable
fun Router() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.ProductList,
        modifier = Modifier.fillMaxSize()
    ) {

        animatedScopeComposable<Route.ProductList> {
            ProductListView(
                onNavigateDetails = { itemId ->
                    navController.navigate(
                        Route.ProductDetails(itemId)
                    )
                }
            )
        }

        animatedScopeComposable<Route.ProductDetails> { entry ->
            val route = entry.toRoute<Route.ProductDetails>()

            ProductDetailsView(
                item = dummyProducts.firstOrNull {
                    it.id == route.itemId
                }
            )
        }

    }
}