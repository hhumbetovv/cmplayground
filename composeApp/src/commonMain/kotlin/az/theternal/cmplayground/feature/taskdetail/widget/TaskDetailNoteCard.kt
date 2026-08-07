package az.theternal.cmplayground.feature.taskdetail.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState

@Stable
data class TaskDetailNoteState(
    val note: State<String>,
) : ComponentState

private val CardPadding = 16.dp

/**
 * Toggling `isDone` on this screen cannot reach the note: the note card holds a reference to the
 * `note` field and to nothing else.
 */
@Composable
fun TaskDetailNoteCard(
    state: TaskDetailNoteState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().trackRecompositions()) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(text = "Note", style = MaterialTheme.typography.labelLarge)
            Text(
                text = state.note.value.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
