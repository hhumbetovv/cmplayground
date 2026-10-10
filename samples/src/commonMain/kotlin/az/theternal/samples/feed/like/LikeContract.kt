package az.theternal.samples.feed.like

data class LikeState(
    val isLiked: Boolean,
    val count: Int,
)

sealed interface LikeIntent {
    data object Toggle : LikeIntent
}

sealed interface LikeEffect {
    data object Failed : LikeEffect
}
