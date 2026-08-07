package az.theternal.cmplayground.feature.tasks.contract

import az.theternal.cmplayground.core.mvi.UiIntent
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

sealed interface TasksIntent : UiIntent {

    data object Retry : TasksIntent
    data object LoadMore : TasksIntent
    data object SimulateFailure : TasksIntent

    data class QueryChanged(val query: String) : TasksIntent
    data object QueryCleared : TasksIntent
    data object FiltersCleared : TasksIntent
    data class FilterToggled(val filter: TaskFilter) : TasksIntent

    data class TaskOpened(val id: String) : TasksIntent
    data class TaskDoneChanged(val id: String, val isDone: Boolean) : TasksIntent

    data object CreateRequested : TasksIntent
    data class EditorTitleChanged(val title: String) : TasksIntent
    data class EditorNoteChanged(val note: String) : TasksIntent
    data class EditorPriorityChanged(val priority: TaskPriority) : TasksIntent
    data object EditorSubmitted : TasksIntent
    data object EditorDismissed : TasksIntent

    data class DeleteRequested(val id: String) : TasksIntent
    data object DeleteConfirmed : TasksIntent
    data object DeleteDismissed : TasksIntent
}
