package az.theternal.samples.feed.like

import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.store
import az.theternal.samples.data.Post
import az.theternal.samples.data.PostRepository

/**
 * The component ViewModel, in its card's own ViewModel store (`ItemScope`): it lives while the post is in
 * the feed, and is cleared when a refresh drops the post. It owns its state from creation; a refresh does
 * not reset it — that is the trade-off of a component ViewModel, and why the feed passes only the initial
 * values.
 */
class LikeViewModel(
    post: Post,
    posts: PostRepository,
) : ViewModel(),
    StoreOwner<LikeState, LikeIntent, LikeEffect> {
    override val store = store(LikeState(post.isLiked, post.likes), LikeLogic(post.id, posts))
}
