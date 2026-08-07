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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.UiState
import az.theternal.cmplayground.core.state.rememberTextInput
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

/**
 * The form's contents, kept as one immutable value inside a single field rather than as fields of
 * their own: the editor is created, filled and discarded as a unit, and nothing outside the sheet
 * reads a part of it. Splitting it would buy granularity no component asks for.
 */
@Immutable
data class TaskEditorData(
    val taskId: String?,
    val title: String,
    val note: String,
    val priority: TaskPriority,
    val isSaving: Boolean,
    val titleError: String?,
) {
    val isEditing: Boolean get() = taskId != null
    val isSubmitEnabled: Boolean get() = title.isNotBlank() && !isSaving
}

@Stable
data class TaskEditorSheetState(
    val editor: State<TaskEditorData?>,
) : ComponentState

/**
 * The half of the editor the state holder has no business knowing about: whether the optional note
 * section is unfolded. It dies with the sheet and never reaches the repository.
 *
 * Same machinery as the screen state — a [UiState] whose owner is the only writer — just owned by
 * the UI instead of the ViewModel.
 */
@Stable
class TaskEditorUiState : UiState() {
    val isNoteExpanded = field(false)

    fun toggleNote() = isNoteExpanded.set { !this }
}

private val SheetPadding = 20.dp
private val SheetSpacing = 16.dp
private val ChipSpacing = 8.dp
private val ProgressSize = 18.dp
private val ProgressStroke = 2.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    state: TaskEditorSheetState,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onSubmitClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val editor = state.editor.value ?: return

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
                Text(if (uiState.isNoteExpanded.value) "Hide note" else "Add a note")
            }

            AnimatedVisibility(visible = uiState.isNoteExpanded.value) {
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
