package az.theternal.post.api.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
data class PostUiModel(
    val title: String,
    val content: String,
)