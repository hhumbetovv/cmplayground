package az.theternal.samples.createpost.tags

import az.theternal.mvi.ActionLogic
import az.theternal.mvi.ContentState
import az.theternal.mvi.Policy
import az.theternal.mvi.StoreScope
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.mvi.toContentState
import az.theternal.samples.data.PostRepository
import kotlinx.coroutines.delay

data class TagsState(
    val query: String = "",
    val suggestions: ContentState<List<String>> = ContentState.Idle,
    val selected: List<String> = emptyList(),
) {
    companion object {
        const val MAX_TAGS = 3
    }
}

sealed interface TagsIntent {
    data class EditQuery(
        val text: String,
    ) : TagsIntent

    data class Add(
        val tag: String,
    ) : TagsIntent

    data class Remove(
        val tag: String,
    ) : TagsIntent
}

sealed interface TagsEffect {
    data object LimitReached : TagsEffect
}

/**
 * A level 2 slice: suggestions are searched as the user types, under [Policy.Latest]. It knows nothing of
 * posts — it would host as well in a profile's interests screen, or as a tag-picker component of its own.
 */
class TagsLogic(
    private val posts: PostRepository,
) : ActionLogic<TagsState, TagsIntent, String, TagsEffect>() {
    override fun StoreScope<TagsState, TagsEffect>.toAction(intent: TagsIntent): String? {
        return when (intent) {
            is TagsIntent.EditQuery -> {
                setState { copy(query = intent.text) }
                intent.text.trim()
            }
            is TagsIntent.Add -> {
                when {
                    intent.tag in state.selected -> Unit
                    state.selected.size >= TagsState.MAX_TAGS -> emit(TagsEffect.LimitReached)
                    else -> setState { copy(selected = selected + intent.tag, query = "", suggestions = ContentState.Idle) }
                }
                // An empty query: cancels a search still running for the text just cleared.
                if (state.query.isEmpty()) "" else null
            }
            is TagsIntent.Remove -> {
                setState { copy(selected = selected - intent.tag) }
                null
            }
        }
    }

    override fun policy(action: String) = Policy.Latest

    override suspend fun StoreScope<TagsState, TagsEffect>.process(action: String) {
        if (action.isEmpty()) {
            setState { copy(suggestions = ContentState.Idle) }
            return
        }
        delay(DEBOUNCE_MILLIS)
        setState { copy(suggestions = ContentState.Loading()) }
        val result = safeCall { posts.searchTags(action) }
        setState { copy(suggestions = result.map { tags -> tags - selected.toSet() }.toContentState()) }
    }

    companion object {
        const val DEBOUNCE_MILLIS = 250L
    }
}
