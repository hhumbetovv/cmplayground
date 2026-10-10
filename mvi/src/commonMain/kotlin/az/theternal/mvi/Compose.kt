package az.theternal.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The store's state for a screen, collected while the screen is at least started. */
@Composable
fun <S> Store<S, *, *>.collectAsState(): State<S> {
    return state.collectAsStateWithLifecycle()
}

/**
 * Hands each effect to [onEffect], once. Collected on [dispatcher], `Dispatchers.Main.immediate` by
 * default, so an effect taken from the store is handled before the screen can go away — not dropped
 * between the two. A target that has no Main dispatcher (desktop without `kotlinx-coroutines-swing`) passes
 * its own.
 */
@Composable
fun <E> Store<*, *, E>.CollectEffects(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    onEffect: suspend (E) -> Unit,
) {
    val currentOnEffect = rememberUpdatedState(onEffect)
    LaunchedEffect(this) {
        withContext(dispatcher) {
            effects.collect { effect -> currentOnEffect.value(effect) }
        }
    }
}

/**
 * One component's state, mapped from the screen's: the component recomposes only when its own part
 * changes. ArenaGo's mapper style — the screen's state stays the logic's, every component gets the shape
 * it draws, and a component that later gets its own ViewModel already has its own state.
 */
@Composable
inline fun <T, R> State<T>.select(crossinline transform: (T) -> R): State<R> {
    return remember(this) { derivedStateOf { transform(value) } }
}

/** [select] whose mapping also reads [key] — it is rebuilt when the key changes. */
@Composable
inline fun <T, R> State<T>.select(
    key: Any?,
    crossinline transform: (T) -> R,
): State<R> {
    return remember(this, key) { derivedStateOf { transform(value) } }
}

/**
 * A ViewModel store per list item — Decompose's `childItems`. An item's component ViewModels live as
 * long as the item is in the list, not as long as it is on screen: scrolling an item away and back keeps
 * its state, and an item removed from the list has its ViewModels cleared. While an item is scrolled
 * away nothing collects its state, so its `whileSubscribed` work pauses.
 *
 * Without this, a component ViewModel per item either lives in the screen's store until the screen
 * closes (a long feed keeps every one), or in a store remembered by the item (its state is lost when the
 * item scrolls away).
 */
class ItemStores : ViewModel() {
    private val owners = mutableMapOf<Any, ViewModelStoreOwner>()

    fun owner(key: Any): ViewModelStoreOwner {
        return owners.getOrPut(key) {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    }

    /** Clears the stores of the items that are no longer in [keys]. */
    fun retain(keys: Set<Any>) {
        val gone = owners.keys - keys
        gone.forEach { key -> owners.remove(key)?.viewModelStore?.clear() }
    }

    override fun onCleared() {
        owners.values.forEach { owner -> owner.viewModelStore.clear() }
        owners.clear()
    }
}

/** The [ItemStores] of a list whose items are [keys], kept in the screen's ViewModel store. */
@Composable
fun rememberItemStores(
    keys: List<Any>,
    name: String = "items",
): ItemStores {
    val stores = viewModel(key = "ItemStores:$name") { ItemStores() }
    SideEffect { stores.retain(keys.toSet()) }
    return stores
}

/** [content] with the item's own ViewModel store: `viewModel { … }` inside it is the item's. */
@Composable
fun ItemScope(
    stores: ItemStores,
    key: Any,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalViewModelStoreOwner provides stores.owner(key), content = content)
}
