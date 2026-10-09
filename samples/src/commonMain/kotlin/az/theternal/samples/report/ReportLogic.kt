package az.theternal.samples.report

import androidx.lifecycle.ViewModel
import az.theternal.mvi.Logic
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.StoreScope
import az.theternal.mvi.launch
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.mvi.store
import az.theternal.samples.data.PostRepository

/**
 * Level 1 — the default. Intents change the state directly; the one request is launched by hand. One
 * flag (`isSending`) and one guard (`canSend`) are honest here: there is a single thing in flight.
 *
 * It outgrows level 1 when a second request can overlap the first, or flags start excluding each other —
 * see the sign-in samples for that moment.
 */
class ReportLogic(
    private val postId: String,
    private val posts: PostRepository,
) : Logic<ReportState, ReportIntent, ReportEffect> {
    override fun StoreScope<ReportState, ReportEffect>.onIntent(intent: ReportIntent) {
        when (intent) {
            is ReportIntent.SelectReason -> setState { copy(reason = intent.reason) }
            is ReportIntent.EditComment -> setState { copy(comment = intent.text) }
            ReportIntent.Send -> send()
        }
    }

    private fun StoreScope<ReportState, ReportEffect>.send() {
        val reason = state.reason
        if (!state.canSend || reason == null) return
        val comment = state.comment.trim()
        setState { copy(isSending = true) }
        launch {
            safeCall { posts.report(postId = postId, reason = reason, comment = comment) }
                .onSuccess { emit(ReportEffect.Sent) }
                .onFailure { error -> emit(ReportEffect.Failed(error.message.orEmpty())) }
            setState { copy(isSending = false) }
        }
    }
}

class ReportViewModel(
    postId: String,
    posts: PostRepository,
) : ViewModel(),
    StoreOwner<ReportState, ReportIntent, ReportEffect> {
    override val store = store(ReportState(), ReportLogic(postId, posts))
}
