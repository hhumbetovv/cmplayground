package az.theternal.cmplayground.feature.tasks.contract

import androidx.compose.runtime.Stable
import az.theternal.cmplayground.core.mvi.UiState
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteDialogState
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorState
import az.theternal.cmplayground.feature.tasks.widget.TaskItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

enum class TasksPhase { LOADING, ERROR, EMPTY, CONTENT }

/**
 * One immutable state class for the screen — the truth, reduced by Orbit.
 *
 * The task list is **normalised** — order in [taskIds] / [visibleTaskIds], data in [tasksById] —
 * so that the two kinds of change stay separable. Editing one task rewrites one map entry and
 * leaves both id lists untouched, which is what lets the list component skip while the edited row
 * recomposes. A `List<TaskItemState>` cannot express that: any item edit produces a new list.
 *
 * Nothing derived is stored: counts, `isFiltered` and the phase are computed, so there is no second
 * copy of the truth to keep in sync.
 *
 * The component states are not built here — they carry `State`, which this class cannot produce.
 * `rememberTasksScreenState` builds them from a `State<TasksState>`.
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
}
