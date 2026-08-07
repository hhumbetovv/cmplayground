package az.theternal.cmplayground.feature.tasks.contract

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import az.theternal.cmplayground.core.state.derive
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteDialogState
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorState
import az.theternal.cmplayground.feature.tasks.widget.TaskListState
import az.theternal.cmplayground.feature.tasks.widget.TasksEmptyViewState
import az.theternal.cmplayground.feature.tasks.widget.TasksErrorViewState
import az.theternal.cmplayground.feature.tasks.widget.TasksFilterRowState
import az.theternal.cmplayground.feature.tasks.widget.TasksSearchFieldState
import az.theternal.cmplayground.feature.tasks.widget.TasksSummaryState

/**
 * Everything the view layer receives, built once from a `State<TasksState>`.
 *
 * Two kinds of field, because the components differ:
 *
 * - **component states that carry `State`** — the search field, the filter row, the summary and the
 *   list. Their parts change at different times, or a projection behind one of them is expensive,
 *   so each part is delivered separately and the component itself is never recomposed by its
 *   caller;
 * - **`State` of a plain value** — the placeholders, the editor and the delete dialog. Their
 *   contents change as a unit and they are read whole, so they stay ordinary values that a test can
 *   build and compare without Compose.
 *
 * See `ComponentState` for how to choose.
 */
@Stable
data class TasksScreenState(
    val phase: State<TasksPhase>,
    val searchField: TasksSearchFieldState,
    val filterRow: TasksFilterRowState,
    val summary: TasksSummaryState,
    val list: TaskListState,
    val errorView: State<TasksErrorViewState>,
    val emptyView: State<TasksEmptyViewState>,
    val editor: State<TaskEditorState?>,
    val deleteTarget: State<TaskDeleteDialogState?>,
)

/**
 * The one place the screen state is taken apart. This is where v1's `xxxState()` projection
 * functions went — same list, same order, but producing `State` instead of values.
 *
 * The body is in two layers, and the order matters:
 *
 * - **layer 1** narrows the screen state to one field each. These are trivial property reads, and
 *   they re-run on every change;
 * - **layer 2** does the real work and chains off layer 1, never off `source`. `doneCount` counts
 *   the map when the map changes, not when a keystroke changes the query.
 *
 * `phase` and `isFiltered` are the documented exception: they genuinely depend on several fields and
 * are a `when` and two emptiness checks. Splitting them into layer 1 would duplicate the rule they
 * encode. See `derive` for the full rule.
 *
 * `remember` is load-bearing — the derivations must outlive the composition pass — and nothing here
 * reads state, so this function composes once.
 */
@Composable
fun rememberTasksScreenState(source: State<TasksState>): TasksScreenState = remember(source) {
    // layer 1 — one selector per field
    val query = source.derive { query }
    val filters = source.derive { filters }
    val taskIds = source.derive { taskIds }
    val tasksById = source.derive { tasksById }
    val visibleTaskIds = source.derive { visibleTaskIds }
    val errorMessage = source.derive { errorMessage }

    // layer 2 — chained off layer 1
    TasksScreenState(
        phase = source.derive { phase() },
        searchField = TasksSearchFieldState(
            query = query,
            isClearVisible = query.derive { isNotEmpty() },
        ),
        filterRow = TasksFilterRowState(selected = filters),
        summary = TasksSummaryState(
            visibleCount = visibleTaskIds.derive { size },
            totalCount = taskIds.derive { size },
            doneCount = tasksById.derive { count { (_, task) -> task.isDone } },
            isFiltered = source.derive { isFiltered },
        ),
        list = TaskListState(
            ids = visibleTaskIds,
            tasksById = tasksById,
            canLoadMore = source.derive { canLoadMore },
            isLoadingMore = source.derive { isLoadingMore },
        ),
        errorView = errorMessage.derive { TasksErrorViewState(message = orEmpty()) },
        emptyView = source.derive { TasksEmptyViewState(isFiltered = isFiltered) },
        editor = source.derive { editor },
        deleteTarget = source.derive { deleteTarget },
    )
}
