package az.theternal.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun aFailureIsReportedAndTheStoreKeepsWorking() = runTest {
        val errors = mutableListOf<String?>()
        val store = StoreHost(0, backgroundScope, Failing(), onError = { errors += it.message })

        store.postIntent("throw")
        store.postIntent("fail later")
        advanceTimeBy(1_000)
        store.postIntent("count")
        advanceTimeBy(1_000)

        assertEquals<List<String?>>(listOf("in the intent", "in launched work"), errors)
        assertEquals<Int>(1, store.state.value)
    }
}
