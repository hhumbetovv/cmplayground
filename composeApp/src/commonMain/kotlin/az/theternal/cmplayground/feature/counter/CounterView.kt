package az.theternal.cmplayground.feature.counter

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.feature.counter.CounterContract.Effect
import az.theternal.common.utils.Logger
import az.theternal.core.framework.delegates.effect_producer.OnEffectUpdate
import az.theternal.core.framework.delegates.intent_consumer.postIntent
import az.theternal.core.framework.delegates.state_holder.collectAsState

@Composable
fun ColumnScope.CounterView(
    viewModel: CounterViewModel = viewModel { CounterViewModel() }
) = with(viewModel) {

    OnEffectUpdate { effect ->
        when(effect) {
            is Effect.ShowSnackbar -> {
                Logger.d(effect.text)
            }
        }
    }

    val state by collectAsState()

    CounterContent(
        state = state,
        postIntent = { viewModel.postIntent(it) }
    )
}
