package az.theternal.cmplayground.feature.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.theternal.cmplayground.core.state.snapshot
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailEffect
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailState
import az.theternal.cmplayground.feature.tasks.data.FakeTaskRepository
import az.theternal.cmplayground.feature.tasks.data.TaskRepository
import az.theternal.cmplayground.feature.tasks.domain.Task
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val UNKNOWN_ERROR = "Something went wrong."

/**
 * The same state machinery without Orbit: the holder is a [TaskDetailState], writes use the same
 * context-parameter `set`, and one-shot events go through a buffered `Channel`.
 *
 * There is no `reduce` here to serialise them, so a multi-field write goes through `state.set { }`
 * for the snapshot atomicity Orbit's event loop does not provide.
 *
 * Nothing in `TaskDetailScreenContent` or in the widgets below it changes because of that — they
 * were never told what writes the fields.
 */
class TaskDetailViewModel(
    private val taskId: String,
    private val repository: TaskRepository = FakeTaskRepository,
) : ViewModel() {

    val state = TaskDetailState()

    private val effectChannel = Channel<TaskDetailEffect>(Channel.BUFFERED)
    val effects: Flow<TaskDetailEffect> = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        state.snapshot {
            isLoading.snapshot(true)
            errorMessage.snapshot(null)
        }
        viewModelScope.launch {
            runCatching { repository.task(taskId) }
                .onSuccess { task -> state.snapshot { apply(task) } }
                .onFailure { error ->
                    state.snapshot {
                        isLoading.snapshot(false)
                        errorMessage.snapshot(error.message ?: UNKNOWN_ERROR)
                    }
                }
        }
    }

    fun changeDone(isDone: Boolean) {
        state.isUpdating.snapshot(true)
        viewModelScope.launch {
            runCatching { repository.setDone(taskId, isDone) }
                .onSuccess { task -> state.snapshot { apply(task) } }
                .onFailure { error ->
                    state.isUpdating.snapshot(false)
                    effectChannel.trySend(
                        TaskDetailEffect.ShowMessage(error.message ?: UNKNOWN_ERROR),
                    )
                }
        }
    }

    private fun TaskDetailState.apply(task: Task) {
        isLoading.snapshot(false)
        isUpdating.snapshot(false)
        errorMessage.snapshot(null)
        title.snapshot(task.title)
        note.snapshot(task.note)
        priority.snapshot(task.priority)
        isDone.snapshot(task.isDone)
    }
}
