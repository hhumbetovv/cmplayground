package az.theternal.samples.createpost

import az.theternal.samples.createpost.body.BodyIntent
import az.theternal.samples.createpost.body.BodyState
import az.theternal.samples.createpost.poll.PollIntent
import az.theternal.samples.createpost.poll.PollState
import az.theternal.samples.createpost.publish.PublishPhase
import az.theternal.samples.createpost.tags.TagsIntent
import az.theternal.samples.createpost.tags.TagsState
import az.theternal.samples.data.PostDraft

/** The screen's state: one immutable value, one field per slice. */
data class CreatePostState(
    val body: BodyState = BodyState(),
    val poll: PollState = PollState(),
    val tags: TagsState = TagsState(),
    val publish: PublishPhase = PublishPhase.Idle,
)

sealed interface CreatePostIntent {
    data class Body(
        val intent: BodyIntent,
    ) : CreatePostIntent

    data class Poll(
        val intent: PollIntent,
    ) : CreatePostIntent

    data class Tags(
        val intent: TagsIntent,
    ) : CreatePostIntent

    data object Publish : CreatePostIntent

    data object Retry : CreatePostIntent

    data object Cancel : CreatePostIntent
}

sealed interface CreatePostEffect {
    data class Published(
        val postId: String,
    ) : CreatePostEffect

    data class Message(
        val text: String,
    ) : CreatePostEffect
}

sealed interface DraftCheck {
    data class Ready(
        val draft: PostDraft,
    ) : DraftCheck

    data class Invalid(
        val reason: String,
    ) : DraftCheck
}

/**
 * The parameter-gathering step for publishing, across slices: the publish slice gets a finished draft and
 * never reads the others. A pure function, tested as one.
 */
fun CreatePostState.checkDraft(): DraftCheck {
    val text = body.text.trim()
    val options = poll.options.map(String::trim)
    return when {
        text.isEmpty() -> DraftCheck.Invalid("Write something first")
        poll.isOn && options.any(String::isEmpty) -> DraftCheck.Invalid("Fill in every poll option")
        poll.isOn && options.toSet().size != options.size -> DraftCheck.Invalid("Poll options must differ")
        else -> DraftCheck.Ready(
            PostDraft(text = text, pollOptions = if (poll.isOn) options else emptyList(), tags = tags.selected),
        )
    }
}
