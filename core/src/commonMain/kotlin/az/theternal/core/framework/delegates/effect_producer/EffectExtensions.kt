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
fun <Effect : ViewEffect> EffectDelegate(): EffectDelegate<Effect> {
    return EffectDelegate<Effect>(
        scope = viewModel.viewModelScope
    ).also { delegate ->
        viewModel.addCloseable(delegate)
    }
}

context(viewModel: ViewModel, producer: EffectProducer<Effect>)
fun <Effect : ViewEffect> sendEffect(effect: Effect) {
    viewModel.viewModelScope.launch {
        producer.effectDelegate.sendEffect(effect)
    }
}

context(viewModel: ViewModel, producer: EffectProducer<Effect>)
@Composable
fun <Effect : ViewEffect> OnEffectUpdate(
    collector: FlowCollector<Effect>
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            producer.effectDelegate.effects.collect(collector)
        }
    }
}
