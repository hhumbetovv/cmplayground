package az.theternal.samples.createpost

import androidx.lifecycle.SavedStateHandle
import az.theternal.mvi.keeper
import kotlinx.serialization.Serializable

/**
 * What survives the process being killed: the draft. Suggestions are fetched again, and a publish in
 * flight is not resumed — its upload is gone with the process.
 */
@Serializable
data class SavedDraft(
    val text: String,
    val pollOn: Boolean,
    val pollOptions: List<String>,
    val tags: List<String>,
)

fun SavedStateHandle.draftKeeper() = keeper<CreatePostState, SavedDraft>(
    key = "draft",
    serializer = SavedDraft.serializer(),
    save = { state -> SavedDraft(state.body.text, state.poll.isOn, state.poll.options, state.tags.selected) },
    restore = { draft ->
        copy(
            body = body.copy(text = draft.text),
            poll = poll.copy(isOn = draft.pollOn, options = draft.pollOptions),
            tags = tags.copy(selected = draft.tags),
        )
    },
)
