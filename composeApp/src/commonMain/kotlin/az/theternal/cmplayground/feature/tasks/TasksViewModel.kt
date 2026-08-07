package az.theternal.cmplayground.feature.tasks

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.core.state.snapshot
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
 * Orbit owns the plumbing exactly as it ships: intents are serialised on its event loop, effects go
 * through `postSideEffect`, and the state lives in the container. The only difference from a
 * reducing ViewModel is what the state is — a holder of fields rather than a value — so `reduce`
 * writes fields and hands the same holder back.
 *
 * The holder's identity never changes, so nothing collects it. The UI reads the fields, and
 * Compose's snapshot system delivers each write to exactly the composables that read it.
 *
 * This class declares nothing beyond `ViewModel` and `ContainerHost`. `set` is available inside it
 * because it takes a `ViewModel` context parameter — outside a ViewModel it does not resolve at
 * all.
 */
class TasksViewModel(
    private val repository: TaskRepository = FakeTaskRepository,
) : ViewModel(), ContainerHost<TasksState, TasksEffect> {

    /** What the UI reads. The holder is the container's state and never changes identity. */
    val state: TasksState get() = container.stateFlow.value

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
        reduce {
            state.snapshot {
                isLoading.snapshot(true)
                errorMessage.snapshot(null)
            }
        }
        loadPage(page = 0, replace = true)
    }

    private fun loadMore() = intent {
        if (state.isLoading.value || state.isLoadingMore.value || !state.canLoadMore.value) {
            return@intent
        }

        reduce {
            state.isLoadingMore.snapshot(true)
            state
        }
        loadPage(page = state.taskIds.value.size / repository.pageSize, replace = false)
    }

    private fun simulateFailure() = intent {
        repository.failNextLoad()
        reduce {
            state.snapshot {
                isLoading.snapshot(true)
                errorMessage.snapshot(null)
            }
        }
        loadPage(page = 0, replace = true)
    }

    private suspend fun Syntax<TasksState, TasksEffect>.loadPage(page: Int, replace: Boolean) {
        runCatching { repository.loadPage(page) }
            .onSuccess { loaded ->
                val items = loaded.tasks.map { it.toItemState() }
                reduce {
                    state.snapshot {
                        mergePage(items, replace = replace)
                        canLoadMore.snapshot(loaded.hasMore)
                        isLoading.snapshot(false)
                        isLoadingMore.snapshot(false)
                        errorMessage.snapshot(null)
                    }
                }
                applyFilters()
            }
            .onFailure { error ->
                val message = error.message ?: UNKNOWN_ERROR
                reduce {
                    state.snapshot {
                        isLoading.snapshot(false)
                        isLoadingMore.snapshot(false)
                        // A failed "load more" must not blank out the page already on screen.
                        if (replace) errorMessage.snapshot(message)
                    }
                }
                if (!replace) postSideEffect(TasksEffect.ShowMessage(message))
            }
    }

    // endregion

    // region query & filters

    private fun changeQuery(query: String) = intent {
        reduce {
            state.query.snapshot(query)
            state
        }
        applyFilters()
    }

    private fun toggleFilter(filter: TaskFilter) = intent {
        reduce {
            state.filters.snapshot {
                if (filter in this) {
                    (this - filter).toPersistentSet()
                } else {
                    (this + filter).toPersistentSet()
                }
            }
            state
        }
        applyFilters()
    }

    private fun clearFilters() = intent {
        reduce {
            state.snapshot {
                query.snapshot("")
                filters.snapshot(persistentSetOf())
            }
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

        if (!isStale) {
            reduce {
                state.visibleTaskIds.snapshot(visible)
                state
            }
        }
    }

    // endregion

    // region items

    private fun openTask(id: String) = intent {
        postSideEffect(TasksEffect.OpenTask(id))
    }

    private fun changeTaskDone(id: String, isDone: Boolean) = intent {
        reduce {
            state.putTask(id) { copy(isBusy = true) }
            state
        }

        runCatching { repository.setDone(id, isDone) }
            .onSuccess { task ->
                reduce {
            state.putTask(id) { task.toItemState() }
            state
        }
                applyFilters()
            }
            .onFailure { error ->
                reduce {
            state.putTask(id) { copy(isBusy = false) }
            state
        }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    // region editor

    private fun openEditor(taskId: String?) = intent {
        val task = taskId?.let { state.tasksById.value[it] }
        reduce {
            state.editor.snapshot(
                TaskEditorData(
                    taskId = task?.id,
                    title = task?.title.orEmpty(),
                    note = task?.note.orEmpty(),
                    priority = task?.priority ?: TaskPriority.NORMAL,
                    isSaving = false,
                    titleError = null,
                ),
            )
            state
        }
    }

    private fun changeEditorTitle(title: String) = intent {
        reduce {
            state.editor.snapshot { this?.copy(title = title, titleError = null) }
            state
        }
    }

    private fun changeEditorNote(note: String) = intent {
        reduce {
            state.editor.snapshot { this?.copy(note = note) }
            state
        }
    }

    private fun changeEditorPriority(priority: TaskPriority) = intent {
        reduce {
            state.editor.snapshot { this?.copy(priority = priority) }
            state
        }
    }

    private fun dismissEditor() = intent {
        reduce {
            state.editor.snapshot(null)
            state
        }
    }

    private fun submitEditor() = intent {
        val current = state.editor.value ?: return@intent

        if (current.title.isBlank()) {
            reduce {
                state.editor.snapshot(current.copy(titleError = EMPTY_TITLE_ERROR))
                state
            }
            return@intent
        }

        reduce {
            state.editor.snapshot(current.copy(isSaving = true))
            state
        }

        val draft = TaskDraft(
            id = current.taskId,
            title = current.title.trim(),
            note = current.note.trim(),
            priority = current.priority,
        )

        runCatching { repository.save(draft) }
            .onSuccess { saved ->
                reduce {
                    state.snapshot {
                        editor.snapshot(null)
                        upsertTask(saved.toItemState())
                    }
                }
                applyFilters()
                postSideEffect(
                    TasksEffect.ShowMessage(if (current.isEditing) "Task updated." else "Task created."),
                )
            }
            .onFailure { error ->
                reduce {
                    state.editor.snapshot { this?.copy(isSaving = false) }
                    state
                }
                postSideEffect(TasksEffect.ShowMessage(error.message ?: UNKNOWN_ERROR))
            }
    }

    // endregion

    // region deletion

    private fun requestDelete(id: String) = intent {
        val task = state.tasksById.value[id] ?: return@intent
        reduce {
            state.deleteTarget.snapshot(
                TaskDeleteData(taskId = task.id, title = task.title, isDeleting = false),
            )
            state
        }
    }

    private fun dismissDelete() = intent {
        reduce {
            state.deleteTarget.snapshot(null)
            state
        }
    }

    private fun confirmDelete() = intent {
        val target = state.deleteTarget.value ?: return@intent
        reduce {
            state.deleteTarget.snapshot(target.copy(isDeleting = true))
            state
        }

        runCatching { repository.delete(target.taskId) }
            .onSuccess {
                reduce {
                    state.snapshot {
                        deleteTarget.snapshot(null)
                        removeTask(target.taskId)
                    }
                }
                postSideEffect(TasksEffect.ShowMessage("Task deleted."))
            }
            .onFailure { error ->
                reduce {
                    state.deleteTarget.snapshot { this?.copy(isDeleting = false) }
                    state
                }
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
    private fun TasksState.mergePage(items: List<TaskItemState>, replace: Boolean) {
        val currentIds = if (replace) persistentListOf() else taskIds.value
        val currentById = if (replace) persistentMapOf() else tasksById.value
        val knownIds = currentIds.toHashSet()

        taskIds.snapshot(
            (currentIds + items.map { it.id }.filterNot { it in knownIds }).toPersistentList(),
        )
        tasksById.snapshot(currentById.toPersistentMap().puttingAll(items.associateBy { it.id }))
    }

    /** Rewrites one map entry; both id lists keep their value, so the list component skips. */
    private fun TasksState.putTask(id: String, transform: TaskItemState.() -> TaskItemState) {
        val task = tasksById.value[id] ?: return
        tasksById.snapshot(tasksById.value.toPersistentMap().putting(id, task.transform()))
    }

    private fun TasksState.upsertTask(item: TaskItemState) {
        if (tasksById.value.containsKey(item.id)) {
            putTask(item.id) { item }
        } else {
            taskIds.snapshot(
                taskIds.value.toPersistentList().addingAt(index = 0, element = item.id),
            )
            tasksById.snapshot(tasksById.value.toPersistentMap().putting(item.id, item))
        }
    }

    /**
     * Drops the id from every list in the same snapshot as the entry itself, so no composition ever
     * sees an id whose task is already gone.
     */
    private fun TasksState.removeTask(id: String) {
        taskIds.snapshot(taskIds.value.toPersistentList().removing(id))
        visibleTaskIds.snapshot(visibleTaskIds.value.toPersistentList().removing(id))
        tasksById.snapshot(tasksById.value.toPersistentMap().removing(id))
    }

    // endregion
}
