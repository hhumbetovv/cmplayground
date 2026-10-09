package az.theternal.mvi

import kotlinx.coroutines.CancellationException

/**
 * [runCatching] for suspending work: a failure becomes a [Result], but a cancellation still cancels — a
 * cancelled search must not come back as an error to show.
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}
