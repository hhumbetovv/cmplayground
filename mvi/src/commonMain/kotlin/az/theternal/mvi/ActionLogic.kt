package az.theternal.mvi

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What happens when an action starts while another with the same key is still running. Every policy that
 * makes actions meet takes its key: there is no default, because a default shared by a whole class of
 * actions makes "follow user B" cancel "follow user A" without a sign that anything is wrong.
 */
sealed interface Policy {
    /** Both run. Independent work: a like on one post and on another. */
    data object Parallel : Policy

    /** The running one is cancelled — its result is stale before it arrives. Search as you type, a filter. */
    data class Latest(
        val key: Any,
    ) : Policy

    /** The new one is ignored while one runs. Submit, pay, pull to refresh. */
    data class Ignore(
        val key: Any,
    ) : Policy

    /** The new one waits for the running one. Writes that build on each other: reorder, append. */
    data class Queue(
        val key: Any,
    ) : Policy
}

/**
 * Level 2. An intent says what the user did; an action says what to do about it, with every parameter it
 * needs already gathered from the state. Processing then never reads a state that changed under it, and
 * the action, being plain data, is what a test asserts on.
 *
 * Every action runs under a [Policy] for its [key]. That is where races are decided — once, by name —
 * instead of by `if (isLoading) return` guards and hand-kept `Job`s. Latest also debounces for free: a
 * `delay` at the start of [process] is cancelled by the next keystroke.
 *
 * Results go back as [Partial]s, so the reduction is in one place and the processing is only IO.
 */
abstract class ActionLogic<S, I, A : Any, E> : Logic<S, I, E> {
    private val runner = ActionRunner()

    /**
     * Intent → action: read the state, gather the parameters, validate. Synchronous changes (a typed
     * character) are reduced here and need no action. Null when there is nothing to run.
     */
    protected abstract fun StoreScope<S, E>.toAction(intent: I): A?

    /** Actions that share a policy's key meet under it: `Policy.Latest("search")`, `Policy.Latest("follow:$id")`. */
    protected open fun policy(action: A): Policy = Policy.Parallel

    protected abstract suspend fun StoreScope<S, E>.process(action: A)

    final override fun StoreScope<S, E>.onIntent(intent: I) {
        toAction(intent)?.let { action -> dispatch(action) }
    }

    /** Runs [action] as if an intent had produced it — for work the logic starts itself (on start, on a timer). */
    protected fun StoreScope<S, E>.dispatch(action: A) {
        runner.run(scope = this, policy = policy(action)) { process(action) }
    }
}

/** The running actions of one host, by key. Called on the host's thread only. */
internal class ActionRunner {
    private val running = mutableMapOf<Any, Job>()
    private val queues = mutableMapOf<Any, Mutex>()

    fun run(
        scope: StoreScope<*, *>,
        policy: Policy,
        block: suspend () -> Unit,
    ) {
        when (policy) {
            Policy.Parallel -> scope.launch { block() }
            is Policy.Latest -> {
                running[policy.key]?.cancel()
                track(policy.key, scope.launch { block() })
            }
            is Policy.Ignore -> if (running[policy.key]?.isActive != true) {
                track(policy.key, scope.launch { block() })
            }
            is Policy.Queue -> {
                val queue = queues.getOrPut(policy.key) { Mutex() }
                scope.launch { queue.withLock { block() } }
            }
        }
    }

    private fun track(
        key: Any,
        job: Job,
    ) {
        running[key] = job
        job.invokeOnCompletion {
            if (running[key] === job) {
                running.remove(key)
            }
        }
    }
}
