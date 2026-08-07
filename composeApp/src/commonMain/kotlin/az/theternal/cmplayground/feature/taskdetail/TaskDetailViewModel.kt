package az.theternal.cmplayground.feature.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailEffect
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailState
import az.theternal.cmplayground.feature.tasks.data.FakeTaskRepository
import az.theternal.cmplayground.feature.tasks.data.TaskRepository
import az.theternal.cmplayground.feature.tasks.domain.Task
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val UNKNOWN_ERROR = "Something went wrong."

/**
 * The same screen contract without Orbit: a `MutableStateFlow` of the state class, a buffered
 * `Channel` for one-shot events, and public methods instead of an intent type — the shape a
 * hand-rolled MVI base class ends up with.
 *
 * Nothing in `TaskDetailScreen` or in the widgets below it changes because of that — the view
 * layer only ever sees `State<TaskDetailState>` and a bag of callbacks.
 */
class TaskDetailViewModel(
    private val taskId: String,
    private val repository: TaskRepository = FakeTaskRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow(TaskDetailState())
    val state: StateFlow<TaskDetailState> = mutableState.asStateFlow()

    private val effectChannel = Channel<TaskDetailEffect>(Channel.BUFFERED)
    val effects: Flow<TaskDetailEffect> = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.task(taskId) }
                .onSuccess { task -> mutableState.update { it.withTask(task) } }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: UNKNOWN_ERROR)
                    }
                }
        }
    }

    fun changeDone(isDone: Boolean) {
        mutableState.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            runCatching { repository.setDone(taskId, isDone) }
                .onSuccess { task -> mutableState.update { it.withTask(task) } }
                .onFailure { error ->
                    mutableState.update { it.copy(isUpdating = false) }
                    effectChannel.trySend(
                        TaskDetailEffect.ShowMessage(error.message ?: UNKNOWN_ERROR),
                    )
                }
        }
    }

    private fun TaskDetailState.withTask(task: Task): TaskDetailState = copy(
        isLoading = false,
        isUpdating = false,
        errorMessage = null,
        title = task.title,
        note = task.note,
        priority = task.priority,
        isDone = task.isDone,
    )
}
