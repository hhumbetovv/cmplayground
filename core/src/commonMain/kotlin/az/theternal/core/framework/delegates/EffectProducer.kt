package az.theternal.core.framework.delegates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


interface ViewEffect

interface EffectProducer<Effect : ViewEffect>{
    val effectDelegate: EffectDelegate<Effect>
}

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

context(viewModel: ViewModel, _: EffectProducer<Effect>)
fun <Effect : ViewEffect> EffectDelegate(): EffectDelegate<Effect> {
    return EffectDelegate<Effect>(
        scope = viewModel.viewModelScope
    ).also { delegate ->
        viewModel.addCloseable(delegate)
    }
}

context(viewModel: ViewModel, producer: EffectProducer<Effect>)
fun <Effect : ViewEffect> sendEffect(
    effect: Effect
) {
    viewModel.viewModelScope.launch {
        producer.effectDelegate.sendEffect(effect)
    }
}

context(viewModel: ViewModel, producer: EffectProducer<Effect>)
@Composable
fun <Effect: ViewEffect> OnEffectUpdate(
    collector: FlowCollector<Effect>
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            producer.effectDelegate.effects.collect(collector)
        }
    }
}