package az.theternal.mvi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * What a screen sees of its presentation logic, at every level: the state to draw, where intents go, and
 * effects, each delivered once. A screen written against this does not change when its logic moves from
 * one level to another.
 */
interface Store<S, I, E> {
    val state: StateFlow<S>
    val effects: Flow<E>

    fun postIntent(intent: I)
}

/**
 * Hosts a [Logic] on [scope]: owns the state, delivers the effects, and handles every intent on the
 * caller's thread, in order. A ViewModel hosts one through [StoreViewModel]; a test hosts one directly.
 */
class StoreHost<S, I, E>(
    initialState: S,
    scope: CoroutineScope,
    private val logic: Logic<S, I, E>,
    private val onError: (Throwable) -> Unit = Throwable::printStackTrace,
) : Store<S, I, E> {
    private val mutableState = MutableStateFlow(initialState)
    private val effectChannel = Channel<E>(Channel.BUFFERED)

    override val state: StateFlow<S> = mutableState.asStateFlow()
    override val effects: Flow<E> = effectChannel.receiveAsFlow()

    private val storeScope = object : StoreScope<S, E> {
        override val state: S get() = mutableState.value
        override val states: Flow<S> get() = mutableState

        override val coroutineScope: CoroutineScope = CoroutineScope(
            scope.coroutineContext +
                SupervisorJob(scope.coroutineContext[Job]) +
                CoroutineExceptionHandler { _, error -> onError(error) },
        )

        override fun reduce(partial: Partial<S>) {
            mutableState.update(partial::reduce)
        }

        override fun emit(effect: E) {
            effectChannel.trySend(effect)
        }
    }

    init {
        logic.start(storeScope)
    }

    override fun postIntent(intent: I) {
        try {
            logic.handle(storeScope, intent)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            onError(error)
        }
    }
}
