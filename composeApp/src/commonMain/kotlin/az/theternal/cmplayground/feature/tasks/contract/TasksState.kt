package az.theternal.cmplayground.feature.tasks.contract

import androidx.compose.runtime.Stable
import az.theternal.cmplayground.core.state.UiState
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteData
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteDialogState
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorData
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorSheetState
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
 * The screen's state, as fields rather than as a value.
 *
 * Three layers, in declaration order:
 *
 * 1. **fields** — the writable truth. Only [az.theternal.cmplayground.feature.tasks.TasksViewModel]
 *    can write them: `set` takes a `ViewModel` context parameter, so it does not resolve outside
 *    one, and every write goes through Orbit's `reduce`.
 * 2. **derivations** — everything computable from the fields. Nothing here is stored, so nothing
 *    here can disagree with the fields it came from.
 * 3. **component states** — one per component, built once out of the `State` references above.
 *    They are constructed with this object and never replaced, so a component's parameters are
 *    fixed for the life of the screen and it recomposes only where it reads a `.value`.
 *
 * The task list is normalised — order in [taskIds] / [visibleTaskIds], data in [tasksById] — so
 * that editing one task rewrites one map entry and leaves both id lists untouched. That is what
 * lets the list component skip while the edited row recomposes.
 */
@Stable
class TasksState : UiState() {

    // region fields

    val isLoading = field(true)
    val errorMessage = field<String?>(null)
    val query = field("")
    val filters = field<ImmutableSet<TaskFilter>>(persistentSetOf())
    val taskIds = field<ImmutableList<String>>(persistentListOf())
    val tasksById = field<ImmutableMap<String, TaskItemState>>(persistentMapOf())
    val visibleTaskIds = field<ImmutableList<String>>(persistentListOf())
    val canLoadMore = field(false)
    val isLoadingMore = field(false)
    val editor = field<TaskEditorData?>(null)
    val deleteTarget = field<TaskDeleteData?>(null)

    // endregion

    // region derivations

    val isFiltered = derived { query.value.isNotBlank() || filters.value.isNotEmpty() }

    val phase = derived {
        when {
            isLoading.value -> TasksPhase.LOADING
            errorMessage.value != null -> TasksPhase.ERROR
            visibleTaskIds.value.isEmpty() -> TasksPhase.EMPTY
            else -> TasksPhase.CONTENT
        }
    }

    // endregion

    // region component states

    val searchField = TasksSearchFieldState(
        query = query,
        isClearVisible = derived { query.value.isNotEmpty() },
    )

    val filterRow = TasksFilterRowState(selected = filters)

    val summary = TasksSummaryState(
        visibleCount = derived { visibleTaskIds.value.size },
        totalCount = derived { taskIds.value.size },
        doneCount = derived { tasksById.value.count { (_, task) -> task.isDone } },
        isFiltered = isFiltered,
    )

    val list = TaskListState(
        ids = visibleTaskIds,
        tasksById = tasksById,
        canLoadMore = canLoadMore,
        isLoadingMore = isLoadingMore,
    )

    val errorView = TasksErrorViewState(message = derived { errorMessage.value.orEmpty() })

    val emptyView = TasksEmptyViewState(isFiltered = isFiltered)

    val editorSheet = TaskEditorSheetState(editor = editor)

    val deleteDialog = TaskDeleteDialogState(target = deleteTarget)

    // endregion
}
