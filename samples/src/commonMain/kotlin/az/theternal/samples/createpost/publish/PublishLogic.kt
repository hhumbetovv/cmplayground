package az.theternal.samples.createpost.publish

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreScope
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.mvi.whileIn
import az.theternal.samples.data.PostRepository

/**
 * A level 3 slice. Cancel works from every busy phase with no cleanup code: leaving Uploading cancels the
 * upload, leaving Publishing cancels the request, and a late result cannot overwrite the phase the user
 * moved to.
 */
class PublishLogic(
    private val posts: PostRepository,
) : Logic<PublishPhase, PublishIntent, PublishEffect> {
    private typealias Scope = StoreScope<PublishPhase, PublishEffect>

    override fun Scope.onStart() {
        // Progress changes the phase's data, not the phase, so it does not restart the upload.
        whileIn(PublishPhase.Uploading::class) { uploading ->
            posts.upload(uploading.draft).collect { progress -> setState { copy(progress = progress) } }
            setState { PublishPhase.Publishing(draft) }
        }
        whileIn(PublishPhase.Publishing::class) { publishing ->
            safeCall { posts.publish(publishing.draft) }.fold(
                onSuccess = { postId ->
                    setState { PublishPhase.Published(postId) }
                    emit(PublishEffect.Published(postId))
                },
                onFailure = { error -> setState { PublishPhase.Failed(draft, error.message.orEmpty()) } },
            )
        }
    }

    override fun Scope.onIntent(intent: PublishIntent) {
        when (intent) {
            // From Failed too: the draft may have been edited since, and a new submit publishes the new one.
            is PublishIntent.Submit -> setState { if (isEditable) PublishPhase.Uploading(intent.draft) else this }
            // A retry publishes the draft the failure came with.
            PublishIntent.Retry -> setState { if (this is PublishPhase.Failed) PublishPhase.Publishing(draft) else this }
            PublishIntent.Cancel -> setState { if (isBusy || this is PublishPhase.Failed) PublishPhase.Idle else this }
        }
    }
}
