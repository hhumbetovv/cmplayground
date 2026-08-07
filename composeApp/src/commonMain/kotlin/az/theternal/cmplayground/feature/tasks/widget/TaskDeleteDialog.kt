package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.read

@Immutable
data class TaskDeleteDialogState(
    val taskId: String,
    val title: String,
    val isDeleting: Boolean,
) : ComponentState

/**
 * A dialog is state, not an event: it is `deleteTarget` in the screen state, so it survives
 * rotation and process death exactly like the rest of the screen. Effects are for what must happen
 * once — navigation, a snackbar — never for what must stay on screen.
 */
@Composable
fun TaskDeleteDialog(
    state: State<TaskDeleteDialogState?>,
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val dialog = state.read() ?: return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Delete task") },
        text = { Text("\"${dialog.title}\" will be removed.") },
        confirmButton = {
            TextButton(onClick = onConfirmClick, enabled = !dialog.isDeleting) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, enabled = !dialog.isDeleting) {
                Text("Cancel")
            }
        },
    )
}
