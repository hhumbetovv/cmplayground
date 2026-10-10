package az.theternal.samples.feed

import az.theternal.mvi.ActionLogic
import az.theternal.mvi.Hosted
import az.theternal.mvi.Policy
import az.theternal.mvi.Slot
import az.theternal.mvi.StoreScope
import az.theternal.mvi.reload
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.mvi.toContentState
import az.theternal.samples.data.PostRepository
import az.theternal.samples.report.ReportEffect
import az.theternal.samples.report.ReportIntent
import az.theternal.samples.report.ReportLogic
import az.theternal.samples.report.ReportState

/**
 * Level 2: one action, loading, under [Policy.Ignore] — a pull to refresh while one runs is ignored. It knows
 * nothing about likes: each card's like is its own component ([LikeLogic]).
 *
 * The report sheet is a [Slot]: the report screen's own logic ([ReportLogic]), opened for one post and
 * closed by the feed. Closing it cancels a report still being sent; its "sent" output is the feed's cue
 * to close it and say so.
 */
class FeedLogic(
    private val repository: PostRepository,
    reportLogic: ReportLogic.Factory,
) : ActionLogic<FeedState, FeedIntent, Unit, FeedEffect>() {
    private typealias Scope = StoreScope<FeedState, FeedEffect>

    private val report: Slot<FeedState, FeedEffect, String, ReportState, ReportIntent, ReportEffect> = Slot(
        get = FeedState::report,
        set = { copy(report = it) },
        create = { postId -> Hosted(ReportState(), reportLogic.create(postId)) },
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

    override fun Scope.onStart() {
        report.attach(this)
        dispatch(Unit)
    }

    override fun Scope.toAction(intent: FeedIntent): Unit? {
        when (intent) {
            FeedIntent.Refresh -> return Unit
            is FeedIntent.OpenReport -> report.open(intent.postId)
            is FeedIntent.Report -> report.postIntent(intent.intent)
            FeedIntent.CloseReport -> report.close()
        }
        return null
    }

    override fun policy(action: Unit) = Policy.Ignore("refresh")

    override suspend fun Scope.process(action: Unit) {
        setState { copy(posts = posts.reload()) }
        val result = safeCall { repository.feed() }
        setState { copy(posts = result.toContentState()) }
    }
}

/** The usual report sheet, for a screen built by hand and for tests. */
fun FeedLogic(repository: PostRepository): FeedLogic {
    return FeedLogic(repository) { postId -> ReportLogic(postId, repository) }
}
