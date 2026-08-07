package az.theternal.cmplayground.feature.tasks.mapper

import az.theternal.cmplayground.feature.tasks.domain.Task
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority
import az.theternal.cmplayground.feature.tasks.widget.TaskItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Domain to component state, in one place. The state holder never builds a [TaskItemState] inline,
 * so what the list renders always has one definition.
 */
fun Task.toItemState(isBusy: Boolean = false): TaskItemState = TaskItemState(
    id = id,
    title = title,
    note = note,
    priority = priority,
    isDone = isDone,
    isBusy = isBusy,
)

/**
 * Filters ids, not items: the visible list is an ordering over the same normalised data, so
 * filtering must not produce a second copy of it.
 *
 * Pure and synchronous on purpose — the caller decides which dispatcher it runs on, and the state
 * holder runs it off the main thread. Filters combine with AND, see [TaskFilter].
 */
fun filterTaskIds(
    ids: List<String>,
    tasksById: Map<String, TaskItemState>,
    query: String,
    filters: Set<TaskFilter>,
): ImmutableList<String> {
    val trimmedQuery = query.trim()
    return ids
        .filter { id ->
            val task = tasksById[id] ?: return@filter false
            val matchesQuery = trimmedQuery.isEmpty() ||
                task.title.contains(trimmedQuery, ignoreCase = true) ||
                task.note.contains(trimmedQuery, ignoreCase = true)

            matchesQuery && filters.all { filter -> task.matches(filter) }
        }
        .toPersistentList()
}

private fun TaskItemState.matches(filter: TaskFilter): Boolean = when (filter) {
    TaskFilter.OPEN -> !isDone
    TaskFilter.DONE -> isDone
    TaskFilter.HIGH_PRIORITY -> priority == TaskPriority.HIGH
}
