package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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

@Immutable
data class TasksSummaryState(
    val visibleCount: Int,
    val totalCount: Int,
    val doneCount: Int,
    val isFiltered: Boolean,
) : ComponentState

private val RowSpacing = 8.dp

/**
 * Counters are derived, never stored: the reducer keeps the task list and nothing else, and this
 * projection is recomputed only when that list actually changes. Storing `doneCount` in the state
 * class would mean a second place to keep in sync for no gain.
 */
@Composable
fun TasksSummaryBar(
    state: State<TasksSummaryState>,
    modifier: Modifier = Modifier,
) {
    val summary = state.read()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .trackRecompositions(),
        verticalArrangement = Arrangement.spacedBy(RowSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (summary.isFiltered) {
                    "${summary.visibleCount} of ${summary.totalCount} shown"
                } else {
                    "${summary.totalCount} tasks"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "${summary.doneCount} done",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LinearProgressIndicator(
            progress = {
                if (summary.totalCount == 0) 0f else summary.doneCount.toFloat() / summary.totalCount
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
