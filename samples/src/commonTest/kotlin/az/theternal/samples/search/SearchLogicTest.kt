package az.theternal.samples.search

import az.theternal.mvi.ContentState
import az.theternal.mvi.StoreHost
import az.theternal.mvi.dataOrNull
import az.theternal.mvi.test.storeTest
import az.theternal.samples.data.FakeUserRepository
import az.theternal.samples.data.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SearchLogicTest {
    @Test
    fun typingFastSearchesOnlyTheLastQuery() = runTest {
        val store = StoreHost(SearchState(), backgroundScope, SearchLogic(FakeUserRepository(latency = 1_000)))
        listOf("a", "ad", "ada").forEach { store.postIntent(SearchIntent.EditQuery(it)) }
        runCurrent()
        advanceTimeBy(5_000)
        assertEquals(listOf("Ada Lovelace"), store.state.value.results.dataOrNull()?.map { it.name })
    }

    @Test
    fun aFailedFollowRollsBack() = runTest {
        val store = StoreHost(SearchState(), backgroundScope, SearchLogic(FakeUserRepository(latency = 1_000)))
        store.postIntent(SearchIntent.EditQuery("ken"))
        runCurrent()
        advanceTimeBy(5_000)
        store.postIntent(SearchIntent.ToggleFollow(FakeUserRepository.FAILING_USER))
        runCurrent()
        assertEquals(true, store.state.value.results.dataOrNull()?.single()?.isFollowing)
        advanceTimeBy(5_000)
        assertEquals(false, store.state.value.results.dataOrNull()?.single()?.isFollowing)
    }

    @Test
    fun clearingTheQueryCancelsTheSearchInFlight() = runTest {
        val store = StoreHost(SearchState(), backgroundScope, SearchLogic(FakeUserRepository(latency = 1_000)))
        store.postIntent(SearchIntent.EditQuery("ada"))
        runCurrent()
        advanceTimeBy(500)
        assertIs<ContentState.Loading<*>>(store.state.value.results)
        store.postIntent(SearchIntent.EditQuery(""))
        runCurrent()
        advanceTimeBy(5_000)
        assertEquals(ContentState.Idle, store.state.value.results)
    }

    @Test
    fun aSearchReadsLikeItsReducer() = runTest {
        storeTest(SearchState(), SearchLogic(FakeUserRepository(latency = 1_000))) {
            send(SearchIntent.EditQuery("a"), SearchIntent.EditQuery("ada"))
            expectState { copy(query = "a") }
            expectState { copy(query = "ada") }

            // Only the last query survives the debounce.
            advance(SearchLogic.DEBOUNCE_MILLIS)
            expectState { copy(results = ContentState.Loading()) }
            advance(1_000)
            expectState { copy(results = ContentState.Success(listOf(User("u0", "Ada Lovelace", isFollowing = false)))) }
        }
    }

    @Test
    fun aRefusedFollowRollsBackAndSaysSo() = runTest {
        val ken = User(FakeUserRepository.FAILING_USER, "Ken Thompson", isFollowing = false)
        val found = SearchState(query = "ken", results = ContentState.Success(listOf(ken)))
        storeTest(found, SearchLogic(FakeUserRepository(latency = 1_000))) {
            send(SearchIntent.ToggleFollow(ken.id))
            expectState { copy(results = ContentState.Success(listOf(ken.copy(isFollowing = true)))) }
            advance(1_000)
            expectState { copy(results = ContentState.Success(listOf(ken))) }
            expectEffect(SearchEffect.FollowFailed(ken.name))
        }
    }
}
