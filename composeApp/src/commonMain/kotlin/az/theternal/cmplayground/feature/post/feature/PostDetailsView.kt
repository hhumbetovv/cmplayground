package az.theternal.cmplayground.feature.post.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.core.navigation.result.LocalResultManager
import az.theternal.cmplayground.feature.post.navigation.PostRoute

@Composable
fun PostDetailsView(
    routeKey: PostRoute.Details
) {
    val resultManager = LocalResultManager.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = routeKey.id,
            modifier = Modifier.clickable(
                onClick = {
                    resultManager.setResult(
                        PostRoute.List.Result.ItemClicked(routeKey.id)
                    )
                }
            )
        )
    }
}