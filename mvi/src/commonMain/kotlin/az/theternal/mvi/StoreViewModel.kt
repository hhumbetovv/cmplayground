package az.theternal.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A ViewModel that only hosts a [Logic]. All of a screen's (or a component's) behaviour is in the logic,
 * which is a plain class: it is tested without a ViewModel, and it can be hosted as a [Slice] or a [Slot]
 * instead.
 *
 * ```kotlin
 * class SearchViewModel(users: UserRepository) :
 *     StoreViewModel<SearchState, SearchIntent, SearchEffect>(SearchState(), SearchLogic(users))
 * ```
 */
open class StoreViewModel<S, I, E>(
    initialState: S,
    logic: Logic<S, I, E>,
    settings: StoreSettings = StoreSettings(),
    keeper: StateKeeper<S>? = null,
) : ViewModel(),
    Store<S, I, E> {
    private val host = StoreHost(
        initialState = initialState,
        scope = viewModelScope,
        logic = logic,
        settings = settings,
        keeper = keeper,
    )

    override val state: StateFlow<S> get() = host.state
    override val effects: Flow<E> get() = host.effects

    override fun postIntent(intent: I) {
        host.postIntent(intent)
    }
}
