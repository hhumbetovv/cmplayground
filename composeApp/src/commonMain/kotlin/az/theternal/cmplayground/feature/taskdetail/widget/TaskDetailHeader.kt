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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

/**
 * `State` fields: ticking the task changes `isDone` and `isUpdating` while the title and the
 * priority stand still, so the status row repaints and the title does not.
 */
@Stable
data class TaskDetailHeaderState(
    val title: State<String>,
    val priority: State<TaskPriority>,
    val isDone: State<Boolean>,
    val isUpdating: State<Boolean>,
) : ComponentState

private val HeaderSpacing = 12.dp

@Composable
fun TaskDetailHeader(
    state: TaskDetailHeaderState,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .trackRecompositions(),
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
    ) {
        Text(text = state.title.value, style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(onClick = {}, label = { Text(state.priority.value.label) })

            Row(
                horizontalArrangement = Arrangement.spacedBy(HeaderSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.isDone.value) "Done" else "Open",
                    style = MaterialTheme.typography.labelLarge,
                )
                Switch(
                    checked = state.isDone.value,
                    onCheckedChange = onDoneChange,
                    enabled = !state.isUpdating.value,
                )
            }
        }
    }
}
