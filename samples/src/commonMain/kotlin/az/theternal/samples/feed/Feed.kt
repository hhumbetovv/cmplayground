package az.theternal.samples.feed

import az.theternal.mvi.ActionLogic
import az.theternal.mvi.ContentState
import az.theternal.mvi.Policy
import az.theternal.mvi.StoreScope
import az.theternal.mvi.StoreViewModel
import az.theternal.mvi.ViewState
import az.theternal.mvi.reload
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.mvi.toContentState
import az.theternal.samples.data.Post
import az.theternal.samples.data.PostRepository

data class FeedState(
    val posts: ContentState<List<Post>> = ContentState.Idle,
) : ViewState

sealed interface FeedIntent {
    data object Refresh : FeedIntent
}

/**
 * Level 2: one action, loading, under [Policy.Drop] — a pull to refresh while one runs is ignored. It knows
 * nothing about likes: each card's like is its own component ([LikeLogic]).
 */
class FeedLogic(
    private val repository: PostRepository,
) : ActionLogic<FeedState, FeedIntent, Unit, Nothing>() {
    override fun StoreScope<FeedState, Nothing>.onStart() {
        dispatch(Unit)
    }

    override fun StoreScope<FeedState, Nothing>.toAction(intent: FeedIntent) = Unit

    override fun policy(action: Unit) = Policy.Drop

    override suspend fun StoreScope<FeedState, Nothing>.process(action: Unit) {
        setState { copy(posts = posts.reload()) }
        val result = safeCall { repository.feed() }
        setState { copy(posts = result.toContentState()) }
    }
}

class FeedViewModel(
    posts: PostRepository,
) : StoreViewModel<FeedState, FeedIntent, Nothing>(FeedState(), FeedLogic(posts))
