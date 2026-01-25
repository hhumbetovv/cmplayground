package az.theternal.cmplayground.core.navigation.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@Composable
inline fun <reified T : Any> ResultEffect(
    consumeOnce: Boolean = true,
    crossinline onResult: suspend (value: T) -> Unit,
) {
    val manager = LocalResultManager.current

    LaunchedEffect(T::class, manager) {
        val key = T::class.simpleName ?: return@LaunchedEffect
        snapshotFlow { manager.getResult<T>(key) }.collect { state ->
            val value = state?.value ?: return@collect
            onResult(value)
            if (consumeOnce) {
                manager.clearResult(key)
            }
        }
    }
}
