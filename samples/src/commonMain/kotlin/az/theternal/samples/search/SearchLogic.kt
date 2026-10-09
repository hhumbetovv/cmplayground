package az.theternal.samples.search

import az.theternal.mvi.ActionLogic
import az.theternal.mvi.Policy
import az.theternal.mvi.StoreScope
import az.theternal.mvi.StoreViewModel
import az.theternal.mvi.dataOrNull
import az.theternal.mvi.safeCall
import az.theternal.samples.data.UserRepository
import kotlinx.coroutines.delay

/**
 * Level 2. Two kinds of work overlap here, and the policies say how, by name:
 * - a search is [Policy.Latest]: each keystroke cancels the one before, so a slow answer to "ad" never
 *   overwrites the answer to "ada", and the `delay` at its start is the debounce;
 * - following is [Policy.Latest] per user: tapping follow, unfollow, follow sends the last wish only — and
 *   the wish is gathered when tapped (`following = !isFollowing`), which is why the backend call is a
 *   "set", never a "toggle". Different users do not cancel each other.
 *
 * The level 1 way to the same behaviour is a `Job?` per kind of work, a map of jobs per user, and checks
 * that a result still matches the query. Those are what this replaces.
 */
class SearchLogic(
    private val users: UserRepository,
) : ActionLogic<SearchState, SearchIntent, SearchAction, SearchEffect>() {
    override fun StoreScope<SearchState, SearchEffect>.toAction(intent: SearchIntent): SearchAction? {
        return when (intent) {
            is SearchIntent.EditQuery -> {
                reduce(SearchPartial.Query(intent.text))
                SearchAction.Search(query = intent.text.trim(), debounce = true)
            }
            SearchIntent.Retry -> SearchAction.Search(query = state.query.trim(), debounce = false)
            is SearchIntent.ToggleFollow -> {
                val user = state.results.dataOrNull()?.firstOrNull { it.id == intent.userId } ?: return null
                SearchAction.SetFollowing(user = user, following = !user.isFollowing)
            }
        }
    }

    override fun policy(action: SearchAction) = Policy.Latest

    override fun key(action: SearchAction): Any {
        return when (action) {
            is SearchAction.Search -> "search"
            is SearchAction.SetFollowing -> "follow:${action.user.id}"
        }
    }

    override suspend fun StoreScope<SearchState, SearchEffect>.process(action: SearchAction) {
        when (action) {
            is SearchAction.Search -> search(action)
            is SearchAction.SetFollowing -> setFollowing(action)
        }
    }

    private suspend fun StoreScope<SearchState, SearchEffect>.search(action: SearchAction.Search) {
        if (action.query.isEmpty()) {
            reduce(SearchPartial.Cleared)
            return
        }
        if (action.debounce) delay(DEBOUNCE_MILLIS)
        reduce(SearchPartial.Loading)
        reduce(SearchPartial.Results(safeCall { users.search(action.query) }))
    }

    private suspend fun StoreScope<SearchState, SearchEffect>.setFollowing(action: SearchAction.SetFollowing) {
        // Optimistic: shown at once, rolled back if the backend says no.
        reduce(SearchPartial.Following(action.user.id, action.following))
        safeCall { users.setFollowing(action.user.id, action.following) }
            .onFailure {
                reduce(SearchPartial.Following(action.user.id, action.user.isFollowing))
                emit(SearchEffect.FollowFailed(action.user.name))
            }
    }

    companion object {
        const val DEBOUNCE_MILLIS = 300L
    }
}

class SearchViewModel(
    users: UserRepository,
) : StoreViewModel<SearchState, SearchIntent, SearchEffect>(SearchState(), SearchLogic(users))
