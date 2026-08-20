package az.theternal.profile.impl.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.theternal.post.api.presentation.model.PostUiModel
import az.theternal.post.api.presentation.renderer.LocalPostCardRenderer

private val samplePosts = listOf(
    PostUiModel(
        title = "Learning Compose Multiplatform",
        content = "Building reusable UI across Android and iOS is getting easier every day.",
    ),
    PostUiModel(
        title = "A quiet morning",
        content = "Coffee, a notebook, and a small list of things worth making.",
    ),
    PostUiModel(
        title = "Design systems matter",
        content = "Small, consistent components make a product feel calm and intentional.",
    ),
    PostUiModel(
        title = "Shipping an experiment",
        content = "The best time to learn whether an idea works is after people can use it.",
    ),
)

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
) {
    val postCardRenderer = LocalPostCardRenderer.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            BasicText(
                text = "Profile",
                style = TextStyle(fontSize = 28.sp),
            )
        }

        item {
            ProfileStats()
        }

        items(samplePosts) { post ->
            val modifier = if(LocalInspectionMode.current) {
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(
                        color = Color.Red,
                        width = 1.dp,
                    )
            } else {
                Modifier.fillMaxWidth()
            }

            postCardRenderer.Render(
                post = post,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ProfileStats() {
    Row(modifier = Modifier.fillMaxWidth()) {
        ProfileStat(
            label = "Following",
            value = "120",
            modifier = Modifier.weight(1f),
        )
        ProfileStat(
            label = "Followers",
            value = "2.4K",
            modifier = Modifier.weight(1f),
        )
        ProfileStat(
            label = "Posts",
            value = samplePosts.size.toString(),
            modifier = Modifier.weight(1f),
        )
    }
    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun ProfileStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BasicText(
            text = value,
            style = TextStyle(fontSize = 20.sp),
        )
        BasicText(
            text = label,
            style = TextStyle(fontSize = 14.sp),
        )
    }
}

@Composable
@Preview
private fun ProfileScreenPreview() {
    ProfileScreen()
}
