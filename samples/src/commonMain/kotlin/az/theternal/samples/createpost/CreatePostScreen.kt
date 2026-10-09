package az.theternal.samples.createpost

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.mvi.CollectEffects
import az.theternal.mvi.Store
import az.theternal.mvi.collectAsState
import az.theternal.samples.createpost.body.BodyIntent
import az.theternal.samples.createpost.poll.PollIntent
import az.theternal.samples.createpost.tags.TagsIntent
import az.theternal.samples.ui.ErrorText
import az.theternal.samples.ui.Hint
import az.theternal.samples.ui.SamplePage
import az.theternal.samples.ui.Section

@Composable
fun CreatePostScreen(
    store: Store<CreatePostState, CreatePostIntent, CreatePostEffect>,
    snackbar: SnackbarHostState,
    onPublished: () -> Unit,
) {
    store.CollectEffects { effect ->
        when (effect) {
            is CreatePostEffect.Published -> {
                snackbar.showSnackbar("Published ${effect.postId}")
                onPublished()
            }
            is CreatePostEffect.Message -> snackbar.showSnackbar(effect.text)
        }
    }
    val state = store.collectAsState()
    val postIntent = store::postIntent
    SamplePage {
        Hint(
            "Level 4: four slices at three levels. Text containing \"fail\" fails to publish; cancel works " +
                "while uploading or publishing; nothing is editable meanwhile.",
        )
        // Each component posts its slice's intents; the parent wraps them. A component with its own
        // ViewModel later would post the same intents to it.
        BodyEditor(state.toBodyUi()) { postIntent(CreatePostIntent.Body(it)) }
        PollEditor(state.toPollUi()) { postIntent(CreatePostIntent.Poll(it)) }
        TagPicker(state.toTagsUi()) { postIntent(CreatePostIntent.Tags(it)) }
        PublishBar(state.toPublishUi(), postIntent)
    }
}

@Composable
private fun BodyEditor(
    state: State<BodyUi>,
    postIntent: (BodyIntent) -> Unit,
) {
    val body by state
    OutlinedTextField(
        value = body.text,
        onValueChange = { postIntent(BodyIntent.Edit(it)) },
        label = { Text("What's happening?") },
        supportingText = { Text("${body.remaining}") },
        enabled = body.editable,
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PollEditor(
    state: State<PollUi>,
    postIntent: (PollIntent) -> Unit,
) {
    val poll by state
    Section("Poll") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add a poll", modifier = Modifier.weight(1f))
            Switch(checked = poll.isOn, onCheckedChange = { postIntent(PollIntent.Toggle) }, enabled = poll.editable)
        }
        if (poll.isOn) {
            poll.options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { postIntent(PollIntent.EditOption(index, it)) },
                        label = { Text("Option ${index + 1}") },
                        enabled = poll.editable,
                        modifier = Modifier.weight(1f),
                    )
                    if (poll.canRemove) {
                        TextButton(onClick = { postIntent(PollIntent.RemoveOption(index)) }, enabled = poll.editable) {
                            Text("Remove")
                        }
                    }
                }
            }
            if (poll.canAdd) {
                TextButton(onClick = { postIntent(PollIntent.AddOption) }, enabled = poll.editable) { Text("Add option") }
            }
        }
    }
}

@Composable
private fun TagPicker(
    state: State<TagsUi>,
    postIntent: (TagsIntent) -> Unit,
) {
    val tags by state
    Section("Tags") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.selected.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { postIntent(TagsIntent.Remove(tag)) },
                    label = { Text("#$tag ✕") },
                    enabled = tags.editable,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = tags.query,
                onValueChange = { postIntent(TagsIntent.EditQuery(it)) },
                label = { Text("Find a tag") },
                enabled = tags.editable,
                modifier = Modifier.weight(1f),
            )
            if (tags.isSearching) CircularProgressIndicator()
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.suggestions.forEach { tag ->
                AssistChip(onClick = { postIntent(TagsIntent.Add(tag)) }, label = { Text("#$tag") }, enabled = tags.editable)
            }
        }
    }
}

@Composable
private fun PublishBar(
    state: State<PublishUi>,
    postIntent: (CreatePostIntent) -> Unit,
) {
    val publish by state
    publish.progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
    publish.error?.let { ErrorText(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { postIntent(CreatePostIntent.Publish) }, enabled = publish.canPublish) { Text(publish.label) }
        if (publish.canRetry) OutlinedButton(onClick = { postIntent(CreatePostIntent.Retry) }) { Text("Retry") }
        if (publish.canCancel) TextButton(onClick = { postIntent(CreatePostIntent.Cancel) }) { Text("Cancel") }
    }
}
