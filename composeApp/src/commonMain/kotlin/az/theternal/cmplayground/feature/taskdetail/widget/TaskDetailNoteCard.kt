package az.theternal.cmplayground.feature.taskdetail.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.read

@Immutable
data class TaskDetailNoteState(
    val note: String,
) : ComponentState

private val CardPadding = 16.dp

/**
 * Toggling `isDone` on this screen does not recompose the note: `noteState()` still compares equal,
 * so the derivation stops here even though the screen state object was replaced.
 */
@Composable
fun TaskDetailNoteCard(
    state: State<TaskDetailNoteState>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().trackRecompositions()) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(text = "Note", style = MaterialTheme.typography.labelLarge)
            Text(
                text = state.read { note }.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
