package az.theternal.cmplayground.feature.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.theternal.cmplayground.core.state.StateWriter
import az.theternal.cmplayground.core.state.reduceState
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
 * The same state machinery without Orbit: the holder is a [TaskDetailState], writes go through the
 * same `reduceState { }`, and one-shot events go through a buffered `Channel`.
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
        reduceState {
            state.isLoading.set(true)
            state.errorMessage.set(null)
        }
        viewModelScope.launch {
            runCatching { repository.task(taskId) }
                .onSuccess { task -> reduceState { apply(task) } }
                .onFailure { error ->
                    reduceState {
                        state.isLoading.set(false)
                        state.errorMessage.set(error.message ?: UNKNOWN_ERROR)
                    }
                }
        }
    }

    fun changeDone(isDone: Boolean) {
        reduceState { state.isUpdating.set(true) }
        viewModelScope.launch {
            runCatching { repository.setDone(taskId, isDone) }
                .onSuccess { task -> reduceState { apply(task) } }
                .onFailure { error ->
                    reduceState { state.isUpdating.set(false) }
                    effectChannel.trySend(
                        TaskDetailEffect.ShowMessage(error.message ?: UNKNOWN_ERROR),
                    )
                }
        }
    }

    private fun StateWriter.apply(task: Task) {
        state.isLoading.set(false)
        state.isUpdating.set(false)
        state.errorMessage.set(null)
        state.title.set(task.title)
        state.note.set(task.note)
        state.priority.set(task.priority)
        state.isDone.set(task.isDone)
    }
}
