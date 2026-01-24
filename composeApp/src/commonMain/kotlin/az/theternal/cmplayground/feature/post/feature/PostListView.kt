package az.theternal.cmplayground.feature.post.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.navigation.navigator.LocalNavigator
import az.theternal.cmplayground.feature.post.navigation.PostRoute

@Composable
fun PostListView() {
    val navigator = LocalNavigator.current

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .safeDrawingPadding()
    ) {
        items(
            count = 50,
            key = { it }
        ) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.clickable(
                    onClick = {
                        navigator.push(
                            PostRoute.Details("item_$index")
                        )
                    }
                )
            )
        }
    }
}