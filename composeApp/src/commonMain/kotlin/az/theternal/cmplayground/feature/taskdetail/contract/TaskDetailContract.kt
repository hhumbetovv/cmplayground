package az.theternal.cmplayground.feature.taskdetail.contract

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import az.theternal.cmplayground.core.mvi.UiEffect
import az.theternal.cmplayground.core.mvi.UiState
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailHeaderState
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailNoteState
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

enum class TaskDetailPhase { LOADING, ERROR, CONTENT }

/**
 * Same shape as `TasksState` — immutable class, projection per component — but reduced by a plain
 * `MutableStateFlow` ViewModel with no MVI library involved.
 *
 * That is the point of this screen: the contract the UI consumes does not change with the
 * architecture behind it.
 */
@Stable
data class TaskDetailState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val title: String = "",
    val note: String = "",
    val priority: TaskPriority = TaskPriority.NORMAL,
    val isDone: Boolean = false,
    val isUpdating: Boolean = false,
) : UiState {

    fun phase(): TaskDetailPhase = when {
        isLoading -> TaskDetailPhase.LOADING
        errorMessage != null -> TaskDetailPhase.ERROR
        else -> TaskDetailPhase.CONTENT
    }

    fun headerState(): TaskDetailHeaderState = TaskDetailHeaderState(
        title = title,
        priority = priority,
        isDone = isDone,
        isUpdating = isUpdating,
    )

    fun noteState(): TaskDetailNoteState = TaskDetailNoteState(note = note)
}

sealed interface TaskDetailEffect : UiEffect {
    data class ShowMessage(val message: String) : TaskDetailEffect
}

/**
 * No intent type here: this holder exposes methods, and `TaskDetailScreen` binds them straight
 * into the actions bag. The components below cannot tell the difference.
 */
@Immutable
data class TaskDetailActions(
    val onRetryClick: () -> Unit = {},
    val onDoneChange: (Boolean) -> Unit = {},
    val onBackClick: () -> Unit = {},
)
