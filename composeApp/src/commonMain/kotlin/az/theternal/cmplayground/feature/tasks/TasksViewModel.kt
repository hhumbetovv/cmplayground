package az.theternal.cmplayground.feature.tasks

import androidx.lifecycle.ViewModel
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
import az.theternal.cmplayground.feature.tasks.widget.TaskDeleteDialogState
import az.theternal.cmplayground.feature.tasks.widget.TaskEditorState
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
 * Orbit is the only architecture-specific thing about this screen, and it stops at this file: the
 * container reduces [TasksState], and everything below `TasksScreen` works off `State<TasksState>`.
 *
 * The container is left at its defaults — initial state plus `onCreate`, no custom settings. Orbit
 * reduces on its own background event loop, which is why the one place that cannot tolerate a
 * round trip, the text field, renders from
 * [az.theternal.cmplayground.core.state.TextInputState] instead. Work that is actually heavy —
 * filtering — is pushed to `Dispatchers.Default` explicitly.
 *
 * Every reduction below touches the narrowest part of the state it can: editing a task rewrites one
 * map entry and leaves the id lists alone, so the change reaches one row and not the list.
 */
class TasksViewModel(
    private val repository: TaskRepository = FakeTaskRepository,
) : ViewModel(), ContainerHost<TasksState, TasksEffect> {

    override val container: Container<TasksState, TasksEffect> =
        container(
            initialState = TasksState(),
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
        reduce { state.copy(isLoading = true, errorMessage = null) }
        loadPage(page = 0, replace = true)
    }

    private fun loadMore() = intent {
        val current = state
        if (current.isLoading || current.isLoadingMore || !current.canLoadMore) return@intent

        reduce { state.copy(isLoadingMore = true) }
        loadPage(page = current.taskIds.size / repository.pageSize, replace = false)
    }

    private fun simulateFailure() = intent {
        repository.failNextLoad()
        reduce { state.copy(isLoading = true, errorMessage = null) }
        loadPage(page = 0, replace = true)
    }

    private suspend fun Syntax<TasksState, TasksEffect>.loadPage(page: Int, replace: Boolean) {
        runCatching { repository.loadPage(page) }
            .onSuccess { loaded ->
                val items = loaded.tasks.map { it.toItemState() }
                reduce { state.mergePage(items, replace = replace, canLoadMore = loaded.hasMore) }
                applyFilters()
            }
            .onFailure { error ->
                reduce {
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        // A failed "load more" must not blank out the page already on screen.
                        errorMessage = if (replace) error.message ?: UNKNOWN_ERROR else null,
                    )
                }
                if (!replace) {
                    postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
                }
            }
    }

    // endregion

    // region query & filters

    private fun changeQuery(query: String) = intent {
        reduce { state.copy(query = query) }
        applyFilters()
    }

    private fun toggleFilter(filter: TaskFilter) = intent {
        reduce {
            val filters = state.filters
            state.copy(
                filters = if (filter in filters) {
                    (filters - filter).toPersistentSet()
                } else {
                    (filters + filter).toPersistentSet()
                },
            )
        }
        applyFilters()
    }

    private fun clearFilters() = intent {
        reduce { state.copy(query = "", filters = persistentSetOf()) }
        applyFilters()
    }

    /**
     * Filtering is the one piece of real work on this screen, so it runs off the main thread and
     * its result is dropped if the inputs moved on while it was running — a later intent has
     * already scheduled its own pass.
     *
     * When the result is unchanged it is still assigned, and that is harmless: the new id list is
     * `equals` to the old one, so every derivation above it stops there.
     */
    private suspend fun Syntax<TasksState, TasksEffect>.applyFilters() {
        val snapshot = state
        val visible = withContext(Dispatchers.Default) {
            filterTaskIds(snapshot.taskIds, snapshot.tasksById, snapshot.query, snapshot.filters)
        }
        reduce {
            val isStale = state.query != snapshot.query ||
                state.filters != snapshot.filters ||
                state.taskIds !== snapshot.taskIds ||
                state.tasksById !== snapshot.tasksById

            if (isStale) state else state.copy(visibleTaskIds = visible)
        }
    }

    // endregion

    // region items

    private fun openTask(id: String) = intent {
        postSideEffect(TasksEffect.OpenTask(id))
    }

    private fun changeTaskDone(id: String, isDone: Boolean) = intent {
        reduce { state.updateTask(id) { copy(isBusy = true) } }

        runCatching { repository.setDone(id, isDone) }
            .onSuccess { task ->
                reduce { state.updateTask(id) { task.toItemState() } }
                applyFilters()
            }
            .onFailure { error ->
                reduce { state.updateTask(id) { copy(isBusy = false) } }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    // region editor

    private fun openEditor(taskId: String?) = intent {
        val task = taskId?.let { state.tasksById[it] }
        reduce {
            state.copy(
                editor = TaskEditorState(
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
        reduce { state.updateEditor { copy(title = title, titleError = null) } }
    }

    private fun changeEditorNote(note: String) = intent {
        reduce { state.updateEditor { copy(note = note) } }
    }

    private fun changeEditorPriority(priority: TaskPriority) = intent {
        reduce { state.updateEditor { copy(priority = priority) } }
    }

    private fun dismissEditor() = intent {
        reduce { state.copy(editor = null) }
    }

    private fun submitEditor() = intent {
        val editor = state.editor ?: return@intent

        if (editor.title.isBlank()) {
            reduce { state.updateEditor { copy(titleError = EMPTY_TITLE_ERROR) } }
            return@intent
        }

        reduce { state.updateEditor { copy(isSaving = true) } }

        val draft = TaskDraft(
            id = editor.taskId,
            title = editor.title.trim(),
            note = editor.note.trim(),
            priority = editor.priority,
        )

        runCatching { repository.save(draft) }
            .onSuccess { saved ->
                reduce { state.copy(editor = null).upsertTask(saved.toItemState()) }
                applyFilters()
                postSideEffect(
                    TasksEffect.ShowMessage(if (editor.isEditing) "Task updated." else "Task created."),
                )
            }
            .onFailure { error ->
                reduce { state.updateEditor { copy(isSaving = false) } }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    // region deletion

    private fun requestDelete(id: String) = intent {
        val task = state.tasksById[id] ?: return@intent
        reduce {
            state.copy(
                deleteTarget = TaskDeleteDialogState(
                    taskId = task.id,
                    title = task.title,
                    isDeleting = false,
                ),
            )
        }
    }

    private fun dismissDelete() = intent {
        reduce { state.copy(deleteTarget = null) }
    }

    private fun confirmDelete() = intent {
        val target = state.deleteTarget ?: return@intent
        reduce { state.copy(deleteTarget = state.deleteTarget?.copy(isDeleting = true)) }

        runCatching { repository.delete(target.taskId) }
            .onSuccess {
                reduce { state.copy(deleteTarget = null).removeTask(target.taskId) }
                postSideEffect(TasksEffect.ShowMessage("Task deleted."))
            }
            .onFailure { error ->
                reduce { state.copy(deleteTarget = state.deleteTarget?.copy(isDeleting = false)) }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    /**
     * Offset paging over a list the user can also insert into: a locally created task shifts the
     * backend's window, so the next page can hand back an id that is already on screen. Ids are the
     * identity the list is keyed by, so a duplicate would be a crash — the merge drops what it
     * already has, while still refreshing the data behind every id it received.
     */
    private fun TasksState.mergePage(
        items: List<TaskItemState>,
        replace: Boolean,
        canLoadMore: Boolean,
    ): TasksState {
        val currentIds = if (replace) persistentListOf() else taskIds
        val currentById = if (replace) persistentMapOf() else tasksById
        val knownIds = currentIds.toHashSet()

        return copy(
            isLoading = false,
            isLoadingMore = false,
            errorMessage = null,
            taskIds = (currentIds + items.map { it.id }.filterNot { it in knownIds })
                .toPersistentList(),
            tasksById = currentById.toPersistentMap().putAll(items.associateBy { it.id }),
            canLoadMore = canLoadMore,
        )
    }

    /** Rewrites one map entry; both id lists keep their identity, so the list component skips. */
    private fun TasksState.updateTask(
        id: String,
        transform: TaskItemState.() -> TaskItemState,
    ): TasksState {
        val task = tasksById[id] ?: return this
        return copy(tasksById = tasksById.toPersistentMap().put(id, task.transform()))
    }

    private fun TasksState.upsertTask(item: TaskItemState): TasksState =
        if (tasksById.containsKey(item.id)) {
            updateTask(item.id) { item }
        } else {
            copy(
                taskIds = taskIds.toPersistentList().add(index = 0, element = item.id),
                tasksById = tasksById.toPersistentMap().put(item.id, item),
            )
        }

    /**
     * Drops the id from every list in the same reduction as the entry itself, so no composition
     * ever sees an id whose task is already gone.
     */
    private fun TasksState.removeTask(id: String): TasksState = copy(
        taskIds = taskIds.toPersistentList().remove(id),
        visibleTaskIds = visibleTaskIds.toPersistentList().remove(id),
        tasksById = tasksById.toPersistentMap().remove(id),
    )

    private fun TasksState.updateEditor(transform: TaskEditorState.() -> TaskEditorState): TasksState =
        copy(editor = editor?.transform())
}
