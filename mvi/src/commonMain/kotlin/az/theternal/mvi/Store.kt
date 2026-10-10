package az.theternal.mvi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * What a screen sees of its presentation logic, at every level: the state to draw, where intents go, and
 * effects, each delivered once. A screen written against this does not change when its logic moves from
 * one level to another.
 */
interface Store<S, I, E> {
    val state: StateFlow<S>

    /**
     * Delivered once, to one collector. An effect emitted while no one collects — the screen is in the
     * background — waits for the next collector instead of being lost. A second collector at the same
     * time is a bug (two screens would race for each effect), and fails at once.
     */
    val effects: Flow<E>

    fun postIntent(intent: I)
}

/**
 * The store with its state mapped for the screen: the logic keeps the state it needs (phases, ids, flags)
 * and the screen sees only what it draws — Orbit's internal and external state. The levels of the
 * sign-in sample keep three different states behind one `Store<SignInUi, …>`.
 *
 * [transform] runs on every read, so it must be pure and cheap; per-component mapping stays in `select`.
 */
fun <S, U, I, E> Store<S, I, E>.mapState(transform: (S) -> U): Store<U, I, E> {
    val source = this
    return object : Store<U, I, E> {
        override val state: StateFlow<U> = MappedStateFlow(source.state, transform)
        override val effects: Flow<E> get() = source.effects

        override fun postIntent(intent: I) {
            source.postIntent(intent)
        }
    }
}

/**
 * Hosts a [Logic] on [scope]: owns the state, delivers the effects, and handles every intent on the
 * caller's thread, in order. A ViewModel hosts one through [store] and hands it out as a [StoreOwner]; a
 * test hosts one directly.
 *
 * With a [keeper], the state starts from what the keeper saved before the process died, and the part the
 * keeper keeps is saved again whenever it changes.
 */
class StoreHost<S, I, E>(
    initialState: S,
    scope: CoroutineScope,
    private val logic: Logic<S, I, E>,
    private val settings: StoreSettings = StoreSettings(),
    keeper: StateKeeper<S>? = null,
) : Store<S, I, E> {
    private val mutableState = MutableStateFlow(keeper?.restore(initialState) ?: initialState)
    private val effectChannel = Channel<E>(Channel.UNLIMITED)
    private val effectCollector = Mutex()
    private var isStarted = false

    /**
     * How many collect the state the way a screen does, through [state]. Not `mutableState.subscriptionCount`:
     * that also counts the store's own collectors (a keeper, `whileIn`, `runOn`), and the work that runs
     * "while the screen is visible" would then never stop.
     */
    private val screens = MutableStateFlow(0)

    override val state: StateFlow<S> = StartingStateFlow()

    override val effects: Flow<E> = flow {
        check(effectCollector.tryLock()) {
            "The effects already have a collector. Each effect goes to one collector; collect them in one place."
        }
        try {
            startIfLazy()
            emitAll(effectChannel.receiveAsFlow())
        } finally {
            effectCollector.unlock()
        }
    }

    private val storeScope = object : StoreScope<S, E> {
        override val state: S get() = mutableState.value
        override val states: Flow<S> get() = mutableState
        override val subscribers: StateFlow<Int> get() = screens

        override val coroutineScope: CoroutineScope = CoroutineScope(
            scope.coroutineContext +
                SupervisorJob(scope.coroutineContext[Job]) +
                CoroutineExceptionHandler { _, error -> settings.onError(error) },
        )

        override fun reduce(partial: Partial<S>) {
            val observer = settings.observer
            if (observer == null) {
                mutableState.update(partial::reduce)
                return
            }
            var previous = mutableState.value
            var next = previous
            mutableState.update { state ->
                previous = state
                next = partial.reduce(state)
                next
            }
            if (previous != next) observer.onState(partial, previous, next)
        }

        override fun emit(effect: E) {
            settings.observer?.onEffect(effect)
            effectChannel.trySend(effect)
        }
    }

    init {
        keeper?.let { keeper -> storeScope.coroutineScope.launch { keeper.save(mutableState) } }
        if (settings.start == Start.Eagerly) start()
    }

    /** Runs the logic's `onStart`, once. Needed only with [Start.Manually]; harmless otherwise. */
    fun start() {
        if (isStarted) return
        isStarted = true
        try {
            logic.start(storeScope)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            settings.onError(error)
        }
    }

    override fun postIntent(intent: I) {
        startIfLazy()
        settings.observer?.onIntent(intent)
        try {
            logic.handle(storeScope, intent)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            settings.onError(error)
        }
    }

    private fun startIfLazy() {
        if (settings.start == Start.Lazily) start()
    }

    /** The state, which starts a lazy store when someone first collects it. Reading `value` does not. */
    @OptIn(ExperimentalForInheritanceCoroutinesApi::class)
    private inner class StartingStateFlow : StateFlow<S> {
        override val value: S get() = mutableState.value
        override val replayCache: List<S> get() = mutableState.replayCache

        override suspend fun collect(collector: FlowCollector<S>): Nothing {
            startIfLazy()
            screens.update { it + 1 }
            try {
                mutableState.collect(collector)
            } finally {
                screens.update { it - 1 }
            }
        }
    }
}

/** [source] mapped with [transform], still a [StateFlow]: no scope, nothing kept, computed on read. */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class MappedStateFlow<T, R>(
    private val source: StateFlow<T>,
    private val transform: (T) -> R,
) : StateFlow<R> {
    override val value: R get() = transform(source.value)
    override val replayCache: List<R> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        coroutineScope { source.map(transform).distinctUntilChanged().collect(collector) }
        awaitCancellation()
    }
}
