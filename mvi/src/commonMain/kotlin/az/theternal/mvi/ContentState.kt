package az.theternal.mvi

import androidx.compose.runtime.Immutable

/**
 * Remote data on screen: not asked for yet, loading (with data to keep showing meanwhile), there, or
 * failed. arch-kit's, with a [Throwable] for its `Failure`. Keep one per independently loaded part of a
 * screen — a profile's header and its posts load, fail and retry on their own.
 */
@Immutable
sealed interface ContentState<out T> {
    data object Idle : ContentState<Nothing>

    data class Loading<T>(
        val previous: T? = null,
    ) : ContentState<T>

    data class Success<T>(
        val data: T,
    ) : ContentState<T>

    data class Error(
        val error: Throwable,
    ) : ContentState<Nothing>
}

/** The data of a success, or what a loading state keeps showing. */
fun <T> ContentState<T>.dataOrNull(): T? {
    return when (this) {
        is ContentState.Loading -> previous
        is ContentState.Success -> data
        ContentState.Idle, is ContentState.Error -> null
    }
}

val ContentState<*>.isLoading: Boolean get() = this is ContentState.Loading

inline fun <T, R> ContentState<T>.map(transform: (T) -> R): ContentState<R> {
    return when (this) {
        ContentState.Idle -> ContentState.Idle
        is ContentState.Loading -> ContentState.Loading(previous?.let(transform))
        is ContentState.Success -> ContentState.Success(transform(data))
        is ContentState.Error -> this
    }
}

/** Loading again, keeping what is shown — a refresh does not blank the screen. */
fun <T> ContentState<T>.reload(): ContentState<T> {
    return ContentState.Loading(previous = dataOrNull())
}

fun <T> Result<T>.toContentState(): ContentState<T> {
    return fold(
        onSuccess = { ContentState.Success(it) },
        onFailure = { ContentState.Error(it) },
    )
}
