package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.derive
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import kotlinx.collections.immutable.ImmutableSet

@Stable
data class TasksFilterRowState(
    val selected: State<ImmutableSet<TaskFilter>>,
) : ComponentState

private val ChipSpacing = 8.dp

/**
 * The row itself reads nothing, so it composes once. Each chip derives its own `Boolean` from the
 * selection, so toggling a filter recomposes exactly two chips — the one switched on and the one
 * switched off.
 *
 * The derivation is `remember`ed per filter: it has to survive recomposition, and the selector
 * captures the filter it belongs to.
 */
@Composable
fun TasksFilterRow(
    state: TasksFilterRowState,
    onFilterClick: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .trackRecompositions(),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        TaskFilter.entries.forEach { filter ->
            TasksFilterChip(
                isSelected = remember(filter) { state.selected.derive { filter in this } },
                label = filter.label,
                onClick = { onFilterClick(filter) },
            )
        }
    }
}

@Composable
private fun TasksFilterChip(
    isSelected: State<Boolean>,
    label: String,
    onClick: () -> Unit,
) {
    val selected = isSelected.value

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.trackRecompositions(),
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else {
            null
        },
    )
}
