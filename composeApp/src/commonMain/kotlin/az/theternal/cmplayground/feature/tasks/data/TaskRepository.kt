package az.theternal.cmplayground.feature.tasks.data

import az.theternal.cmplayground.feature.tasks.domain.Task
import az.theternal.cmplayground.feature.tasks.domain.TaskDraft
import az.theternal.cmplayground.feature.tasks.domain.TaskPage
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TaskRepository {

    /** Page size the backend paginates by, so callers do not have to guess the next page index. */
    val pageSize: Int

    suspend fun loadPage(page: Int): TaskPage

    suspend fun task(id: String): Task

    suspend fun setDone(id: String, isDone: Boolean): Task

    suspend fun save(draft: TaskDraft): Task

    suspend fun delete(id: String)

    /** Playground switch: makes the next [loadPage] fail so the error path can be exercised. */
    fun failNextLoad()
}

class TaskFailure(message: String) : Exception(message)

/**
 * In-memory fake with artificial latency. A singleton so the list and the detail screen — which
 * deliberately run on different architectures — share one source of data.
 */
object FakeTaskRepository : TaskRepository {

    private const val PAGE_SIZE = 8
    private const val PAGE_COUNT = 3
    private const val LOAD_DELAY_MS = 700L
    private const val MUTATION_DELAY_MS = 450L

    override val pageSize: Int = PAGE_SIZE

    private val mutex = Mutex()
    private var nextId = 0
    private var shouldFail = false
    private val tasks = mutableListOf<Task>()

    private val titles = listOf(
        "Split the state class per component",
        "Measure recomposition counts",
        "Replace lambda readers with State",
        "Move filtering off the main thread",
        "Write preview for every widget",
        "Check strong skipping report",
        "Audit derivedStateOf usage",
        "Delete the god ViewModel",
        "Introduce component state classes",
        "Benchmark the list scroll",
        "Document the effect contract",
        "Drop the redundant remember",
    )

    override suspend fun loadPage(page: Int): TaskPage {
        delay(LOAD_DELAY_MS)
        return mutex.withLock {
            if (shouldFail) {
                shouldFail = false
                throw TaskFailure("Could not reach the task service.")
            }
            seedUpTo(page)
            val from = page * PAGE_SIZE
            val to = minOf(from + PAGE_SIZE, tasks.size)
            TaskPage(
                tasks = if (from >= to) emptyList() else tasks.subList(from, to).toList(),
                hasMore = page + 1 < PAGE_COUNT,
            )
        }
    }

    override suspend fun task(id: String): Task {
        delay(LOAD_DELAY_MS)
        return mutex.withLock {
            tasks.firstOrNull { it.id == id } ?: throw TaskFailure("Task $id no longer exists.")
        }
    }

    override suspend fun setDone(id: String, isDone: Boolean): Task {
        delay(MUTATION_DELAY_MS)
        return mutex.withLock {
            val index = tasks.indexOfFirst { it.id == id }
            if (index < 0) throw TaskFailure("Task $id no longer exists.")
            tasks[index].copy(isDone = isDone).also { tasks[index] = it }
        }
    }

    override suspend fun save(draft: TaskDraft): Task {
        delay(MUTATION_DELAY_MS)
        return mutex.withLock {
            val index = tasks.indexOfFirst { it.id == draft.id }
            if (index >= 0) {
                tasks[index].copy(
                    title = draft.title,
                    note = draft.note,
                    priority = draft.priority,
                ).also { tasks[index] = it }
            } else {
                Task(
                    id = newId(),
                    title = draft.title,
                    note = draft.note,
                    priority = draft.priority,
                    isDone = false,
                ).also { tasks.add(0, it) }
            }
        }
    }

    override suspend fun delete(id: String) {
        delay(MUTATION_DELAY_MS)
        mutex.withLock { tasks.removeAll { it.id == id } }
    }

    override fun failNextLoad() {
        shouldFail = true
    }

    private fun seedUpTo(page: Int) {
        val required = minOf((page + 1) * PAGE_SIZE, PAGE_COUNT * PAGE_SIZE)
        while (tasks.size < required) {
            val index = tasks.size
            tasks += Task(
                id = newId(),
                title = titles[index % titles.size],
                note = "Seeded item #$index of the paged fake backend.",
                priority = TaskPriority.entries[index % TaskPriority.entries.size],
                isDone = index % 4 == 0,
            )
        }
    }

    private fun newId(): String = "task-${nextId++}"
}
