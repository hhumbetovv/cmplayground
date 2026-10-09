package az.theternal.samples.feed

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreHost
import az.theternal.mvi.StoreScope
import az.theternal.mvi.slice
import az.theternal.samples.data.FakePostRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LikeLogicTest {
    private val posts = FakePostRepository(latency = 1_000)

    @Test
    fun manyTapsSendTheLastWishOnly() = runTest {
        val store = StoreHost(LikeState(isLiked = false, count = 12), backgroundScope, LikeLogic("p1", posts))
        repeat(5) { store.postIntent(LikeIntent.Toggle) }
        runCurrent()
        assertEquals(LikeState(isLiked = true, count = 13), store.state.value)

        advanceTimeBy(2_000)
        assertEquals(LikeState(isLiked = true, count = 13), store.state.value)
        assertEquals(true, posts.feed().first { it.id == "p1" }.isLiked)
    }

    @Test
    fun aFailureRollsBack() = runTest {
        val store = StoreHost(LikeState(isLiked = false, count = 7), backgroundScope, LikeLogic(FakePostRepository.FAILING_POST, posts))
        val effects = mutableListOf<LikeEffect>()
        backgroundScope.launch { store.effects.toList(effects) }
        store.postIntent(LikeIntent.Toggle)
        runCurrent()
        advanceTimeBy(2_000)
        assertEquals(LikeState(isLiked = false, count = 7), store.state.value)
        assertEquals(listOf<LikeEffect>(LikeEffect.Failed), effects)
    }

    @Test
    fun theSameLogicIsASliceOfAPostDetailsScreen() = runTest {
        data class Details(
            val text: String,
            val like: LikeState,
        )

        val details = object : Logic<Details, LikeIntent, LikeEffect> {
            val like = slice(LikeLogic("p1", posts), get = Details::like, set = { copy(like = it) })

            override fun StoreScope<Details, LikeEffect>.onStart() = like.attach(this)

            override fun StoreScope<Details, LikeEffect>.onIntent(intent: LikeIntent) = like.postIntent(intent)
        }
        val store = StoreHost(Details("post", LikeState(isLiked = false, count = 12)), backgroundScope, details)
        store.postIntent(LikeIntent.Toggle)
        runCurrent()
        advanceTimeBy(2_000)
        assertEquals(Details("post", LikeState(isLiked = true, count = 13)), store.state.value)
    }
}
