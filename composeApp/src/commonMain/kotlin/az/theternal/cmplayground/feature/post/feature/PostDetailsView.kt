package az.theternal.cmplayground.feature.post.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.feature.post.navigation.PostRoute

@Composable
fun PostDetailsView(
    routeKey: PostRoute.Details
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = routeKey.id,
        )
    }
}