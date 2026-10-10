package az.theternal.samples.feed

import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.store
import az.theternal.samples.data.PostRepository

class FeedViewModel(
    posts: PostRepository,
) : ViewModel(),
    StoreOwner<FeedState, FeedIntent, FeedEffect> {
    override val store = store(FeedState(), FeedLogic(posts))
}
