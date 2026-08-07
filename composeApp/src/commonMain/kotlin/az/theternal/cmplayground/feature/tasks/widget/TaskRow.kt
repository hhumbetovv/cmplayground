package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.read
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

@Immutable
data class TaskItemState(
    val id: String,
    val title: String,
    val note: String,
    val priority: TaskPriority,
    val isDone: Boolean,
    val isBusy: Boolean,
) : ComponentState

private val RowPadding = 12.dp
private val ContentSpacing = 12.dp
private val BusyIndicatorSize = 20.dp
private val BusyIndicatorStroke = 2.dp

/**
 * Takes a `State` rather than a value, because a row is the level at which an item change has to
 * stop: the projection that feeds it is derived per id inside the list, so flipping one task's
 * `isDone` invalidates that row's derivation and no other — not the list, not its siblings.
 *
 * Nullable because the row outlives its data by one frame when the item is removed: the list is
 * keyed by id, so Compose keeps the disposed row's group around long enough to read a state that
 * no longer has the entry.
 *
 * The per-item `isBusy` flag is why the whole screen is not blocked while one task is being
 * toggled.
 */
@Composable
fun TaskRow(
    state: State<TaskItemState?>,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val task = state.read() ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .trackRecompositions(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RowPadding),
            horizontalArrangement = Arrangement.spacedBy(ContentSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = { onCheckedChange(it) },
                    enabled = !task.isBusy,
                )
                if (task.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(BusyIndicatorSize),
                        strokeWidth = BusyIndicatorStroke,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                )
                Text(
                    text = "${task.priority.label} · ${task.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onDeleteClick, enabled = !task.isBusy) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${task.title}")
            }
        }
    }
}
