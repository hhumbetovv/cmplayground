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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.state.preview
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
 * The whole view layer in one signature: the state holder and a bag of callbacks.
 *
 * No container, no ViewModel, no intent type, no coroutine scope — which is why this screen can be
 * driven by Orbit, by any other holder, or by a preview with hand-set fields.
 *
 * This function reads nothing at all. It hands each component the component state built for it and
 * composes once; every recomposition below happens in the component that read the field that
 * changed.
 */
@Composable
fun TasksScreenContent(
    state: TasksState,
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
                    state = state.searchField,
                    onQueryChange = actions.onQueryChange,
                    onClearClick = actions.onQueryClearClick,
                )

                TasksFilterRow(
                    state = state.filterRow,
                    onFilterClick = actions.onFilterClick,
                )

                TasksSummaryBar(state = state.summary)
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
        state = state.editorSheet,
        onTitleChange = actions.onEditorTitleChange,
        onNoteChange = actions.onEditorNoteChange,
        onPriorityChange = actions.onEditorPriorityChange,
        onSubmitClick = actions.onEditorSubmitClick,
        onDismissRequest = actions.onEditorDismissRequest,
    )

    TaskDeleteDialog(
        state = state.deleteDialog,
        onConfirmClick = actions.onDeleteConfirmClick,
        onDismissRequest = actions.onDeleteDismissRequest,
    )
}

/**
 * The phase switch is the one place that has to read, so it sits in its own composable: going from
 * loading to content recomposes the body and leaves the search field, filters and summary above it
 * untouched.
 */
@Composable
private fun TasksBodySection(
    state: TasksState,
    actions: TasksActions,
    modifier: Modifier = Modifier,
) {
    when (state.phase.value) {
        TasksPhase.LOADING -> TasksLoadingView(modifier)

        TasksPhase.ERROR -> TasksErrorView(
            state = state.errorView,
            onRetryClick = actions.onRetryClick,
            modifier = modifier,
        )

        TasksPhase.EMPTY -> TasksEmptyView(
            state = state.emptyView,
            onClearFiltersClick = actions.onFiltersClearClick,
            modifier = modifier,
        )

        TasksPhase.CONTENT -> TaskList(
            state = state.list,
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

    val state = remember { TasksState() }.preview {
        state.isLoading.set(false)
        state.taskIds.set(tasks.keys.toPersistentList())
        state.tasksById.set(tasks)
        state.visibleTaskIds.set(tasks.keys.toPersistentList())
    }

    TasksScreenContent(state = state, actions = TasksActions())
}
