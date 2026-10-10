package az.theternal.samples.createpost.tags

import az.theternal.mvi.ContentState

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
