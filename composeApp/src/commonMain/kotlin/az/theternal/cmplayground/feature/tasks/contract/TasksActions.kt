package az.theternal.cmplayground.feature.tasks.contract

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

/**
 * What the UI can do, expressed as callbacks rather than as an intent type.
 *
 * This is the second half of keeping the view layer architecture-neutral: components below the
 * screen take plain callbacks and never import `TasksIntent`, so the same components work whether
 * the holder reduces intents, calls ViewModel methods, or is a preview with no holder at all.
 * Every field defaults to a no-op, which is what makes `TasksActions()` a valid preview argument.
 *
 * Built once through [rememberTasksActions]: one stable instance means passing `actions.onX` down
 * the tree never breaks a child's skipping.
 */
@Immutable
data class TasksActions(
    val onRetryClick: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onSimulateFailureClick: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onQueryClearClick: () -> Unit = {},
    val onFiltersClearClick: () -> Unit = {},
    val onFilterClick: (TaskFilter) -> Unit = {},
    val onTaskClick: (String) -> Unit = {},
    val onTaskDoneChange: (String, Boolean) -> Unit = { _, _ -> },
    val onTaskDeleteClick: (String) -> Unit = {},
    val onCreateClick: () -> Unit = {},
    val onEditorTitleChange: (String) -> Unit = {},
    val onEditorNoteChange: (String) -> Unit = {},
    val onEditorPriorityChange: (TaskPriority) -> Unit = {},
    val onEditorSubmitClick: () -> Unit = {},
    val onEditorDismissRequest: () -> Unit = {},
    val onDeleteConfirmClick: () -> Unit = {},
    val onDeleteDismissRequest: () -> Unit = {},
)

@Composable
fun rememberTasksActions(dispatch: (TasksIntent) -> Unit): TasksActions = remember(dispatch) {
    TasksActions(
        onRetryClick = { dispatch(TasksIntent.Retry) },
        onLoadMore = { dispatch(TasksIntent.LoadMore) },
        onSimulateFailureClick = { dispatch(TasksIntent.SimulateFailure) },
        onQueryChange = { dispatch(TasksIntent.QueryChanged(it)) },
        onQueryClearClick = { dispatch(TasksIntent.QueryCleared) },
        onFiltersClearClick = { dispatch(TasksIntent.FiltersCleared) },
        onFilterClick = { dispatch(TasksIntent.FilterToggled(it)) },
        onTaskClick = { dispatch(TasksIntent.TaskOpened(it)) },
        onTaskDoneChange = { id, isDone -> dispatch(TasksIntent.TaskDoneChanged(id, isDone)) },
        onTaskDeleteClick = { dispatch(TasksIntent.DeleteRequested(it)) },
        onCreateClick = { dispatch(TasksIntent.CreateRequested) },
        onEditorTitleChange = { dispatch(TasksIntent.EditorTitleChanged(it)) },
        onEditorNoteChange = { dispatch(TasksIntent.EditorNoteChanged(it)) },
        onEditorPriorityChange = { dispatch(TasksIntent.EditorPriorityChanged(it)) },
        onEditorSubmitClick = { dispatch(TasksIntent.EditorSubmitted) },
        onEditorDismissRequest = { dispatch(TasksIntent.EditorDismissed) },
        onDeleteConfirmClick = { dispatch(TasksIntent.DeleteConfirmed) },
        onDeleteDismissRequest = { dispatch(TasksIntent.DeleteDismissed) },
    )
}
