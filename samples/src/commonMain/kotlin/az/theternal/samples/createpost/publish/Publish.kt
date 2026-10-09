package az.theternal.samples.createpost.publish

import az.theternal.mvi.Logic
import az.theternal.mvi.machineLogic
import az.theternal.mvi.safeCall
import az.theternal.samples.data.PostDraft
import az.theternal.samples.data.PostRepository

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
}

sealed interface PublishIntent {
    /** The draft comes from the parent, which gathers it from the other slices. */
    data class Submit(
        val draft: PostDraft,
    ) : PublishIntent

    data object Retry : PublishIntent

    data object Cancel : PublishIntent
}

sealed interface PublishEvent {
    data class Submit(
        val draft: PostDraft,
    ) : PublishEvent

    data object Retry : PublishEvent

    data object Cancel : PublishEvent

    data class Progress(
        val value: Float,
    ) : PublishEvent

    data object Uploaded : PublishEvent

    data class Answered(
        val result: Result<String>,
    ) : PublishEvent
}

sealed interface PublishEffect {
    data class Published(
        val postId: String,
    ) : PublishEffect
}

/**
 * A level 3 slice. Cancel works from every busy phase with no cleanup code: leaving Uploading cancels the
 * upload, leaving Publishing cancels the request.
 */
fun publishLogic(posts: PostRepository): Logic<PublishPhase, PublishIntent, PublishEffect> {
    return machineLogic<PublishPhase, PublishIntent, PublishEvent, PublishEffect>(
        toEvent = { intent ->
            when (intent) {
                is PublishIntent.Submit -> PublishEvent.Submit(intent.draft)
                PublishIntent.Retry -> PublishEvent.Retry
                PublishIntent.Cancel -> PublishEvent.Cancel
            }
        },
    ) {
        state<PublishPhase.Idle> {
            onEvent<PublishEvent.Submit> { _, event -> PublishPhase.Uploading(event.draft) }
        }
        state<PublishPhase.Uploading> {
            // Keyed on the draft: progress updates change the phase, and must not restart the upload.
            sideEffect(key = { it.draft }) { uploading ->
                posts.upload(uploading.draft).collect { progress -> send(PublishEvent.Progress(progress)) }
                send(PublishEvent.Uploaded)
            }
            onEvent<PublishEvent.Progress> { uploading, event -> uploading.copy(progress = event.value) }
            onEvent<PublishEvent.Uploaded> { uploading, _ -> PublishPhase.Publishing(uploading.draft) }
            onEvent<PublishEvent.Cancel> { _, _ -> PublishPhase.Idle }
        }
        state<PublishPhase.Publishing> {
            sideEffect(key = { Unit }) { publishing ->
                send(PublishEvent.Answered(safeCall { posts.publish(publishing.draft) }))
            }
            onEvent<PublishEvent.Answered> { publishing, event ->
                event.result.fold(
                    onSuccess = { postId ->
                        trigger(PublishEffect.Published(postId))
                        PublishPhase.Published(postId)
                    },
                    onFailure = { error -> PublishPhase.Failed(publishing.draft, error.message.orEmpty()) },
                )
            }
            onEvent<PublishEvent.Cancel> { _, _ -> PublishPhase.Idle }
        }
        state<PublishPhase.Failed> {
            // The draft may have been edited since; a retry publishes the new one.
            onEvent<PublishEvent.Submit> { _, event -> PublishPhase.Uploading(event.draft) }
            onEvent<PublishEvent.Retry> { failed, _ -> PublishPhase.Publishing(failed.draft) }
            onEvent<PublishEvent.Cancel> { _, _ -> PublishPhase.Idle }
        }
        state<PublishPhase.Published>()
    }
}
