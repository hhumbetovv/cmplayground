package az.theternal.core.framework.delegates.effect_producer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class EffectDelegate<Effect : ViewEffect> internal constructor(
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val _effects = Channel<Effect>()
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    fun sendEffect(effect: Effect) {
        scope.launch {
            _effects.send(effect)
        }
    }

    override fun close() {
        _effects.close()
    }
}
