package az.theternal.samples.report

import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.store
import az.theternal.samples.data.PostRepository

class ReportViewModel(
    postId: String,
    posts: PostRepository,
) : ViewModel(),
    StoreOwner<ReportState, ReportIntent, ReportEffect> {
    override val store = store(ReportState(), ReportLogic(postId, posts))
}
