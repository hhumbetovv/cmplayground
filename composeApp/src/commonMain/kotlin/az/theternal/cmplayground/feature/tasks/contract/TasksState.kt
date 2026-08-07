package az.theternal.cmplayground.feature.tasks.contract

import androidx.compose.runtime.Stable
import az.theternal.cmplayground.core.mvi.UiState
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteDialogState
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorState
import az.theternal.cmplayground.feature.tasks.widget.TaskItemState
import az.theternal.cmplayground.feature.tasks.widget.TaskListState
import az.theternal.cmplayground.feature.tasks.widget.TasksEmptyViewState
import az.theternal.cmplayground.feature.tasks.widget.TasksErrorViewState
import az.theternal.cmplayground.feature.tasks.widget.TasksFilterRowState
import az.theternal.cmplayground.feature.tasks.widget.TasksSearchFieldState
import az.theternal.cmplayground.feature.tasks.widget.TasksSummaryState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

enum class TasksPhase { LOADING, ERROR, EMPTY, CONTENT }

/**
 * One immutable state class for the screen, plus one projection function per component.
 *
 * The projections are what make a wide state class cheap to consume: a component receives the
 * result of exactly one of them, so it recomposes when that result stops being `equals` to the
 * previous one and at no other time.
 *
 * The task list is **normalised** — order in [taskIds] / [visibleTaskIds], data in [tasksById] —
 * so that the two kinds of change stay separable. Editing one task rewrites one map entry and
 * leaves both id lists untouched, which is what lets the list component skip while the edited row
 * recomposes. A `List<TaskItemState>` cannot express that: any item edit produces a new list.
 *
 * Component state classes live next to the component that renders them, not here — the component
 * owns the shape of its own input, this class only knows how to fill it.
 *
 * Nothing derived is stored: counts, `isFiltered`, phase — all computed in projections, so there
 * is no second copy of the truth to keep in sync.
 */
@Stable
data class TasksState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val query: String = "",
    val filters: ImmutableSet<TaskFilter> = persistentSetOf(),
    val taskIds: ImmutableList<String> = persistentListOf(),
    val tasksById: ImmutableMap<String, TaskItemState> = persistentMapOf(),
    val visibleTaskIds: ImmutableList<String> = persistentListOf(),
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val editor: TaskEditorState? = null,
    val deleteTarget: TaskDeleteDialogState? = null,
) : UiState {

    val isFiltered: Boolean get() = query.isNotBlank() || filters.isNotEmpty()

    fun phase(): TasksPhase = when {
        isLoading -> TasksPhase.LOADING
        errorMessage != null -> TasksPhase.ERROR
        visibleTaskIds.isEmpty() -> TasksPhase.EMPTY
        else -> TasksPhase.CONTENT
    }

    fun searchFieldState(): TasksSearchFieldState = TasksSearchFieldState(
        query = query,
        isClearVisible = query.isNotEmpty(),
    )

    fun filterRowState(): TasksFilterRowState = TasksFilterRowState(selected = filters)

    fun summaryState(): TasksSummaryState = TasksSummaryState(
        visibleCount = visibleTaskIds.size,
        totalCount = taskIds.size,
        doneCount = tasksById.count { (_, task) -> task.isDone },
        isFiltered = isFiltered,
    )

    fun listState(): TaskListState = TaskListState(
        ids = visibleTaskIds,
        tasksById = tasksById,
        canLoadMore = canLoadMore,
        isLoadingMore = isLoadingMore,
    )

    fun errorViewState(): TasksErrorViewState = TasksErrorViewState(message = errorMessage.orEmpty())

    fun emptyViewState(): TasksEmptyViewState = TasksEmptyViewState(isFiltered = isFiltered)
}
