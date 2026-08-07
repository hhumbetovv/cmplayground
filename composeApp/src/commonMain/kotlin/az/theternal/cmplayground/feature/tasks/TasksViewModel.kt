package az.theternal.cmplayground.feature.tasks

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.core.state.StateWriter
import az.theternal.cmplayground.core.state.reduceState
import az.theternal.cmplayground.feature.tasks.contract.TasksEffect
import az.theternal.cmplayground.feature.tasks.contract.TasksIntent
import az.theternal.cmplayground.feature.tasks.contract.TasksState
import az.theternal.cmplayground.feature.tasks.data.FakeTaskRepository
import az.theternal.cmplayground.feature.tasks.data.TaskRepository
import az.theternal.cmplayground.feature.tasks.domain.TaskDraft
import az.theternal.cmplayground.feature.tasks.domain.TaskFilter
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority
import az.theternal.cmplayground.feature.tasks.mapper.filterTaskIds
import az.theternal.cmplayground.feature.tasks.mapper.toItemState
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteData
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorData
import az.theternal.cmplayground.feature.tasks.widget.TaskItemState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

private const val UNKNOWN_ERROR = "Something went wrong."
private const val EMPTY_TITLE_ERROR = "Title cannot be empty."

/**
 * Orbit still owns the plumbing — intents are serialised on its event loop, effects go through
 * `postSideEffect`, the container is the state's home — but the state itself is a field holder, so
 * an intent writes the fields it changed instead of rebuilding the screen's state.
 *
 * The container's state never changes identity, so nothing collects it. The UI reads the fields,
 * and Compose's snapshot system delivers each write to exactly the composables that read it. That
 * is why there is no `reduce` here: there is nothing to reduce into.
 *
 * This class declares nothing beyond `ViewModel` and `ContainerHost`. Writes go through
 * `reduceState { }`, a `ViewModel` extension that puts a [StateWriter] in scope for the block and
 * applies everything inside it as one snapshot.
 */
