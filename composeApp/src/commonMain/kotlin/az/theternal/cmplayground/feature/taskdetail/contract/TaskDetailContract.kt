package az.theternal.cmplayground.feature.taskdetail.contract

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import az.theternal.cmplayground.core.mvi.UiEffect
import az.theternal.cmplayground.core.state.UiState
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailHeaderState
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailNoteState
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority

enum class TaskDetailPhase { LOADING, ERROR, CONTENT }

/**
 * Same shape as `TasksState` — fields, derivations, component states — but written by a plain
 * ViewModel with no MVI library involved.
 *
 * That is the point of this screen: neither the state holder nor the components care which
 * architecture drives the writes.
 */
@Stable
class TaskDetailState : UiState() {

    val isLoading = field(true)
    val errorMessage = field<String?>(null)
    val title = field("")
    val note = field("")
    val priority = field(TaskPriority.NORMAL)
    val isDone = field(false)
    val isUpdating = field(false)

    val phase = derived {
        when {
            isLoading.value -> TaskDetailPhase.LOADING
            errorMessage.value != null -> TaskDetailPhase.ERROR
            else -> TaskDetailPhase.CONTENT
        }
    }

    val error = derived { errorMessage.value.orEmpty() }

    val header = TaskDetailHeaderState(
        title = title,
        priority = priority,
        isDone = isDone,
        isUpdating = isUpdating,
    )

    val noteCard = TaskDetailNoteState(note = note)
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
