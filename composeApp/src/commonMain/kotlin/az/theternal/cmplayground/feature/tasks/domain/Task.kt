package az.theternal.cmplayground.feature.tasks.domain

import androidx.compose.runtime.Immutable

@Immutable
data class Task(
    val id: String,
    val title: String,
    val note: String,
    val priority: TaskPriority,
    val isDone: Boolean,
)

enum class TaskPriority(val label: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High"),
}

/**
 * Filters combine with AND, so selecting `OPEN` together with `DONE` legitimately yields nothing —
 * that is the empty-with-filters case the screen has to render.
 */
enum class TaskFilter(val label: String) {
    OPEN("Open"),
    DONE("Done"),
    HIGH_PRIORITY("High priority"),
}

@Immutable
data class TaskDraft(
    val id: String?,
    val title: String,
    val note: String,
    val priority: TaskPriority,
)

@Immutable
data class TaskPage(
    val tasks: List<Task>,
    val hasMore: Boolean,
)
