package az.theternal.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * Something that owns a [Store] — usually a ViewModel. Named after Android's owners (`LifecycleOwner`,
 * `ViewModelStoreOwner`): it hands out the store, it is not the store. Being an interface, it leaves the
 * superclass free, so an app's own base ViewModel works as it is.
 *
 * The ViewModel holds no behaviour. All of a screen's (or a component's) behaviour is in the [Logic], a
 * plain class: it is tested without a ViewModel, and it can be hosted as a [Slice] or a [Slot] instead.
 *
 * ```kotlin
 * class SearchViewModel(users: UserRepository) :
 *     ViewModel(),
 *     StoreOwner<SearchState, SearchIntent, SearchEffect> {
 *     override val store = store(SearchState(), SearchLogic(users))
 * }
 * ```
 */
interface StoreOwner<S, I, E> {
    val store: Store<S, I, E>
}

/** Hosts [logic] for as long as this ViewModel lives: its work runs in `viewModelScope`. */
fun <S, I, E> ViewModel.store(
    initialState: S,
    logic: Logic<S, I, E>,
    settings: StoreSettings = StoreSettings(),
    keeper: StateKeeper<S>? = null,
): Store<S, I, E> {
    return StoreHost(
        initialState = initialState,
        scope = viewModelScope,
        logic = logic,
        settings = settings,
        keeper = keeper,
    )
}
