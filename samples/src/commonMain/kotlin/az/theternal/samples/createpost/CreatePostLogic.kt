package az.theternal.samples.createpost

import az.theternal.mvi.Logic
import az.theternal.mvi.Slice
import az.theternal.mvi.StoreScope
import az.theternal.mvi.setState
import az.theternal.mvi.slice
import az.theternal.samples.createpost.body.BodyLogic
import az.theternal.samples.createpost.poll.PollLogic
import az.theternal.samples.createpost.publish.PublishEffect
import az.theternal.samples.createpost.publish.PublishIntent
import az.theternal.samples.createpost.publish.publishLogic
import az.theternal.samples.createpost.tags.TagsEffect
import az.theternal.samples.createpost.tags.TagsLogic
import az.theternal.samples.createpost.tags.TagsState
import az.theternal.samples.data.PostRepository

/**
 * Level 4: a screen made of slices, each at the level it needs — body and poll at 1, tags at 2, publishing
 * at 3. This class is only the composition root, and owns exactly three things:
 * - routing: which intent goes to which slice;
 * - the rules that span slices: nothing is editable while publishing;
 * - gathering across slices: the draft the publish slice is sent ([checkDraft]).
 *
 * A slice's effects are its outputs, and they come here first: the parent turns the tag limit into a
 * message for the screen, and on publishing it also clears the draft, so that a saved copy is not
 * offered again.
 *
 * Act2Act's CreatePostViewModel holds all of this in one 600-line class; here every slice is tested on its
 * own, and any of them can be hosted elsewhere unchanged.
 */
class CreatePostLogic(
    posts: PostRepository,
) : Logic<CreatePostState, CreatePostIntent, CreatePostEffect> {
    private typealias Scope = StoreScope<CreatePostState, CreatePostEffect>

    private val body = slice(BodyLogic(), get = CreatePostState::body, set = { copy(body = it) })
    private val poll = slice(PollLogic(), get = CreatePostState::poll, set = { copy(poll = it) })
    private val tags = Slice(
        TagsLogic(posts),
        get = CreatePostState::tags,
        set = { copy(tags = it) },
        onEffect = { effect ->
            when (effect) {
                TagsEffect.LimitReached -> emit(CreatePostEffect.Message("Up to ${TagsState.MAX_TAGS} tags"))
            }
        },
    )
    private val publish = Slice(
        publishLogic(posts),
        get = CreatePostState::publish,
        set = { copy(publish = it) },
        onEffect = { effect ->
            when (effect) {
                is PublishEffect.Published -> {
                    setState { CreatePostState(publish = publish) }
                    emit(CreatePostEffect.Published(effect.postId))
                }
            }
        },
    )

    override fun Scope.onStart() {
        body.attach(this)
        poll.attach(this)
        tags.attach(this)
        publish.attach(this)
    }

    override fun Scope.onIntent(intent: CreatePostIntent) {
        val editable = state.publish.isEditable
        when (intent) {
            is CreatePostIntent.Body -> if (editable) body.postIntent(intent.intent)
            is CreatePostIntent.Poll -> if (editable) poll.postIntent(intent.intent)
            is CreatePostIntent.Tags -> if (editable) tags.postIntent(intent.intent)
            CreatePostIntent.Publish -> when (val check = state.checkDraft()) {
                is DraftCheck.Ready -> publish.postIntent(PublishIntent.Submit(check.draft))
                is DraftCheck.Invalid -> emit(CreatePostEffect.Message(check.reason))
            }
            CreatePostIntent.Retry -> publish.postIntent(PublishIntent.Retry)
            CreatePostIntent.Cancel -> publish.postIntent(PublishIntent.Cancel)
        }
    }
}
