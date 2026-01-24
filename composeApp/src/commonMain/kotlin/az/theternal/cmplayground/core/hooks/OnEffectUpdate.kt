package az.theternal.cmplayground.core.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import az.theternal.cmplayground.core.base.BaseViewModel
import az.theternal.cmplayground.core.base.ViewEffect
import kotlinx.coroutines.flow.FlowCollector

@Composable
fun <Effect : ViewEffect> BaseViewModel<*, *, Effect>.onEffectUpdate(
    collector: FlowCollector<Effect>,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            this@onEffectUpdate.viewEffect.collect(collector)
        }
    }
}