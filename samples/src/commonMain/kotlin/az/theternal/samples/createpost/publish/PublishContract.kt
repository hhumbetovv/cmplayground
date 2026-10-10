package az.theternal.samples.createpost.publish

import az.theternal.samples.data.PostDraft

/** Publishing, as phases. Idle and Failed are the only ones in which the draft may still change. */
sealed interface PublishPhase {
    data object Idle : PublishPhase

    data class Uploading(
        val draft: PostDraft,
        val progress: Float = 0f,
    ) : PublishPhase

    data class Publishing(
        val draft: PostDraft,
    ) : PublishPhase

    data class Published(
        val postId: String,
    ) : PublishPhase

    data class Failed(
        val draft: PostDraft,
        val message: String,
    ) : PublishPhase

    val isEditable: Boolean get() = this is Idle || this is Failed

    val isBusy: Boolean get() = this is Uploading || this is Publishing
}

sealed interface PublishIntent {
    /** The draft comes from the parent, which gathers it from the other slices. */
    data class Submit(
        val draft: PostDraft,
    ) : PublishIntent

    data object Retry : PublishIntent

    data object Cancel : PublishIntent
}

sealed interface PublishEffect {
    data class Published(
        val postId: String,
    ) : PublishEffect
}
