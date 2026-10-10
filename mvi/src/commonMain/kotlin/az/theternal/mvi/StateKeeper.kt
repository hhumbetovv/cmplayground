package az.theternal.mvi

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * What of a store's state survives the process being killed — Decompose's `StateKeeper`, on Jetpack's
 * `SavedStateHandle`, with kotlinx-serialization so that it works on every platform.
 *
 * Only what the user would be upset to lose is kept: a draft, a selection, a step. Loaded data is
 * fetched again, and a phase such as "publishing" is never restored — the work behind it is gone. That
 * is why a keeper saves a part ([save]) and merges it back into the initial state ([restore]), instead
 * of saving the whole state.
 *
 * ```kotlin
 * override val store = store(CreatePostState(), logic, keeper = handle.keeper("draft", SavedDraft.serializer(), …))
 * ```
 */
class StateKeeper<S> internal constructor(
    internal val restore: (initial: S) -> S,
    internal val save: suspend (states: Flow<S>) -> Unit,
)

fun <S, T> SavedStateHandle.keeper(
    key: String,
    serializer: KSerializer<T>,
    save: (S) -> T,
    restore: S.(T) -> S,
    saveIntervalMillis: Long = DEFAULT_SAVE_INTERVAL_MILLIS,
): StateKeeper<S> {
    val handle = this
    return StateKeeper(
        restore = { initial ->
            val saved = handle.get<String>(key)
            // A saved value that no longer decodes — the app was updated meanwhile — is dropped.
            val part = saved?.let {
                try {
                    keeperJson.decodeFromString(serializer, it)
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            if (part == null) initial else initial.restore(part)
        },
        save = { states ->
            // The first change is written at once; the ones that follow within the interval are merged into
            // one write of the last value, so typing is not encoded on every key.
            states.map(save).distinctUntilChanged().conflate().collect { part ->
                handle[key] = keeperJson.encodeToString(serializer, part)
                delay(saveIntervalMillis)
            }
        },
    )
}

private val keeperJson = Json { ignoreUnknownKeys = true }

const val DEFAULT_SAVE_INTERVAL_MILLIS = 300L
