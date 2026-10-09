package az.theternal.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StoreHostTest {
    private class Failing : Logic<Int, String, Nothing> {
        override fun StoreScope<Int, Nothing>.onIntent(intent: String) {
            when (intent) {
                "throw" -> error("in the intent")
                "fail later" -> launch { error("in launched work") }
                else -> setState { this + 1 }
            }
        }
    }

    private class Echo(
        private val starts: MutableList<Unit> = mutableListOf(),
    ) : Logic<Int, Int, Int> {
        override fun StoreScope<Int, Int>.onStart() {
            starts += Unit
        }

        override fun StoreScope<Int, Int>.onIntent(intent: Int) {
            reduce(Add(intent))
            emit(intent)
        }
    }

    private data class Add(
        val amount: Int,
    ) : Partial<Int> {
        override fun reduce(state: Int) = state + amount
    }

    @Test
    fun aFailureIsReportedAndTheStoreKeepsWorking() = runTest {
        val errors = mutableListOf<String?>()
        val store = StoreHost(0, backgroundScope, Failing(), StoreSettings(onError = { errors += it.message }))

        store.postIntent("throw")
        store.postIntent("fail later")
        advanceTimeBy(1_000)
        store.postIntent("count")
        advanceTimeBy(1_000)

        assertEquals<List<String?>>(listOf("in the intent", "in launched work"), errors)
        assertEquals<Int>(1, store.state.value)
    }

    @Test
    fun effectsEmittedWithNoCollectorWaitForOne() = runTest {
        val store = StoreHost(0, backgroundScope, Echo())
        store.postIntent(1)
        store.postIntent(2)
        val effects = mutableListOf<Int>()
        backgroundScope.launch { store.effects.toList(effects) }
        runCurrent()
        assertEquals(listOf(1, 2), effects)
    }

    @Test
    fun aSecondEffectCollectorFailsAtOnce() = runTest {
        val store = StoreHost(0, backgroundScope, Echo())
        backgroundScope.launch { store.effects.collect { } }
        runCurrent()
        assertFailsWith<IllegalStateException> { store.effects.first() }
    }

    @Test
    fun aLazyStoreStartsOnFirstUseAndAManualOneOnlyWhenAsked() = runTest {
        val lazyStarts = mutableListOf<Unit>()
        val lazy = StoreHost(0, backgroundScope, Echo(lazyStarts))
        assertEquals(0, lazy.state.value)
        assertEquals(0, lazyStarts.size)
        backgroundScope.launch { lazy.state.collect { } }
        runCurrent()
        assertEquals(1, lazyStarts.size)

        val manualStarts = mutableListOf<Unit>()
        val manual = StoreHost(0, backgroundScope, Echo(manualStarts), StoreSettings(start = Start.Manually))
        manual.postIntent(1)
        assertEquals(0, manualStarts.size)
        manual.start()
        manual.start()
        assertEquals(1, manualStarts.size)
    }

    @Test
    fun theObserverSeesIntentsNamedPartialsAndEffectsInOrder() = runTest {
        val log = mutableListOf<String>()
        val observer = object : StoreObserver {
            override fun onIntent(intent: Any?) {
                log += "intent $intent"
            }

            override fun onState(
                partial: Partial<*>,
                previous: Any?,
                state: Any?,
            ) {
                log += "$partial: $previous -> $state"
            }

            override fun onEffect(effect: Any?) {
                log += "effect $effect"
            }
        }
        val store = StoreHost(0, backgroundScope, Echo(), StoreSettings(observer = observer))
        store.postIntent(2)
        store.postIntent(0)
        assertEquals(
            listOf("intent 2", "Add(amount=2): 0 -> 2", "effect 2", "intent 0", "effect 0"),
            log,
        )
    }

    @Test
    fun theMappedStoreShowsOnlyTheScreensState() = runTest {
        val store = StoreHost(0, backgroundScope, Echo()).mapState { count -> "count: $count" }
        val seen = mutableListOf<String>()
        backgroundScope.launch { store.state.toList(seen) }
        runCurrent()
        store.postIntent(1)
        runCurrent()
        assertEquals("count: 1", store.state.value)
        assertEquals(listOf("count: 0", "count: 1"), seen)
    }

    @Test
    fun whileSubscribedWorkRunsOnlyWhileTheStateIsCollected() = runTest {
        val source = MutableSharedFlow<Int>()
        val logic = object : Logic<Int, Unit, Nothing> {
            override fun StoreScope<Int, Nothing>.onStart() {
                observeWhileSubscribed(source, stopTimeoutMillis = 1_000) { value -> setState { value } }
            }

            override fun StoreScope<Int, Nothing>.onIntent(intent: Unit) = Unit
        }
        val store = StoreHost(0, backgroundScope, logic, StoreSettings(start = Start.Eagerly))
        runCurrent()
        assertEquals(0, source.subscriptionCount.value)

        val screen = backgroundScope.launch { store.state.collect { } }
        runCurrent()
        source.emit(1)
        assertEquals(1, store.state.value)

        screen.cancel()
        runCurrent()
        assertTrue(source.subscriptionCount.value > 0, "still running within the stop timeout")
        advanceTimeBy(1_500)
        assertEquals(0, source.subscriptionCount.value)
    }
}
