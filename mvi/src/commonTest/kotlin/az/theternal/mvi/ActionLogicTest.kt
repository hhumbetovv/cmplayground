package az.theternal.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ActionLogicTest {
    /** Each intent runs one action that takes [duration] and then appends its value to the log. */
    private class Logging(
        private val policy: Policy,
    ) : ActionLogic<List<String>, Pair<String, Long>, Pair<String, Long>, Nothing>() {
        override fun StoreScope<List<String>, Nothing>.toAction(intent: Pair<String, Long>) = intent

        override fun policy(action: Pair<String, Long>) = policy

        override fun key(action: Pair<String, Long>) = "one key"

        override suspend fun StoreScope<List<String>, Nothing>.process(action: Pair<String, Long>) {
            delay(action.second)
            setState { this + action.first }
        }
    }

    private fun runPolicy(policy: Policy): List<String> {
        var log = emptyList<String>()
        runTest {
            val store = StoreHost(emptyList(), backgroundScope, Logging(policy))
            store.postIntent("slow" to 300L)
            store.postIntent("fast" to 100L)
            advanceTimeBy(1_000)
            log = store.state.value
        }
        return log
    }

    @Test
    fun parallelRunsBothAndTheFasterFinishesFirst() {
        assertEquals(listOf("fast", "slow"), runPolicy(Policy.Parallel))
    }

    @Test
    fun latestCancelsTheRunningOneSoItsStaleResultNeverLands() {
        assertEquals(listOf("fast"), runPolicy(Policy.Latest))
    }

    @Test
    fun dropIgnoresTheNewOneWhileOneRuns() {
        assertEquals(listOf("slow"), runPolicy(Policy.Drop))
    }

    @Test
    fun queueRunsThemOneAfterAnotherInOrder() {
        assertEquals(listOf("slow", "fast"), runPolicy(Policy.Queue))
    }

    @Test
    fun dropAcceptsAgainOnceTheRunningOneIsDone() = runTest {
        val store = StoreHost(emptyList(), backgroundScope, Logging(Policy.Drop))
        store.postIntent("first" to 100L)
        advanceTimeBy(1_000)
        store.postIntent("second" to 100L)
        advanceTimeBy(1_000)
        assertEquals(listOf("first", "second"), store.state.value)
    }
}
