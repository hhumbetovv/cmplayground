package az.theternal.samples.search

import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.store
import az.theternal.samples.data.UserRepository

class SearchViewModel(
    users: UserRepository,
) : ViewModel(),
    StoreOwner<SearchState, SearchIntent, SearchEffect> {
    override val store = store(SearchState(), SearchLogic(users))
}
