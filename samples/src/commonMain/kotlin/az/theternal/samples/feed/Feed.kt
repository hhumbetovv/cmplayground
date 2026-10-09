package az.theternal.samples.feed

import az.theternal.mvi.ActionLogic
import az.theternal.mvi.Child
import az.theternal.mvi.ContentState
import az.theternal.mvi.Policy
import az.theternal.mvi.Slot
import az.theternal.mvi.StoreScope
import az.theternal.mvi.StoreViewModel
import az.theternal.mvi.ViewState
import az.theternal.mvi.reload
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.mvi.toContentState
import az.theternal.samples.data.Post
import az.theternal.samples.data.PostRepository
import az.theternal.samples.report.ReportEffect
import az.theternal.samples.report.ReportIntent
import az.theternal.samples.report.ReportLogic
import az.theternal.samples.report.ReportState

data class FeedState(
    val posts: ContentState<List<Post>> = ContentState.Idle,
    /** The report sheet: open when not null. */
    val report: ReportState? = null,
) : ViewState

sealed interface FeedIntent {
    data object Refresh : FeedIntent

    data class OpenReport(
        val postId: String,
    ) : FeedIntent

    data class Report(
        val intent: ReportIntent,
    ) : FeedIntent

    data object CloseReport : FeedIntent
}

sealed interface FeedEffect {
    data class Message(
        val text: String,
    ) : FeedEffect
}

/**
 * Level 2: one action, loading, under [Policy.Drop] — a pull to refresh while one runs is ignored. It knows
 * nothing about likes: each card's like is its own component ([LikeLogic]).
 *
 * The report sheet is a [Slot]: the report screen's own logic ([ReportLogic]), opened for one post and
 * closed by the feed. Closing it cancels a report still being sent; its "sent" output is the feed's cue
 * to close it and say so.
 */
class FeedLogic(
    private val repository: PostRepository,
) : ActionLogic<FeedState, FeedIntent, Unit, FeedEffect>() {
    private val report: Slot<FeedState, FeedEffect, String, ReportState, ReportIntent, ReportEffect> = Slot(
        get = FeedState::report,
        set = { copy(report = it) },
        create = { postId -> Child(ReportState(), ReportLogic(postId, repository)) },
        onEffect = { effect ->
            when (effect) {
                ReportEffect.Sent -> {
                    report.close()
                    emit(FeedEffect.Message("Report sent"))
                }
                is ReportEffect.Failed -> emit(FeedEffect.Message(effect.message))
            }
        },
    )

    override fun StoreScope<FeedState, FeedEffect>.onStart() {
        report.attach(this)
        dispatch(Unit)
    }

    override fun StoreScope<FeedState, FeedEffect>.toAction(intent: FeedIntent): Unit? {
        when (intent) {
            FeedIntent.Refresh -> return Unit
            is FeedIntent.OpenReport -> report.open(intent.postId)
            is FeedIntent.Report -> report.postIntent(intent.intent)
            FeedIntent.CloseReport -> report.close()
        }
        return null
    }

    override fun policy(action: Unit) = Policy.Drop

    override suspend fun StoreScope<FeedState, FeedEffect>.process(action: Unit) {
        setState { copy(posts = posts.reload()) }
        val result = safeCall { repository.feed() }
        setState { copy(posts = result.toContentState()) }
    }
}

class FeedViewModel(
    posts: PostRepository,
) : StoreViewModel<FeedState, FeedIntent, FeedEffect>(FeedState(), FeedLogic(posts))
