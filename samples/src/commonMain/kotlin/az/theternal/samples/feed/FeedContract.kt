package az.theternal.samples.feed

import az.theternal.mvi.ContentState
import az.theternal.samples.data.Post
import az.theternal.samples.report.ReportIntent
import az.theternal.samples.report.ReportState

data class FeedState(
    val posts: ContentState<List<Post>> = ContentState.Idle,
    /** The report sheet: open when not null. */
    val report: ReportState? = null,
)

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
