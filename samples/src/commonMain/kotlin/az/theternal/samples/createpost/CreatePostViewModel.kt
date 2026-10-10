package az.theternal.samples.createpost

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.keeper
import az.theternal.mvi.store
import az.theternal.samples.data.PostRepository

class CreatePostViewModel(
    posts: PostRepository,
    savedState: SavedStateHandle,
) : ViewModel(),
    StoreOwner<CreatePostState, CreatePostIntent, CreatePostEffect> {
    override val store = store(CreatePostState(), CreatePostLogic(posts), keeper = savedState.draftKeeper())
}
