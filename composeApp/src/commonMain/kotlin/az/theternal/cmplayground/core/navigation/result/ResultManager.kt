package az.theternal.cmplayground.core.navigation.result

import androidx.compose.runtime.mutableStateMapOf
import kotlin.reflect.KClass

class ResultManager {
    private val _results = mutableStateMapOf<String, ResultState<*>>()

    fun <T : Any> setResult(key: KClass<T>, value: T) {
        val resolvedKey = key.simpleName ?: return

        _results[resolvedKey] = ResultState(
            value = value,
            key = resolvedKey,
        )
    }

    inline fun <reified T : Any> setResult(value: T) {
        setResult(T::class, value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getResult(key: String): ResultState<T>? {
        return _results[key] as? ResultState<T>
    }

    fun clearResult(key: String) {
        _results.remove(key)
    }

    fun clearAll() {
        _results.clear()
    }
}
