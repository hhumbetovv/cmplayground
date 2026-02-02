package az.theternal.cmplayground.feature.counter

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.feature.counter.CounterContract.Effect
import az.theternal.common.utils.Logger
import az.theternal.core.framework.effect_producer.OnEffectUpdate
import az.theternal.core.framework.intent_consumer.postIntent
import az.theternal.core.framework.state_holder.collectAsState

@Composable
fun ColumnScope.CounterView(
    viewModel: CounterViewModel = viewModel { CounterViewModel() }
) {

    viewModel.OnEffectUpdate { effect ->
        when(effect) {
            is Effect.ShowSnackbar -> {
                Logger.d(effect.text)
            }
        }
    }

    val state by viewModel.collectAsState()

    CounterContent(
        state = state,
        postIntent = { viewModel.postIntent(it) }
    )
}
