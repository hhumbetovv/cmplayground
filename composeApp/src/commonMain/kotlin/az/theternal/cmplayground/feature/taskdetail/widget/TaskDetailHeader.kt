package az.theternal.cmplayground.feature.taskdetail.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.read
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

@Immutable
data class TaskDetailHeaderState(
    val title: String,
    val priority: TaskPriority,
    val isDone: Boolean,
    val isUpdating: Boolean,
) : ComponentState

private val HeaderSpacing = 12.dp

@Composable
fun TaskDetailHeader(
    state: State<TaskDetailHeaderState>,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val header = state.read()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .trackRecompositions(),
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
    ) {
        Text(text = header.title, style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(onClick = {}, label = { Text(header.priority.label) })

            Row(
                horizontalArrangement = Arrangement.spacedBy(HeaderSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (header.isDone) "Done" else "Open",
                    style = MaterialTheme.typography.labelLarge,
                )
                Switch(
                    checked = header.isDone,
                    onCheckedChange = onDoneChange,
                    enabled = !header.isUpdating,
                )
            }
        }
    }
}
