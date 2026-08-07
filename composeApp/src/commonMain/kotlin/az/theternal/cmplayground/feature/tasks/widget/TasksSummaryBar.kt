package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState

/**
 * `State` fields, because the counters move independently — `doneCount` when a task is ticked,
 * `visibleCount` when the filter changes — and because `doneCount` is the one expensive projection
 * on this screen. Behind it is a derivation chained off `tasksById`, so it counts when the map
 * changes and not on every keystroke.
 */
@Stable
data class TasksSummaryState(
    val visibleCount: State<Int>,
    val totalCount: State<Int>,
    val doneCount: State<Int>,
    val isFiltered: State<Boolean>,
) : ComponentState

private val RowSpacing = 8.dp

@Composable
fun TasksSummaryBar(
    state: TasksSummaryState,
    modifier: Modifier = Modifier,
) {
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
                text = if (state.isFiltered.value) {
                    "${state.visibleCount.value} of ${state.totalCount.value} shown"
                } else {
                    "${state.totalCount.value} tasks"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "${state.doneCount.value} done",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LinearProgressIndicator(
            progress = {
                val total = state.totalCount.value
                if (total == 0) 0f else state.doneCount.value.toFloat() / total
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
