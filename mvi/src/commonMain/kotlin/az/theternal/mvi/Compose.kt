package az.theternal.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
 * A ViewModel store per list item, for component ViewModels. An item's ViewModels live while the item is
 * on screen, and after it has scrolled away for as long as their work runs: a like that is being sent is
 * not cancelled by a scroll, and a ViewModel that has nothing running is let go. The ones kept are the
 * items on screen and those with work in flight, so the number does not grow with the list.
 *
 * "Work" is what [Store.isBusy] says: launched work, actions, the work of a phase. A ViewModel's state is
 * gone with it, so what a finished piece of work produced belongs in the list's data source (a repository
 * or a cache) before it ends; the item reads it from there when it comes back.
 *
 * An item that leaves the list is cleared at once, working or not.
 */
class ItemViewModels : ViewModel() {
    private class Entry(
        val owner: ViewModelStoreOwner,
    ) {
        var onScreen = 0
        var release: Job? = null
    }

    private val entries = mutableMapOf<Any, Entry>()

    fun owner(key: Any): ViewModelStoreOwner = entry(key).owner

    private fun entry(key: Any): Entry {
        return entries.getOrPut(key) {
            Entry(
                object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                },
            )
        }
    }

    /** The item is composed: nothing releases it now. */
    internal fun attach(key: Any) {
        val entry = entry(key)
        entry.onScreen++
        entry.release?.cancel()
        entry.release = null
    }

    /** The item left the composition: it is released as soon as none of its ViewModels is busy. */
    internal fun detach(key: Any) {
        val entry = entries[key] ?: return
        entry.onScreen--
        if (entry.onScreen > 0) return
        entry.release = viewModelScope.launch {
            val busy = busyOf(entry.owner)
            if (busy.isNotEmpty()) combine(busy) { states -> states.any { it } }.first { isBusy -> !isBusy }
            if (entry.onScreen == 0) clear(key)
        }
    }

    private fun busyOf(owner: ViewModelStoreOwner): List<StateFlow<Boolean>> {
        val store = owner.viewModelStore
        return store.keys().mapNotNull { name -> (store[name] as? StoreOwner<*, *, *>)?.store?.isBusy }
    }

    /** Clears the ViewModels of the items that are no longer in [keys], busy or not. */
    fun retain(keys: Set<Any>) {
        (entries.keys - keys).forEach(::clear)
    }

    private fun clear(key: Any) {
        val entry = entries.remove(key) ?: return
        entry.release?.cancel()
        entry.owner.viewModelStore.clear()
    }

    override fun onCleared() {
        entries.keys.toList().forEach(::clear)
    }
}

/** The [ItemViewModels] of a list whose items are [keys], kept in the screen's ViewModel store. */
@Composable
fun rememberItemViewModels(
    keys: List<Any>,
    name: String = "items",
): ItemViewModels {
    val items = viewModel(key = "ItemViewModels:$name") { ItemViewModels() }
    val keySet = remember(keys) { keys.toSet() }
    SideEffect { items.retain(keySet) }
    return items
}

/** [content] with the item's own ViewModel store: `viewModel { … }` inside it is the item's. */
@Composable
fun ItemScope(
    items: ItemViewModels,
    key: Any,
    content: @Composable () -> Unit,
) {
    DisposableEffect(items, key) {
        items.attach(key)
        onDispose { items.detach(key) }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides items.owner(key), content = content)
}
