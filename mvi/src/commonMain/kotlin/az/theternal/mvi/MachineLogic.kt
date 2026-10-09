package az.theternal.mvi

import dev.klitsie.statemachine.StateMachine
import dev.klitsie.statemachine.StateMachineBuilder
import dev.klitsie.statemachine.stateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted

/**
 * Level 3. The state is one of several exclusive phases, and the transitions between them are the logic:
 * a graph built with dev.klitsie.statemachine. An event a phase does not handle is ignored, so the
 * `if (isSubmitting) return` guards disappear, and impossible combinations of flags cannot be written.
 * Work that belongs to a phase is its `sideEffect`: it starts on entering the phase and is cancelled on
 * leaving it.
 *
 * Intent → event is the same parameter-gathering step as intent → action at level 2. It also keeps the
 * machine's own events (a timer's tick, a request's result) out of the screen's reach.
 *
 * The machine runs eagerly on the host's scope — never the library's default `WhileSubscribed(5s)`, which
 * stops a phase's side effects (an upload, a countdown) five seconds after the screen leaves the
 * foreground — and every state it enters is written into the host's state, so it also runs as a [Slice].
 */
class MachineLogic<S : Any, I, Ev : Any, E : Any>
@PublishedApi
internal constructor(
    private val create: (scope: CoroutineScope, initialState: S) -> StateMachine<S, E, Ev>,
    private val toEvent: StoreScope<S, E>.(I) -> Ev?,
) : Logic<S, I, E> {
    private var machine: StateMachine<S, E, Ev>? = null

    override fun StoreScope<S, E>.onStart() {
        val machine = create(coroutineScope, state)
        this@MachineLogic.machine = machine
        launch { machine.collect { phase -> setState { phase } } }
        launch { machine.consumeEffects { effect -> emit(effect) } }
    }

    override fun StoreScope<S, E>.onIntent(intent: I) {
        val event = toEvent(intent) ?: return
        checkNotNull(machine) { "The machine is not started" }.send(event)
    }
}

/**
 * A [MachineLogic]: [toEvent] turns intents into the machine's events, [graph] is the machine.
 *
 * A `sideEffect` restarts whenever its key changes, and the key is the whole state by default — a phase
 * that updates its own data (an upload's progress) must key its side effect on what identifies the work,
 * or the work restarts on every update.
 */
inline fun <reified S : Any, I, Ev : Any, E : Any> machineLogic(
    noinline toEvent: StoreScope<S, E>.(I) -> Ev?,
    noinline graph: StateMachineBuilder<S, S, E, Ev>.() -> Unit,
): Logic<S, I, E> {
    return MachineLogic(
        create = { scope, initialState ->
            stateMachine<S, E, Ev>(
                scope = scope,
                initialState = initialState,
                started = SharingStarted.Eagerly,
                builder = graph,
            )
        },
        toEvent = toEvent,
    )
}
