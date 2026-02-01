package az.theternal.core.framework.delegates.effect_producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

context(viewModel: ViewModel, _: EffectProducer<Effect>)
fun <Effect : ViewEffect> EffectHandler(): EffectHandler<Effect> {
    return EffectHandler<Effect>(
        scope = viewModel.viewModelScope
    ).also { handler ->
        viewModel.addCloseable(handler)
    }
}

context(viewModel: ViewModel, producer: EffectProducer<Effect>)
fun <Effect : ViewEffect> sendEffect(effect: Effect) {
    viewModel.viewModelScope.launch {
        producer.effectHandler.sendEffect(effect)
    }
}

@Composable
fun <Effect : ViewEffect> EffectProducer<Effect>.OnEffectUpdate(
    collector: FlowCollector<Effect>
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            effectHandler.effects.collect(collector)
        }
    }
}
