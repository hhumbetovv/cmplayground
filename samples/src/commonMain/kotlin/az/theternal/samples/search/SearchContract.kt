package az.theternal.samples.search

import az.theternal.mvi.ContentState
import az.theternal.mvi.Partial
import az.theternal.mvi.map
import az.theternal.mvi.toContentState
import az.theternal.samples.data.User

data class SearchState(
    val query: String = "",
    val results: ContentState<List<User>> = ContentState.Idle,
)

/** What the user did. */
sealed interface SearchIntent {
    data class EditQuery(
        val text: String,
    ) : SearchIntent

    data object Retry : SearchIntent

    data class ToggleFollow(
        val userId: String,
    ) : SearchIntent
}

/** What to do about it — every parameter gathered, so processing reads nothing from the state. */
sealed interface SearchAction {
    data class Search(
        val query: String,
        val debounce: Boolean,
    ) : SearchAction

    /** The wish, not a toggle: "follow" or "unfollow", decided when the user tapped. */
    data class SetFollowing(
        val user: User,
        val following: Boolean,
    ) : SearchAction
}

/** Every change the logic makes, named, its reduction beside it. */
sealed interface SearchPartial : Partial<SearchState> {
    data class Query(
        val text: String,
    ) : SearchPartial {
        override fun reduce(state: SearchState) = state.copy(query = text)
    }

    data object Cleared : SearchPartial {
        override fun reduce(state: SearchState) = state.copy(results = ContentState.Idle)
    }

    data object Loading : SearchPartial {
        override fun reduce(state: SearchState) = state.copy(results = ContentState.Loading(previous = null))
    }

    data class Results(
        val result: Result<List<User>>,
    ) : SearchPartial {
        override fun reduce(state: SearchState) = state.copy(results = result.toContentState())
    }

    data class Following(
        val userId: String,
        val following: Boolean,
    ) : SearchPartial {
        override fun reduce(state: SearchState) = state.copy(
            results = state.results.map { users ->
                users.map { user -> if (user.id == userId) user.copy(isFollowing = following) else user }
            },
        )
    }
}

sealed interface SearchEffect {
    data class FollowFailed(
        val name: String,
    ) : SearchEffect
}
