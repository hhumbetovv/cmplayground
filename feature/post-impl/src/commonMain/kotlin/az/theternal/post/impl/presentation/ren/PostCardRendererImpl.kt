package az.theternal.post.impl.presentation.ren

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import az.theternal.post.api.presentation.model.PostUiModel
import az.theternal.post.api.presentation.renderer.PostCardRenderer
import az.theternal.post.impl.presentation.components.PostCard

class PostCardRendererImpl : PostCardRenderer {
    @Composable
    override fun Render(
        post: PostUiModel,
        modifier: Modifier,
    ) {
        PostCard(
            post = post,
            modifier = modifier,
        )
    }
}
