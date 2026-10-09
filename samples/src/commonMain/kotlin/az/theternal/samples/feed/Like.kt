package az.theternal.samples.feed

import az.theternal.mvi.ActionLogic
import az.theternal.mvi.Policy
import az.theternal.mvi.StoreScope
import az.theternal.mvi.StoreViewModel
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.samples.data.Post
import az.theternal.samples.data.PostRepository

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

/**
 * A component's logic: a like button that owns its IO. Optimistic, and [Policy.Latest] — five taps send
 * the last wish only. The `before` it carries is what a failure rolls back to.
 *
 * Hosted three ways with no change: as each feed card's own ViewModel ([LikeViewModel]), as a slice of a
 * post-details screen, or as a test's store.
 */
class LikeLogic(
    private val postId: String,
    private val posts: PostRepository,
) : ActionLogic<LikeState, LikeIntent, LikeLogic.SetLiked, LikeEffect>() {
    data class SetLiked(
        val liked: Boolean,
        val before: LikeState,
    )

    override fun StoreScope<LikeState, LikeEffect>.toAction(intent: LikeIntent): SetLiked {
        val before = state
        val liked = !before.isLiked
        setState { copy(isLiked = liked, count = count + if (liked) 1 else -1) }
        return SetLiked(liked = liked, before = before)
    }

    override fun policy(action: SetLiked) = Policy.Latest

    override suspend fun StoreScope<LikeState, LikeEffect>.process(action: SetLiked) {
        safeCall { posts.setLiked(postId, action.liked) }
            .onSuccess { count -> setState { copy(count = count) } }
            .onFailure {
                setState { action.before }
                emit(LikeEffect.Failed)
            }
    }
}

/**
 * The component ViewModel: keyed by the post, in the screen's ViewModel store. It owns its state from
 * creation; a feed refresh does not reset it — that is the trade-off of a component ViewModel, and why
 * the feed passes only the initial values.
 */
class LikeViewModel(
    post: Post,
    posts: PostRepository,
) : StoreViewModel<LikeState, LikeIntent, LikeEffect>(LikeState(post.isLiked, post.likes), LikeLogic(post.id, posts))
