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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.map
import az.theternal.cmplayground.core.state.read
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import kotlinx.collections.immutable.ImmutableSet

@Immutable
data class TasksFilterRowState(
    val selected: ImmutableSet<TaskFilter>,
) : ComponentState

private val ChipSpacing = 8.dp

/**
 * Each chip narrows the row state down to its own `Boolean`, so toggling one filter recomposes
 * exactly two chips — the one switched on and the one switched off — instead of the whole row.
 *
 * The selector captures `filter`, which is precisely the case
 * [az.theternal.cmplayground.core.state.map] handles by tracking the selector: no `key(...)`
 * wrapper is needed to keep the derivations from going stale.
 */
@Composable
fun TasksFilterRow(
    state: State<TasksFilterRowState>,
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
                isSelected = state.map { filter in selected },
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
    val selected = isSelected.read()

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