class TasksViewModel(
    private val repository: TaskRepository = FakeTaskRepository,
) : ViewModel(), ContainerHost<TasksState, TasksEffect> {

    val state = TasksState()

    override val container: Container<TasksState, TasksEffect> =
        container(
            initialState = state,
            onCreate = { loadPage(page = 0, replace = true) },
        )

    fun dispatch(intent: TasksIntent) {
        when (intent) {
            TasksIntent.Retry -> refresh()
            TasksIntent.LoadMore -> loadMore()
            TasksIntent.SimulateFailure -> simulateFailure()
            is TasksIntent.QueryChanged -> changeQuery(intent.query)
            TasksIntent.QueryCleared -> changeQuery("")
            TasksIntent.FiltersCleared -> clearFilters()
            is TasksIntent.FilterToggled -> toggleFilter(intent.filter)
            is TasksIntent.TaskOpened -> openTask(intent.id)
            is TasksIntent.TaskDoneChanged -> changeTaskDone(intent.id, intent.isDone)
            TasksIntent.CreateRequested -> openEditor(taskId = null)
            is TasksIntent.EditorTitleChanged -> changeEditorTitle(intent.title)
            is TasksIntent.EditorNoteChanged -> changeEditorNote(intent.note)
            is TasksIntent.EditorPriorityChanged -> changeEditorPriority(intent.priority)
            TasksIntent.EditorSubmitted -> submitEditor()
            TasksIntent.EditorDismissed -> dismissEditor()
            is TasksIntent.DeleteRequested -> requestDelete(intent.id)
            TasksIntent.DeleteConfirmed -> confirmDelete()
            TasksIntent.DeleteDismissed -> dismissDelete()
        }
    }

    // region loading

    private fun refresh() = intent {
        reduceState {
            state.isLoading.set(true)
            state.errorMessage.set(null)
        }
        loadPage(page = 0, replace = true)
    }

    private fun loadMore() = intent {
        if (state.isLoading.value || state.isLoadingMore.value || !state.canLoadMore.value) {
            return@intent
        }

        reduceState { state.isLoadingMore.set(true) }
        loadPage(page = state.taskIds.value.size / repository.pageSize, replace = false)
    }

    private fun simulateFailure() = intent {
        repository.failNextLoad()
        reduceState {
            state.isLoading.set(true)
            state.errorMessage.set(null)
        }
        loadPage(page = 0, replace = true)
    }

    private suspend fun Syntax<TasksState, TasksEffect>.loadPage(page: Int, replace: Boolean) {
        runCatching { repository.loadPage(page) }
            .onSuccess { loaded ->
                val items = loaded.tasks.map { it.toItemState() }
                reduceState {
                    mergePage(items, replace = replace)
                    state.canLoadMore.set(loaded.hasMore)
                    state.isLoading.set(false)
                    state.isLoadingMore.set(false)
                    state.errorMessage.set(null)
                }
                applyFilters()
            }
            .onFailure { error ->
                val message = error.message ?: UNKNOWN_ERROR
                reduceState {
                    state.isLoading.set(false)
                    state.isLoadingMore.set(false)
                    // A failed "load more" must not blank out the page already on screen.
                    if (replace) state.errorMessage.set(message)
                }
                if (!replace) postSideEffect(TasksEffect.ShowMessage(message))
            }
    }

    // endregion

    // region query & filters

    private fun changeQuery(query: String) = intent {
        reduceState { state.query.set(query) }
        applyFilters()
    }

    private fun toggleFilter(filter: TaskFilter) = intent {
        reduceState {
            state.filters.update {
                if (filter in this) {
                    (this - filter).toPersistentSet()
                } else {
                    (this + filter).toPersistentSet()
                }
            }
        }
        applyFilters()
    }

    private fun clearFilters() = intent {
        reduceState {
            state.query.set("")
            state.filters.set(persistentSetOf())
        }
        applyFilters()
    }

    /**
     * Filtering is the one piece of real work on this screen, so it runs off the main thread. Its
     * result is dropped if any input moved on while it was running — a later intent has already
     * scheduled its own pass.
     */
    private suspend fun Syntax<TasksState, TasksEffect>.applyFilters() {
        val ids = state.taskIds.value
        val tasksById = state.tasksById.value
        val query = state.query.value
        val filters = state.filters.value

        val visible = withContext(Dispatchers.Default) {
            filterTaskIds(ids, tasksById, query, filters)
        }

        val isStale = state.taskIds.value !== ids ||
            state.tasksById.value !== tasksById ||
            state.query.value != query ||
            state.filters.value != filters

        if (!isStale) reduceState { state.visibleTaskIds.set(visible) }
    }

    // endregion

    // region items

    private fun openTask(id: String) = intent {
        postSideEffect(TasksEffect.OpenTask(id))
    }

    private fun changeTaskDone(id: String, isDone: Boolean) = intent {
        reduceState { putTask(id) { copy(isBusy = true) } }

        runCatching { repository.setDone(id, isDone) }
            .onSuccess { task ->
                reduceState { putTask(id) { task.toItemState() } }
                applyFilters()
            }
            .onFailure { error ->
                reduceState { putTask(id) { copy(isBusy = false) } }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    // region editor

    private fun openEditor(taskId: String?) = intent {
        val task = taskId?.let { state.tasksById.value[it] }
        reduceState {
            state.editor.set(
                TaskEditorData(
                    taskId = task?.id,
                    title = task?.title.orEmpty(),
                    note = task?.note.orEmpty(),
                    priority = task?.priority ?: TaskPriority.NORMAL,
                    isSaving = false,
                    titleError = null,
                ),
            )
        }
    }

    private fun changeEditorTitle(title: String) = intent {
        reduceState { state.editor.update { this?.copy(title = title, titleError = null) } }
    }

    private fun changeEditorNote(note: String) = intent {
        reduceState { state.editor.update { this?.copy(note = note) } }
    }

    private fun changeEditorPriority(priority: TaskPriority) = intent {
        reduceState { state.editor.update { this?.copy(priority = priority) } }
    }

    private fun dismissEditor() = intent {
        reduceState { state.editor.set(null) }
    }

    private fun submitEditor() = intent {
        val current = state.editor.value ?: return@intent

        if (current.title.isBlank()) {
            reduceState { state.editor.set(current.copy(titleError = EMPTY_TITLE_ERROR)) }
            return@intent
        }

        reduceState { state.editor.set(current.copy(isSaving = true)) }

        val draft = TaskDraft(
            id = current.taskId,
            title = current.title.trim(),
            note = current.note.trim(),
            priority = current.priority,
        )

        runCatching { repository.save(draft) }
            .onSuccess { saved ->
                reduceState {
                    state.editor.set(null)
                    upsertTask(saved.toItemState())
                }
                applyFilters()
                postSideEffect(
                    TasksEffect.ShowMessage(if (current.isEditing) "Task updated." else "Task created."),
                )
            }
            .onFailure { error ->
                reduceState { state.editor.update { this?.copy(isSaving = false) } }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    // region deletion

    private fun requestDelete(id: String) = intent {
        val task = state.tasksById.value[id] ?: return@intent
        reduceState {
            state.deleteTarget.set(
                TaskDeleteData(taskId = task.id, title = task.title, isDeleting = false),
            )
        }
    }

    private fun dismissDelete() = intent {
        reduceState { state.deleteTarget.set(null) }
    }

    private fun confirmDelete() = intent {
        val target = state.deleteTarget.value ?: return@intent
        reduceState { state.deleteTarget.set(target.copy(isDeleting = true)) }

        runCatching { repository.delete(target.taskId) }
            .onSuccess {
                reduceState {
                    state.deleteTarget.set(null)
                    removeTask(target.taskId)
                }
                postSideEffect(TasksEffect.ShowMessage("Task deleted."))
            }
            .onFailure { error ->
                reduceState { state.deleteTarget.update { this?.copy(isDeleting = false) } }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    // region field writes

    /**
     * Offset paging over a list the user can also insert into: a locally created task shifts the
     * backend's window, so the next page can hand back an id that is already on screen. Ids are the
     * identity the list is keyed by, so a duplicate would be a crash — the merge drops ids it
     * already has, while still refreshing the data behind every id it received.
     */
    private fun StateWriter.mergePage(items: List<TaskItemState>, replace: Boolean) {
        val currentIds = if (replace) persistentListOf() else state.taskIds.value
        val currentById = if (replace) persistentMapOf() else state.tasksById.value
        val knownIds = currentIds.toHashSet()

        state.taskIds.set(
            (currentIds + items.map { it.id }.filterNot { it in knownIds }).toPersistentList(),
        )
        state.tasksById.set(currentById.toPersistentMap().putAll(items.associateBy { it.id }))
    }

    /** Rewrites one map entry; both id lists keep their value, so the list component skips. */
    private fun StateWriter.putTask(id: String, transform: TaskItemState.() -> TaskItemState) {
        val task = state.tasksById.value[id] ?: return
        state.tasksById.set(state.tasksById.value.toPersistentMap().put(id, task.transform()))
    }

    private fun StateWriter.upsertTask(item: TaskItemState) {
        if (state.tasksById.value.containsKey(item.id)) {
            putTask(item.id) { item }
        } else {
            state.taskIds.set(
                state.taskIds.value.toPersistentList().add(index = 0, element = item.id),
            )
            state.tasksById.set(state.tasksById.value.toPersistentMap().put(item.id, item))
        }
    }

    /**
     * Drops the id from every list in the same snapshot as the entry itself, so no composition ever
     * sees an id whose task is already gone.
     */
    private fun StateWriter.removeTask(id: String) {
        state.taskIds.set(state.taskIds.value.toPersistentList().remove(id))
        state.visibleTaskIds.set(state.visibleTaskIds.value.toPersistentList().remove(id))
        state.tasksById.set(state.tasksById.value.toPersistentMap().remove(id))
    }

    // endregion
}
