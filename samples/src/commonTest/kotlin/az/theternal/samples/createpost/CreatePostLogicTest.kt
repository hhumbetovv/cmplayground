package az.theternal.samples.createpost

import androidx.lifecycle.SavedStateHandle
import az.theternal.mvi.ContentState
import az.theternal.mvi.StoreHost
import az.theternal.mvi.test.storeTest
import az.theternal.samples.createpost.body.BodyIntent
import az.theternal.samples.createpost.poll.PollIntent
import az.theternal.samples.createpost.publish.PublishPhase
import az.theternal.samples.createpost.tags.TagsIntent
import az.theternal.samples.createpost.tags.TagsLogic
import az.theternal.samples.createpost.tags.TagsState
import az.theternal.samples.data.FakePostRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePostLogicTest {
    private val posts = FakePostRepository(latency = 1_000)

    private fun TestScope.store(): Pair<StoreHost<CreatePostState, CreatePostIntent, CreatePostEffect>, List<CreatePostEffect>> {
        val store = StoreHost(CreatePostState(), backgroundScope, CreatePostLogic(posts))
        val effects = mutableListOf<CreatePostEffect>()
        backgroundScope.launch { store.effects.toList(effects) }
        runCurrent()
        return store to effects
    }

    private fun TestScope.send(
        store: StoreHost<CreatePostState, CreatePostIntent, CreatePostEffect>,
        vararg intents: CreatePostIntent,
    ) {
        intents.forEach(store::postIntent)
        runCurrent()
    }

    @Test
    fun theDraftIsGatheredFromEverySliceAndChecked() {
        val state = CreatePostState().copy(
            body = CreatePostState().body.copy(text = "  hello  "),
            poll = CreatePostState().poll.copy(isOn = true, options = listOf("yes", "yes")),
        )
        assertEquals(DraftCheck.Invalid("Poll options must differ"), state.checkDraft())

        val ready = state.copy(poll = state.poll.copy(options = listOf("yes", "no"))).checkDraft()
        assertIs<DraftCheck.Ready>(ready)
        assertEquals("hello", ready.draft.text)
        assertEquals(listOf("yes", "no"), ready.draft.pollOptions)
    }

    @Test
    fun publishesThroughUploadingAndPublishing() = runTest {
        val (store, effects) = store()
        send(store, CreatePostIntent.Body(BodyIntent.Edit("hello")), CreatePostIntent.Publish)
        assertIs<PublishPhase.Uploading>(store.state.value.publish)

        advanceTimeBy(1_600)
        val uploading = assertIs<PublishPhase.Uploading>(store.state.value.publish)
        assertTrue(uploading.progress > 0f)

        advanceTimeBy(5_000)
        assertIs<PublishPhase.Published>(store.state.value.publish)
        assertIs<CreatePostEffect.Published>(effects.single())
    }

    @Test
    fun nothingIsEditableWhilePublishingAndCancelStopsIt() = runTest {
        val (store, _) = store()
        send(store, CreatePostIntent.Body(BodyIntent.Edit("hello")), CreatePostIntent.Publish)
        send(store, CreatePostIntent.Body(BodyIntent.Edit("changed")), CreatePostIntent.Poll(PollIntent.Toggle))
        assertEquals("hello", store.state.value.body.text)
        assertEquals(false, store.state.value.poll.isOn)

        send(store, CreatePostIntent.Cancel)
        assertEquals(PublishPhase.Idle, store.state.value.publish)
        advanceTimeBy(10_000)
        assertEquals(PublishPhase.Idle, store.state.value.publish)

        send(store, CreatePostIntent.Body(BodyIntent.Edit("changed")))
        assertEquals("changed", store.state.value.body.text)
    }

    @Test
    fun aFailureKeepsTheDraftAndRetryPublishesAgain() = runTest {
        val (store, effects) = store()
        send(store, CreatePostIntent.Body(BodyIntent.Edit("this will fail")), CreatePostIntent.Publish)
        advanceTimeBy(10_000)
        val failed = assertIs<PublishPhase.Failed>(store.state.value.publish)
        assertEquals("this will fail", failed.draft.text)

        // Editable again: fix the text and publish the new draft.
        send(store, CreatePostIntent.Body(BodyIntent.Edit("this will work")), CreatePostIntent.Publish)
        advanceTimeBy(10_000)
        assertIs<PublishPhase.Published>(store.state.value.publish)
        assertEquals(1, effects.size)
    }

    @Test
    fun anInvalidDraftIsAMessageNotAPhase() = runTest {
        val (store, effects) = store()
        send(store, CreatePostIntent.Publish)
        assertEquals(PublishPhase.Idle, store.state.value.publish)
        assertEquals(listOf<CreatePostEffect>(CreatePostEffect.Message("Write something first")), effects)
    }

    @Test
    fun theTagsSliceRunsAloneTooAndOnlyTheLastQueryIsSearched() = runTest {
        val store = StoreHost(TagsState(), backgroundScope, TagsLogic(posts))
        listOf("k", "ko", "kot").forEach { store.postIntent(TagsIntent.EditQuery(it)) }
        runCurrent()
        advanceTimeBy(2_000)
        assertEquals(ContentState.Success(listOf("kotlin")), store.state.value.suggestions)

        store.postIntent(TagsIntent.Add("kotlin"))
        runCurrent()
        assertEquals(listOf("kotlin"), store.state.value.selected)
        assertEquals("", store.state.value.query)
    }

    @Test
    fun theDraftSurvivesTheProcessBeingKilledButAPublishInFlightDoesNot() = runTest {
        val handle = SavedStateHandle()
        val store = StoreHost(CreatePostState(), backgroundScope, CreatePostLogic(posts), keeper = handle.draftKeeper())
        send(
            store,
            CreatePostIntent.Body(BodyIntent.Edit("hello")),
            CreatePostIntent.Poll(PollIntent.Toggle),
            CreatePostIntent.Poll(PollIntent.EditOption(0, "yes")),
            CreatePostIntent.Poll(PollIntent.EditOption(1, "no")),
            CreatePostIntent.Publish,
        )
        assertIs<PublishPhase.Uploading>(store.state.value.publish)

        val restored = StoreHost(CreatePostState(), backgroundScope, CreatePostLogic(posts), keeper = handle.afterProcessDeath().draftKeeper())
        assertEquals("hello", restored.state.value.body.text)
        assertEquals(listOf("yes", "no"), restored.state.value.poll.options)
        assertEquals(PublishPhase.Idle, restored.state.value.publish)
    }

    @Test
    fun publishingClearsTheDraftSoItIsNotOfferedAgain() = runTest {
        val handle = SavedStateHandle()
        storeTest(CreatePostState(), CreatePostLogic(posts), keeper = handle.draftKeeper()) {
            send(CreatePostIntent.Body(BodyIntent.Edit("hello")), CreatePostIntent.Publish)
            advance(10_000)
            skipAll()
            assertIs<PublishPhase.Published>(state.publish)
            assertEquals("", state.body.text)
        }
        assertEquals(CreatePostState().body, handle.afterProcessDeath().draftKeeper().let {
            StoreHost(CreatePostState(), backgroundScope, CreatePostLogic(posts), keeper = it).state.value.body
        })
    }

    /** What a killed process leaves: the handle's contents, in a new handle. */
    private fun SavedStateHandle.afterProcessDeath(): SavedStateHandle {
        return SavedStateHandle(keys().associateWith { key -> get<Any>(key) })
    }
}
