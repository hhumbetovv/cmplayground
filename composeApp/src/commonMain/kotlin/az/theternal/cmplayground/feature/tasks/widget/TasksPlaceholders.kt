package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.read

@Immutable
data class TasksErrorViewState(
    val message: String,
) : ComponentState

@Immutable
data class TasksEmptyViewState(
    val isFiltered: Boolean,
) : ComponentState

private val PlaceholderPadding = 32.dp
private val PlaceholderSpacing = 16.dp

@Composable
fun TasksLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .trackRecompositions(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun TasksErrorView(
    state: State<TasksErrorViewState>,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PlaceholderPadding)
            .trackRecompositions(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PlaceholderSpacing, Alignment.CenterVertically),
    ) {
        Text(
            text = state.read { message },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetryClick) {
            Text("Retry")
        }
    }
}

@Composable
fun TasksEmptyView(
    state: State<TasksEmptyViewState>,
    onClearFiltersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFiltered = state.read { isFiltered }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PlaceholderPadding)
            .trackRecompositions(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PlaceholderSpacing, Alignment.CenterVertically),
    ) {
        Text(
            text = if (isFiltered) {
                "No task matches the current search and filters."
            } else {
                "No tasks yet. Create the first one."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (isFiltered) {
            Button(onClick = onClearFiltersClick) {
                Text("Clear filters")
            }
        }
    }
}
