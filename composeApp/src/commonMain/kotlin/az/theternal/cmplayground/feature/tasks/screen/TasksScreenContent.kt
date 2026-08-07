package az.theternal.cmplayground.feature.tasks.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.mvi.asUiState
import az.theternal.cmplayground.core.state.map
import az.theternal.cmplayground.core.state.read
import az.theternal.cmplayground.feature.tasks.contract.TasksActions
import az.theternal.cmplayground.feature.tasks.contract.TasksPhase
import az.theternal.cmplayground.feature.tasks.contract.TasksState
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteDialog
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorSheet
import az.theternal.cmplayground.feature.tasks.widget.TaskItemState
import az.theternal.cmplayground.feature.tasks.widget.TaskList
import az.theternal.cmplayground.feature.tasks.widget.TasksEmptyView
import az.theternal.cmplayground.feature.tasks.widget.TasksErrorView
import az.theternal.cmplayground.feature.tasks.widget.TasksFilterRow
import az.theternal.cmplayground.feature.tasks.widget.TasksLoadingView
import az.theternal.cmplayground.feature.tasks.widget.TasksSearchField
import az.theternal.cmplayground.feature.tasks.widget.TasksSummaryBar
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ScreenPadding = 16.dp
private val SectionSpacing = 12.dp
private val FabPadding = 16.dp

/**
 * The whole view layer in one signature: a `State<TasksState>` and a bag of callbacks.
 *
 * No container, no ViewModel, no intent type, no coroutine scope — which is why this screen can be
 * driven by Orbit, by a `StateFlow` holder, or by a preview with a hand-written state, and why
 * this function reads exactly one thing itself: the phase.
 */
@Composable
fun TasksScreenContent(
    state: State<TasksState>,
    actions: TasksActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    top = ScreenPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(SectionSpacing),
            ) {
                TasksSearchField(
                    state = state.map { searchFieldState() },
                    onQueryChange = actions.onQueryChange,
                    onClearClick = actions.onQueryClearClick,
                )

                TasksFilterRow(
                    state = state.map { filterRowState() },
                    onFilterClick = actions.onFilterClick,
                )

                TasksSummaryBar(state = state.map { summaryState() })
            }

            TasksBodySection(
                state = state,
                actions = actions,
                modifier = Modifier.weight(1f),
            )
        }

        FloatingActionButton(
            onClick = actions.onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(FabPadding),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New task")
        }
    }

    TaskEditorSheet(
        state = state.map { editor },
        onTitleChange = actions.onEditorTitleChange,
        onNoteChange = actions.onEditorNoteChange,
        onPriorityChange = actions.onEditorPriorityChange,
        onSubmitClick = actions.onEditorSubmitClick,
        onDismissRequest = actions.onEditorDismissRequest,
    )

    TaskDeleteDialog(
        state = state.map { deleteTarget },
        onConfirmClick = actions.onDeleteConfirmClick,
        onDismissRequest = actions.onDeleteDismissRequest,
    )
}

/**
 * The phase switch sits in its own composable so that going from loading to content recomposes
 * only the body — the search field, filters and summary above it are untouched.
 */
@Composable
private fun TasksBodySection(
    state: State<TasksState>,
    actions: TasksActions,
    modifier: Modifier = Modifier,
) {
    when (state.read { phase() }) {
        TasksPhase.LOADING -> TasksLoadingView(modifier)

        TasksPhase.ERROR -> TasksErrorView(
            state = state.map { errorViewState() },
            onRetryClick = actions.onRetryClick,
            modifier = modifier,
        )

        TasksPhase.EMPTY -> TasksEmptyView(
            state = state.map { emptyViewState() },
            onClearFiltersClick = actions.onFiltersClearClick,
            modifier = modifier,
        )

        TasksPhase.CONTENT -> TaskList(
            state = state.map { listState() },
            onTaskClick = actions.onTaskClick,
            onTaskCheckedChange = actions.onTaskDoneChange,
            onTaskDeleteClick = actions.onTaskDeleteClick,
            onLoadMore = actions.onLoadMore,
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun TasksScreenContentPreview() {
    val tasks = persistentMapOf(
        "1" to TaskItemState("1", "Split the state class", "note", TaskPriority.HIGH, false, false),
        "2" to TaskItemState("2", "Measure recompositions", "note", TaskPriority.NORMAL, true, true),
    )
    val state = TasksState(
        isLoading = false,
        taskIds = tasks.keys.toPersistentList(),
        tasksById = tasks,
        visibleTaskIds = tasks.keys.toPersistentList(),
    )

    TasksScreenContent(state = state.asUiState(), actions = TasksActions())
}
