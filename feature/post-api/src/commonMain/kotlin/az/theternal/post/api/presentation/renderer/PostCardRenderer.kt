package az.theternal.post.api.presentation.renderer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import az.theternal.post.api.presentation.model.PostUiModel

interface PostCardRenderer {
    @Composable
    fun Render(
        post: PostUiModel,
        modifier: Modifier = Modifier,
    )
}

val LocalPostCardRenderer = compositionLocalOf<PostCardRenderer> {
    object : PostCardRenderer {
        @Composable
        override fun Render(
            post: PostUiModel,
            modifier: Modifier,
        ) {
            Box(modifier = modifier)
        }
    }
}
