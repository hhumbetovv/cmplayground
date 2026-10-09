package az.theternal.samples.createpost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import az.theternal.mvi.ContentState
import az.theternal.mvi.dataOrNull
import az.theternal.mvi.select
import az.theternal.samples.createpost.publish.PublishPhase

/*
 * ArenaGo's mapper style: the screen's state is the logic's, each component gets the state it draws. A
 * component's state is built from its own slice plus what the parent decides for it (`editable`), so a
 * component never reads another slice — the same boundary the logic has.
 */

data class BodyUi(
    val text: String,
    val remaining: Int,
    val editable: Boolean,
)

data class PollUi(
    val isOn: Boolean,
    val options: List<String>,
    val canAdd: Boolean,
    val canRemove: Boolean,
    val editable: Boolean,
)

data class TagsUi(
    val query: String,
    val suggestions: List<String>,
    val isSearching: Boolean,
    val selected: List<String>,
    val editable: Boolean,
)

data class PublishUi(
    val label: String,
    val progress: Float?,
    val error: String?,
    val canPublish: Boolean,
    val canRetry: Boolean,
    val canCancel: Boolean,
)

@Composable
fun State<CreatePostState>.toBodyUi(): State<BodyUi> = select { state ->
    BodyUi(text = state.body.text, remaining = state.body.remaining, editable = state.publish.isEditable)
}

@Composable
fun State<CreatePostState>.toPollUi(): State<PollUi> = select { state ->
    PollUi(
        isOn = state.poll.isOn,
        options = state.poll.options,
        canAdd = state.poll.canAdd,
        canRemove = state.poll.canRemove,
        editable = state.publish.isEditable,
    )
}

@Composable
fun State<CreatePostState>.toTagsUi(): State<TagsUi> = select { state ->
    TagsUi(
        query = state.tags.query,
        suggestions = state.tags.suggestions.dataOrNull().orEmpty(),
        isSearching = state.tags.suggestions is ContentState.Loading,
        selected = state.tags.selected,
        editable = state.publish.isEditable,
    )
}

@Composable
fun State<CreatePostState>.toPublishUi(): State<PublishUi> = select { state ->
    when (val phase = state.publish) {
        PublishPhase.Idle -> PublishUi("Publish", null, null, canPublish = true, canRetry = false, canCancel = false)
        is PublishPhase.Uploading -> PublishUi("Uploading…", phase.progress, null, canPublish = false, canRetry = false, canCancel = true)
        is PublishPhase.Publishing -> PublishUi("Publishing…", null, null, canPublish = false, canRetry = false, canCancel = true)
        is PublishPhase.Published -> PublishUi("Published", 1f, null, canPublish = false, canRetry = false, canCancel = false)
        is PublishPhase.Failed -> PublishUi("Publish", null, phase.message, canPublish = true, canRetry = true, canCancel = true)
    }
}
