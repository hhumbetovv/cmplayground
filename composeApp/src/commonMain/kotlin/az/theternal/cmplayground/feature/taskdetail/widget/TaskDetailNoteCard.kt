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

/**
 * A plain value passed as `State`: one field, read whole, nothing to separate. Wrapping it in
 * `State` fields would add ceremony and buy nothing.
 */
@Immutable
data class TaskDetailNoteState(
    val note: String,
) : ComponentState

private val CardPadding = 16.dp

/**
 * Toggling `isDone` on this screen cannot reach the note: the derivation behind this state is
 * chained off the `note` field, so it does not change and this card does not recompose.
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
                text = state.value.note.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
