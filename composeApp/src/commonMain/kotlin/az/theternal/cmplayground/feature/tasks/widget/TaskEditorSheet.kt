package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.read
import az.theternal.cmplayground.core.state.rememberTextInput
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

@Immutable
data class TaskEditorState(
    val taskId: String?,
    val title: String,
    val note: String,
    val priority: TaskPriority,
    val isSaving: Boolean,
    val titleError: String?,
) : ComponentState {
    val isEditing: Boolean get() = taskId != null
    val isSubmitEnabled: Boolean get() = title.isNotBlank() && !isSaving
}

/**
 * The half of the editor that the reducer has no business knowing about: whether the optional note
 * section is unfolded. It dies with the sheet, never reaches the repository, and would only add a
 * field and an intent to the screen contract.
 *
 * State the UI owns needs no framework — `private set` is the whole ownership story.
 */
@Stable
class TaskEditorUiState {
    var isNoteExpanded by mutableStateOf(false)
        private set

    fun toggleNote() {
        isNoteExpanded = !isNoteExpanded
    }
}

private val SheetPadding = 20.dp
private val SheetSpacing = 16.dp
private val ChipSpacing = 8.dp
private val ProgressSize = 18.dp
private val ProgressStroke = 2.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    state: State<TaskEditorState?>,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onSubmitClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    // A whole read is right here: the editor state IS the narrow slice, and the sheet only exists
    // while it is non-null.
    val editor = state.read() ?: return

    val sheetState = rememberModalBottomSheetState()
    val uiState = remember { TaskEditorUiState() }
    val titleInput = rememberTextInput(editor.title, onTitleChange)
    val noteInput = rememberTextInput(editor.note, onNoteChange)

    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SheetPadding)
                .navigationBarsPadding()
                .trackRecompositions(),
            verticalArrangement = Arrangement.spacedBy(SheetSpacing),
        ) {
            Text(
                text = if (editor.isEditing) "Edit task" else "New task",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = titleInput.value,
                onValueChange = titleInput::onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                isError = editor.titleError != null,
                supportingText = editor.titleError?.let { error -> { Text(error) } },
                enabled = !editor.isSaving,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = editor.priority == priority,
                        onClick = { onPriorityChange(priority) },
                        label = { Text(priority.label) },
                        enabled = !editor.isSaving,
                    )
                }
            }

            TextButton(onClick = uiState::toggleNote) {
                Text(if (uiState.isNoteExpanded) "Hide note" else "Add a note")
            }

            AnimatedVisibility(visible = uiState.isNoteExpanded) {
                OutlinedTextField(
                    value = noteInput.value,
                    onValueChange = noteInput::onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    enabled = !editor.isSaving,
                )
            }

            Button(
                onClick = onSubmitClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = editor.isSubmitEnabled,
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ProgressSize),
                        strokeWidth = ProgressStroke,
                    )
                } else {
                    Text(if (editor.isEditing) "Save" else "Create")
                }
            }
        }
    }
}
