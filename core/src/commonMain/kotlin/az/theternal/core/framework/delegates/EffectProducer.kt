package az.theternal.core.framework.delegates

import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

// EffectProducer - EffectEmitter

interface ViewEffect

interface EffectProducer<Effect : ViewEffect>{
    val effectEmitter: EffectEmitter<Effect>

    fun sendEffect(effect: Effect) {
        effectEmitter.sendEffect(effect)
    }
}

class EffectEmitter<Effect : ViewEffect> internal constructor(
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val _viewEffect = Channel<Effect>()
    val viewEffect: Flow<Effect> = _viewEffect.receiveAsFlow()

    fun sendEffect(effect: Effect) {
        scope.launch {
            _viewEffect.send(effect)
        }
    }

    override fun close() {
        _viewEffect.close()
    }
}

context(viewModel: ViewModel)
fun <Effect : ViewEffect> EffectProducer<Effect>.EffectEmitter(): EffectEmitter<Effect> {
    val emitter =  EffectEmitter<Effect>(
        scope = viewModel.viewModelScope
    )

    viewModel.addCloseable(emitter)

    return emitter
}

context(viewModel: ViewModel)
fun <Effect : ViewEffect> EffectProducer<Effect>.sendEffect(
    effect: Effect
) {
    viewModel.viewModelScope.launch {
        effectEmitter.sendEffect(effect)
    }
}

@Composable
context(viewModel: ViewModel)
fun <Effect: ViewEffect> EffectProducer<Effect>.OnEffectUpdate(
    collector: FlowCollector<Effect>
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effectEmitter.viewEffect.collect(collector)
        }
    }
}