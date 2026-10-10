package az.theternal.samples.createpost.publish

import az.theternal.mvi.Logic
import az.theternal.mvi.machineLogic
import az.theternal.mvi.safeCall
import az.theternal.samples.data.PostRepository

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
