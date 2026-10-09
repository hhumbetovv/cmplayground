package az.theternal.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MachineLogicTest {
    private sealed interface Phase {
        data object Idle : Phase

        data class Running(
            val progress: Int,
        ) : Phase

        data object Done : Phase
    }

    private sealed interface Event {
        data object Start : Event

        data object Tick : Event

        data object Cancel : Event
    }

    private fun upload() = machineLogic<Phase, String, Event, String>(
        toEvent = { intent ->
            when (intent) {
                "start" -> Event.Start
                "cancel" -> Event.Cancel
                else -> null
            }
        },
    ) {
        state<Phase.Idle> {
            onEvent<Event.Start> { _, _ -> Phase.Running(progress = 0) }
        }
        state<Phase.Running> {
            // Keyed on the phase, not its data: otherwise every tick would restart the work.
            sideEffect(key = { Unit }) {
                while (true) {
                    delay(100)
                    send(Event.Tick)
                }
            }
            onEvent<Event.Tick> { running, _ ->
                if (running.progress == 2) {
                    trigger("done")
                    Phase.Done
                } else {
                    running.copy(progress = running.progress + 1)
                }
            }
            onEvent<Event.Cancel> { _, _ -> Phase.Idle }
        }
    }

    @Test
    fun runsThePhasesSideEffectUntilItLeavesThePhase() = runTest {
        val store = StoreHost<Phase, String, String>(Phase.Idle, backgroundScope, upload())
        val effects = mutableListOf<String>()
        backgroundScope.launch { store.effects.toList(effects) }

        store.postIntent("start")
        runCurrent()
        assertEquals(Phase.Running(progress = 0), store.state.value)

        advanceTimeBy(150)
        assertEquals(Phase.Running(progress = 1), store.state.value)

        advanceTimeBy(300)
        assertEquals(Phase.Done, store.state.value)
        assertEquals(listOf("done"), effects)
    }

    @Test
    fun anEventThePhaseDoesNotHandleIsIgnored() = runTest {
        val store = StoreHost<Phase, String, String>(Phase.Idle, backgroundScope, upload())

        store.postIntent("cancel")
        runCurrent()
        assertEquals(Phase.Idle, store.state.value)

        store.postIntent("start")
        runCurrent()
        store.postIntent("cancel")
        runCurrent()
        advanceTimeBy(1_000)
        assertEquals(Phase.Idle, store.state.value)
    }

    @Test
    fun runsAsASliceOfABiggerState() = runTest {
        data class Screen(
            val title: String = "draft",
            val upload: Phase = Phase.Idle,
        )

        val parent = object : Logic<Screen, String, String> {
            val upload = slice(upload(), get = Screen::upload, set = { copy(upload = it) })

            override fun StoreScope<Screen, String>.onStart() {
                upload.attach(this)
            }

            override fun StoreScope<Screen, String>.onIntent(intent: String) {
                upload.postIntent(intent)
            }
        }
        val store = StoreHost(Screen(), backgroundScope, parent)

        store.postIntent("start")
        runCurrent()
        advanceTimeBy(150)

        assertEquals(Screen(title = "draft", upload = Phase.Running(progress = 1)), store.state.value)
    }
}
